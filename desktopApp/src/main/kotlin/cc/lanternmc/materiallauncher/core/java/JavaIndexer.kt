/*
 * Copyright (C) 2026 Mark <github@xxdmkbmark> & Pidan <github@bretren>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cc.lanternmc.materiallauncher.core.java

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import cc.lanternmc.materiallauncher.api.JavaInstallation
import cc.lanternmc.materiallauncher.api.LauncherEvent
import cc.lanternmc.materiallauncher.core.util.Logger
import cc.lanternmc.materiallauncher.core.util.Toml

/**
 * 全盘扫描 Java 安装的后台索引器。
 * 两阶段：先快速探测已知路径，再全盘 BFS 扫描。
 * 结果缓存到 java-index.toml，7 天内复用。
 */
class JavaIndexer(
    private val cachePath: String,
    private val scope: CoroutineScope,
    private val emit: (LauncherEvent) -> Unit,
) {
    private val lock = Any()
    private var results: MutableList<JavaInstallation> = mutableListOf()
    private var running = false
    private var scanWorkers = 0
    private var indexedAt: Instant? = null

    init {
        loadCache()
    }

    fun cachedResults(): List<JavaInstallation> {
        synchronized(lock) {
            return results.toList()
        }
    }

    fun cacheIsFresh(): Boolean {
        synchronized(lock) {
            val indexed = indexedAt ?: return false
            val age = ChronoUnit.HOURS.between(indexed, Instant.now())
            return age in 0..167
        }
    }

    /**
     * 启动后台索引。已在进行 / 缓存新鲜时返回 false（未启动）。
     */
    fun start(force: Boolean): Boolean {
        synchronized(lock) {
            if (running) return false
            if (!force && cacheIsFresh()) return false
            running = true
        }
        scope.launch(Dispatchers.Default) {
            run()
        }
        return true
    }

    private suspend fun run() {
        try {
            val startedAt = System.currentTimeMillis()
            val cachedCount = cachedResults().size
            val roots = JavaFinder.platformJavaIndexRoots()
            val workers = Runtime.getRuntime().availableProcessors().coerceIn(2, 8)
            synchronized(lock) { scanWorkers = workers }

            emit(LauncherEvent.JavaIndexStarted(roots = roots, workers = workers, cachedCount = cachedCount))

            // 阶段一：快速探测
            val quick = (JavaFinder.platformJavaCandidates() + JavaFinder.environmentJavaCandidates()).distinct()
            val quickResults = probeAll(quick)
            for (installation in quickResults) {
                emit(LauncherEvent.JavaIndexFound(installation))
            }
            mergeResults(quickResults)
            saveSnapshot(completed = false)

            // 阶段二：全盘扫描
            val (candidates, scanned) = scanJavaIndex(roots, workers) { directoriesScanned, candidatesFound ->
                emit(
                    LauncherEvent.JavaIndexProgress(
                        directoriesScanned = directoriesScanned,
                        candidatesFound = candidatesFound,
                        elapsedMs = System.currentTimeMillis() - startedAt,
                    ),
                )
            }

            val indexed = probeAll(candidates)
            for (installation in indexed) {
                emit(LauncherEvent.JavaIndexFound(installation))
            }
            mergeResults(indexed)

            val finalResults = cachedResults()
            saveSnapshot(completed = true)
            emit(
                LauncherEvent.JavaIndexCompleted(
                    installations = finalResults,
                    durationMs = System.currentTimeMillis() - startedAt,
                    cachePath = cachePath,
                ),
            )
        } catch (e: Exception) {
            Logger.error("Java 索引失败: ${e.message}")
            emit(LauncherEvent.JavaIndexError(e.message ?: "unknown error"))
        } finally {
            synchronized(lock) {
                running = false
            }
        }
    }

    private fun probeAll(paths: List<String>): List<JavaInstallation> {
        val unique = paths.filter { path ->
            val file = File(path)
            file.isFile && !Files.isSymbolicLink(file.toPath())
        }.distinct()
        if (unique.isEmpty()) return emptyList()
        val workers = Runtime.getRuntime().availableProcessors().coerceIn(1, 4)
        val executor = Executors.newFixedThreadPool(workers) { runnable ->
            Thread(runnable, "java-probe").apply { isDaemon = true }
        }
        val results = ConcurrentLinkedQueue<JavaInstallation>()
        try {
            for (path in unique) {
                executor.execute {
                    JavaFinder.probeJavaVersion(path)?.let { results.add(it) }
                }
            }
            executor.shutdown()
            executor.awaitTermination(30, TimeUnit.SECONDS)
        } finally {
            executor.shutdownNow()
        }
        return results.toList()
    }

    private fun mergeResults(found: List<JavaInstallation>) {
        synchronized(lock) {
            val byPath = HashMap<String, JavaInstallation>()
            for (item in results + found) {
                if (File(item.path).isFile) {
                    byPath[JavaFinder.pathKey(item.path)] = item
                }
            }
            results = byPath.values.toMutableList()
            JavaFinder.sortJavaInstallations(results)
        }
    }

    private fun scanJavaIndex(
        roots: List<String>,
        workers: Int,
        onProgress: (scanned: Long, candidates: Int) -> Unit,
    ): Pair<List<String>, Long> {
        val pending = AtomicInteger()
        val tasks = LinkedBlockingQueue<String>()
        val visited = ConcurrentHashMap.newKeySet<String>()
        val candidates = ConcurrentLinkedQueue<String>()
        val scanned = AtomicLong()
        val lastProgress = AtomicLong(System.currentTimeMillis())

        for (root in roots) {
            val abs = runCatching { Paths.get(root).toAbsolutePath().normalize().toString() }.getOrNull() ?: continue
            if (Files.isDirectory(Paths.get(abs)) && visited.add(JavaFinder.pathKey(abs))) {
                pending.incrementAndGet()
                tasks.add(abs)
            }
        }
        if (pending.get() == 0) return Pair(emptyList(), 0)

        val executor = Executors.newFixedThreadPool(workers) { runnable ->
            Thread(runnable, "java-index-worker").apply { isDaemon = true }
        }
        repeat(workers) {
            executor.execute {
                while (true) {
                    val directory = tasks.poll(50, TimeUnit.MILLISECONDS)
                    if (directory == null) {
                        if (pending.get() == 0) break
                        continue
                    }
                    val (dirs, cands) = scanJavaDirectory(directory)
                    scanned.incrementAndGet()
                    for (child in dirs) {
                        if (visited.add(JavaFinder.pathKey(child))) {
                            pending.incrementAndGet()
                            tasks.add(child)
                        }
                    }
                    candidates.addAll(cands)
                    val now = System.currentTimeMillis()
                    if (now - lastProgress.get() >= 500) {
                        lastProgress.set(now)
                        onProgress(scanned.get(), candidates.size)
                    }
                    if (pending.decrementAndGet() == 0) break
                }
            }
        }
        executor.shutdown()
        while (!executor.isTerminated) {
            runCatching { executor.awaitTermination(100, TimeUnit.MILLISECONDS) }
        }
        return Pair(
            candidates.toList().distinct().filter { File(it).isFile },
            scanned.get(),
        )
    }

    private fun scanJavaDirectory(directory: String): Pair<List<String>, List<String>> {
        val dirs = mutableListOf<String>()
        val cands = mutableListOf<String>()
        val stream = try {
            Files.newDirectoryStream(Paths.get(directory))
        } catch (e: Exception) {
            return Pair(emptyList(), emptyList())
        }
        stream.use { entries ->
            for (entry in entries) {
                val name = entry.fileName.toString()
                if (Files.isDirectory(entry)) {
                    if (!JavaFinder.shouldSkipDirectory(name)) {
                        dirs.add(entry.toAbsolutePath().toString())
                    }
                    continue
                }
                if (!JavaFinder.isJavaExecutable(name)) continue
                if (Files.isSymbolicLink(entry)) continue
                val inBin = entry.parent?.fileName?.toString()?.equals("bin", ignoreCase = true) == true
                if (inBin) {
                    cands.add(entry.toAbsolutePath().toString())
                }
            }
        }
        return Pair(dirs, cands)
    }

    private fun loadCache() {
        val file = File(cachePath)
        if (!file.isFile) return
        val doc = runCatching { Toml.parse(file.readText()) }.getOrNull() ?: return
        val root = doc.section("")
        val java = doc.section("java")
        synchronized(lock) {
            scanWorkers = root.values["scan_workers"]?.toIntOrNull() ?: 0
            indexedAt = runCatching { Instant.parse(root.values["indexed_at"] ?: "") }.getOrNull()
            results = java.items.mapNotNull { item ->
                val path = item["path"] ?: return@mapNotNull null
                JavaInstallation(
                    path = path,
                    home = item["home"].orEmpty(),
                    javaType = item["type"].orEmpty(),
                    version = item["java_version"].orEmpty(),
                    vendor = item["vendor"].orEmpty(),
                    architecture = item["architecture"].orEmpty(),
                    lastVerified = item["last_verified"].orEmpty(),
                )
            }.filter { File(it.path).isFile }.toMutableList()
            JavaFinder.sortJavaInstallations(results)
        }
    }

    private fun saveSnapshot(completed: Boolean) {
        runCatching {
            val now = Instant.now()
            synchronized(lock) {
                if (completed) indexedAt = now
            }
            val indexedAtText = indexedAt?.toString().orEmpty()
            val items = cachedResults()
            val content = buildString {
                appendLine("# Material Launcher Java index. Generated automatically.")
                appendLine("version = 1")
                appendLine("updated_at = ${Toml.quote(now.toString())}")
                appendLine("indexed_at = ${Toml.quote(indexedAtText)}")
                appendLine("scan_workers = ${scanWorkers.coerceAtLeast(1)}")
                for (item in items) {
                    appendLine()
                    appendLine("[[java]]")
                    appendLine("path = ${Toml.quote(item.path)}")
                    appendLine("home = ${Toml.quote(item.home)}")
                    appendLine("type = ${Toml.quote(item.javaType)}")
                    appendLine("java_version = ${Toml.quote(item.version)}")
                    appendLine("vendor = ${Toml.quote(item.vendor)}")
                    appendLine("architecture = ${Toml.quote(item.architecture)}")
                    appendLine("last_verified = ${Toml.quote(item.lastVerified)}")
                }
            }
            File(cachePath).parentFile?.mkdirs()
            val tmp = File("$cachePath.tmp")
            tmp.writeText(content)
            val target = File(cachePath)
            if (target.exists()) target.delete()
            tmp.renameTo(target)
        }.onFailure {
            Logger.warn("保存 Java 索引缓存失败: ${it.message}")
        }
    }
}
