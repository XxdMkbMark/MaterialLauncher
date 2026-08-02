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
package cc.lanternmc.materiallauncher.core

import java.io.File
import java.util.concurrent.atomic.AtomicLong
import javax.swing.JFileChooser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import kotlinx.serialization.json.Json
import cc.lanternmc.materiallauncher.api.DownloadConfig
import cc.lanternmc.materiallauncher.api.DownloadProgress
import cc.lanternmc.materiallauncher.api.JavaInstallation
import cc.lanternmc.materiallauncher.api.JavaReleaseInfo
import cc.lanternmc.materiallauncher.api.LauncherApi
import cc.lanternmc.materiallauncher.api.LauncherEvent
import cc.lanternmc.materiallauncher.api.LauncherEventBus
import cc.lanternmc.materiallauncher.api.LaunchRequest
import cc.lanternmc.materiallauncher.api.MinecraftVersionEntry
import cc.lanternmc.materiallauncher.core.config.AppDataPathsResolver
import cc.lanternmc.materiallauncher.core.config.DownloadConfigStore
import cc.lanternmc.materiallauncher.core.download.AssetDownloader
import cc.lanternmc.materiallauncher.core.download.JavaVersionService
import cc.lanternmc.materiallauncher.core.download.LibraryDownloader
import cc.lanternmc.materiallauncher.core.download.MinecraftVersionService
import cc.lanternmc.materiallauncher.core.java.JavaFinder
import cc.lanternmc.materiallauncher.core.java.JavaIndexer
import cc.lanternmc.materiallauncher.core.launch.LauncherException
import cc.lanternmc.materiallauncher.core.launch.LaunchService
import cc.lanternmc.materiallauncher.core.model.VersionJson
import cc.lanternmc.materiallauncher.core.util.ArchiveExtractor
import cc.lanternmc.materiallauncher.core.util.HttpUtil
import cc.lanternmc.materiallauncher.core.util.Logger
import cc.lanternmc.materiallauncher.core.util.Sha1
import cc.lanternmc.materiallauncher.core.util.compareMinecraftVersion

/**
 * 后端实现：实现 [LauncherApi]（前端调用）与 [LauncherEventBus]（前端订阅）。
 * 前端只依赖 [cc.lanternmc.materiallauncher.api]，绝不直接触碰本类以外的东西。
 */
class LauncherBackend : LauncherApi, LauncherEventBus {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val json = Json { ignoreUnknownKeys = true }

    private val paths = AppDataPathsResolver.resolve()
    private val configStore = DownloadConfigStore(paths)
    private val assetDownloader = AssetDownloader()
    private val libraryDownloader = LibraryDownloader()
    private val launchService = LaunchService(scope, assetDownloader, libraryDownloader)
    private val javaIndexer = JavaIndexer(paths.javaIndex, scope) { event -> _events.tryEmit(event) }
    private val downloadId = AtomicLong(0)

    private val _events = MutableSharedFlow<LauncherEvent>(extraBufferCapacity = 1024)
    override val events: SharedFlow<LauncherEvent> = _events.asSharedFlow()

    // ---------- 日志 ----------

    override fun logInfo(message: String) = Logger.info("[ui] $message")
    override fun logWarn(message: String) = Logger.warn("[ui] $message")
    override fun logError(message: String) = Logger.error("[ui] $message")

    // ---------- 配置 ----------

    override suspend fun getDownloadConfig(): DownloadConfig = withContext(Dispatchers.IO) {
        configStore.load()
    }

    override suspend fun saveDownloadConfig(config: DownloadConfig) {
        withContext(Dispatchers.IO) { configStore.save(config) }
    }

    override suspend fun getDefaultMinecraftDir(): String = withContext(Dispatchers.IO) {
        configStore.defaultMinecraftDir()
    }

    override suspend fun getLauncherMinecraftDir(): String = withContext(Dispatchers.IO) {
        configStore.launcherMinecraftDir()
    }

    override suspend fun getLauncherJavaDir(): String = withContext(Dispatchers.IO) {
        configStore.launcherJavaDir()
    }

    // ---------- 已安装内容 ----------

    override suspend fun getInstalledMinecraftVersions(mcPath: String): List<String> = withContext(Dispatchers.IO) {
        val versionsDir = File(mcPath, "versions")
        if (!versionsDir.isDirectory) return@withContext emptyList()
        versionsDir.listFiles()
            ?.filter { it.isDirectory && File(it, "${it.name}.json").isFile }
            ?.map { it.name }
            ?.sortedWith { a, b -> compareMinecraftVersion(b, a) }
            ?: emptyList()
    }

    override suspend fun findJavaPaths(): List<JavaInstallation> = withContext(Dispatchers.IO) {
        javaIndexer.ensureQuickProbe()
        javaIndexer.start(force = false)
        javaIndexer.cachedResults()
    }

    override fun refreshJavaIndex(): Boolean = javaIndexer.start(force = true)

    // ---------- 远程版本列表 ----------

    override suspend fun getMinecraftVersions(): List<MinecraftVersionEntry> =
        MinecraftVersionService.fetchVersions()

    override suspend fun getJavaVersions(): List<JavaReleaseInfo> =
        JavaVersionService.fetchVersions()

    // ---------- 下载 ----------

    override fun startMinecraftDownload(versionId: String) {
        scope.launch { downloadMinecraft(versionId) }
    }

    override fun startJavaDownload(javaVersionId: String) {
        scope.launch { downloadJava(javaVersionId) }
    }

    private fun nextDownloadId(): String = "dl-${downloadId.incrementAndGet()}"

    private suspend fun downloadMinecraft(versionId: String) {
        val id = nextDownloadId()
        fun emitProgress(status: String, downloaded: Long = 0, total: Long = 0, error: String? = null) {
            _events.tryEmit(
                LauncherEvent.DownloadProgressEvent(
                    DownloadProgress(
                        id = id, type = "minecraft", item = versionId,
                        status = status, downloaded = downloaded, total = total, error = error,
                    ),
                ),
            )
        }

        emitProgress("fetching")
        val mcPath = configStore.load().minecraft.path
        try {
            val manifest = MinecraftVersionService.fetchVersions()
            val entry = manifest.firstOrNull { it.id == versionId }
                ?: throw LauncherException("未找到版本 $versionId")

            val versionDir = File(mcPath, "versions/$versionId")
            versionDir.mkdirs()

            emitProgress("downloading", 0, 0)
            val versionJsonBytes = HttpUtil.getBytes(entry.url)
            val versionInfo = json.decodeFromString<VersionJson>(String(versionJsonBytes, Charsets.UTF_8))
            File(versionDir, "$versionId.json").writeBytes(versionJsonBytes)

            val client = versionInfo.downloads?.client
            if (client == null || client.url.isBlank()) {
                throw LauncherException("版本 $versionId 缺少 client 下载信息")
            }
            val clientJar = File(versionDir, "$versionId.jar")
            emitProgress("downloading", 0, client.size)
            HttpUtil.downloadFile(client.url, clientJar.absolutePath) { downloaded, total ->
                emitProgress("downloading", downloaded, if (total > 0) total else client.size)
            }
            if (!Sha1.isFileValid(clientJar.absolutePath, client.sha1, client.size)) {
                clientJar.delete()
                throw LauncherException("版本 $versionId 的 client 文件未通过 SHA-1/大小校验")
            }
            emitProgress("done", client.size, client.size)
        } catch (e: Exception) {
            Logger.error("Minecraft 下载失败: ${e.message}")
            emitProgress("error", error = e.message ?: "unknown error")
        }
    }

    private suspend fun downloadJava(javaVersionId: String) {
        val id = nextDownloadId()
        fun emitProgress(status: String, downloaded: Long = 0, total: Long = 0, error: String? = null) {
            _events.tryEmit(
                LauncherEvent.DownloadProgressEvent(
                    DownloadProgress(
                        id = id, type = "java", item = javaVersionId,
                        status = status, downloaded = downloaded, total = total, error = error,
                    ),
                ),
            )
        }

        emitProgress("fetching")
        val javaDir = configStore.load().java.path
        try {
            val selected = JavaVersionService.fetchVersions().firstOrNull { it.id == javaVersionId }
                ?: throw LauncherException("未找到 Java 版本 $javaVersionId")

            val destDir = File(javaDir, "jdk-${selected.version}")
            val archiveName = selected.downloadUrl.substringAfterLast('/')
            val archivePath = File(javaDir, archiveName)
            File(javaDir).mkdirs()

            emitProgress("downloading", 0, selected.downloadSize)
            HttpUtil.downloadFile(selected.downloadUrl, archivePath.absolutePath) { downloaded, total ->
                emitProgress("downloading", downloaded, if (total > 0) total else selected.downloadSize)
            }
            if (archivePath.length() != selected.downloadSize) {
                archivePath.delete()
                throw LauncherException("Java 归档大小校验失败: ${archivePath.length()} != ${selected.downloadSize}")
            }
            emitProgress("extracting", selected.downloadSize, selected.downloadSize)

            ArchiveExtractor.extractArchive(archivePath.absolutePath, destDir.absolutePath)
            archivePath.delete()

            val javaBin = locateJavaBinary(destDir.absolutePath)
            if (javaBin != null) {
                JavaFinder.probeJavaVersion(javaBin)
                    ?.let { Logger.info("Java 下载完成: ${it.path} (${it.version})") }
            }
            emitProgress("done", selected.downloadSize, selected.downloadSize)
        } catch (e: Exception) {
            Logger.error("Java 下载失败: ${e.message}")
            emitProgress("error", error = e.message ?: "unknown error")
        }
    }

    private fun locateJavaBinary(extractDir: String): String? {
        val direct = File(extractDir, "bin/${JavaFinder.javaExecutableName()}")
        if (direct.isFile) return direct.absolutePath
        File(extractDir).listFiles()?.forEach { child ->
            if (child.isDirectory) {
                val candidate = File(child, "bin/${JavaFinder.javaExecutableName()}")
                if (candidate.isFile) return candidate.absolutePath
            }
        }
        return null
    }

    // ---------- 游戏启动 ----------

    override suspend fun launchMinecraft(request: LaunchRequest): Int =
        launchService.fullLaunch(
            javaPath = request.javaPath,
            gameDir = request.gameDir,
            versionId = request.versionId,
            username = request.username,
            maxMem = request.maxMemory,
            isolateVersion = request.isolateVersion,
            emit = { event -> _events.tryEmit(event) },
        )

    /**
     * 根据版本所需 Java 主版本号，在已安装 Java 中解析最合适的启动 Java。
     */
    override suspend fun resolveLaunchJava(gameDir: String, versionId: String, preferred: String): String =
        withContext(Dispatchers.IO) {
            val required = requiredJavaMajor(gameDir, versionId)
            val candidates = javaIndexer.cachedResults()
            val chosen = resolveJavaForRequired(required, candidates, preferred)
            if (chosen != preferred) {
                val chosenVersion = candidates.firstOrNull { it.path == chosen }?.version ?: chosen
                val preferredVersion = candidates.firstOrNull { it.path == preferred }?.version ?: preferred
                Logger.warn("启动 $versionId 需要 Java $required，已自动切换: $preferredVersion -> $chosenVersion")
            }
            chosen
        }

    private fun requiredJavaMajor(gameDir: String, versionId: String): Int = runCatching {
        val file = File(gameDir, "versions/$versionId/$versionId.json")
        val v = json.decodeFromString<VersionJson>(file.readText())
        v.javaVersion?.majorVersion ?: 8
    }.getOrDefault(8)

    private fun javaMajorOfPath(path: String): Int =
        JavaFinder.probeJavaVersion(path)?.version?.let { JavaFinder.javaFeatureVersion(it) } ?: 8

    /**
     * 老版本（需 Java 8 / LWJGL2）优先选 8；新版本优先选 ≥ 需求的最低可用版本。
     */
    private fun resolveJavaForRequired(required: Int, javas: List<JavaInstallation>, preferred: String): String {
        val preferredMajor = javaMajorOfPath(preferred)
        if (required <= 8) {
            if (preferredMajor == 8) return preferred
            javas.firstOrNull { JavaFinder.javaFeatureVersion(it.version) == 8 }?.let { return it.path }
            val lowest = javas.minByOrNull { JavaFinder.javaFeatureVersion(it.version) }
            if (lowest != null && preferredMajor > JavaFinder.javaFeatureVersion(lowest.version)) return lowest.path
            return preferred
        }
        if (preferredMajor >= required) return preferred
        val best = javas.mapNotNull { j ->
            val major = JavaFinder.javaFeatureVersion(j.version)
            if (major >= required) major to j else null
        }.minByOrNull { it.first }?.second?.path
        return best ?: preferred
    }

    // ---------- 系统集成 ----------

    override suspend fun openDirectoryDialog(title: String): String? = withContext(Dispatchers.Main) {
        val chooser = JFileChooser().apply {
            dialogTitle = title
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            isAcceptAllFileFilterUsed = false
        }
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile?.absolutePath
        } else {
            null
        }
    }
}
