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
import java.nio.file.FileVisitOption
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.time.OffsetDateTime
import java.util.EnumSet
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import cc.lanternmc.materiallauncher.api.JavaInstallation
import cc.lanternmc.materiallauncher.core.util.Os
import cc.lanternmc.materiallauncher.core.util.currentOs

/**
 * 在系统中查找 Java 安装，覆盖 Windows / macOS / Linux。
 */
object JavaFinder {

    fun javaExecutableName(): String = if (currentOs == Os.WINDOWS) "java.exe" else "java"

    fun environmentJavaCandidates(): List<String> {
        val result = mutableListOf<String>()
        val javaHome = System.getenv("JAVA_HOME")?.trim()?.trim('"')
        if (!javaHome.isNullOrEmpty()) {
            result.add(File(javaHome, "bin/${javaExecutableName()}").absolutePath)
        }
        runCatching {
            val pathEnv = System.getenv("PATH") ?: return@runCatching
            for (dir in pathEnv.split(File.pathSeparator)) {
                val candidate = File(dir, javaExecutableName())
                if (candidate.isFile) {
                    result.add(candidate.absolutePath)
                    break
                }
            }
        }
        return result
    }

    fun platformJavaCandidates(): List<String> = when (currentOs) {
        Os.WINDOWS -> windowsCandidates()
        Os.MAC -> macCandidates()
        Os.LINUX -> linuxCandidates()
        else -> emptyList()
    }

    fun platformJavaIndexRoots(): List<String> = when (currentOs) {
        Os.WINDOWS -> File.listRoots().map { it.absolutePath }
        Os.MAC -> listOf("/", "/Volumes")
        Os.LINUX -> listOf("/")
        else -> emptyList()
    }

    private fun windowsCandidates(): List<String> {
        val result = windowsRegistryJavaCandidates().toMutableList()
        val roots = listOfNotNull(
            System.getenv("ProgramFiles"),
            System.getenv("ProgramFiles(x86)"),
            System.getenv("LOCALAPPDATA")?.let { File(it, "Programs").absolutePath },
            System.getenv("LOCALAPPDATA")?.let { File(it, "JetBrains/Toolbox/apps").absolutePath },
            System.getenv("USERPROFILE")?.let { File(it, ".jdks").absolutePath },
            System.getenv("APPDATA")?.let { File(it, ".minecraft/runtime").absolutePath },
        )
        result.addAll(findJavaCandidatesLimited(roots, 4))
        return result.distinct()
    }

    private fun linuxCandidates(): List<String> {
        val home = System.getProperty("user.home")
        return findJavaCandidatesLimited(
            listOfNotNull(
                "/usr/lib/jvm", "/usr/java", "/opt", "/usr/local",
                home?.let { File(it, ".jdks").absolutePath },
                home?.let { File(it, ".sdkman/candidates/java").absolutePath },
            ),
            4,
        )
    }

    private fun macCandidates(): List<String> {
        val home = System.getProperty("user.home")
        return findJavaCandidatesLimited(
            listOfNotNull(
                "/Library/Java/JavaVirtualMachines",
                "/System/Library/Java/JavaVirtualMachines",
                "/opt/homebrew/opt", "/usr/local/opt",
                home?.let { File(it, "Library/Java/JavaVirtualMachines").absolutePath },
                home?.let { File(it, ".jdks").absolutePath },
                home?.let { File(it, ".sdkman/candidates/java").absolutePath },
            ),
            6,
        )
    }

    /**
     * Windows 注册表 JAVA_HOME 扫描（reg query 递归）。
     */
    fun windowsRegistryJavaCandidates(): List<String> {
        val result = mutableListOf<String>()
        for (root in listOf(
            "HKLM\\SOFTWARE\\JavaSoft",
            "HKLM\\SOFTWARE\\WOW6432Node\\JavaSoft",
            "HKCU\\SOFTWARE\\JavaSoft",
            "HKCU\\SOFTWARE\\WOW6432Node\\JavaSoft",
        )) {
            val output = runCommand("reg", "query", root, "/s") ?: continue
            for (line in output.lineSequence()) {
                val trimmed = line.trim()
                if (trimmed.startsWith("JavaHome")) {
                    val home = trimmed.substringAfter("REG_SZ", "").trim()
                    if (home.isNotEmpty()) {
                        result.add(File(home, "bin/${javaExecutableName()}").absolutePath)
                    }
                }
            }
        }
        return result.distinct()
    }

    /**
     * 深度受限的目录遍历，找到 `bin/java(.exe)` 候选。
     * 使用 walkFileTree 在遍历前剪枝（跳过系统/缓存目录），避免无谓扫描。
     */
    fun findJavaCandidatesLimited(roots: List<String>, maxDepth: Int): List<String> {
        val result = mutableListOf<String>()
        for (root in roots) {
            val rootPath = Paths.get(root)
            if (!Files.isDirectory(rootPath)) continue
            runCatching {
                Files.walkFileTree(
                    rootPath,
                    EnumSet.noneOf(FileVisitOption::class.java),
                    maxDepth,
                    object : SimpleFileVisitor<Path>() {
                        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                            val name = dir.fileName?.toString().orEmpty()
                            if (dir != rootPath && shouldSkipDirectory(name)) {
                                return FileVisitResult.SKIP_SUBTREE
                            }
                            return FileVisitResult.CONTINUE
                        }

                        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                            val name = file.fileName?.toString().orEmpty()
                            val inBin = file.parent?.fileName?.toString()?.equals("bin", ignoreCase = true) == true
                            if (inBin && isJavaExecutable(name)) {
                                result.add(file.toAbsolutePath().toString())
                            }
                            return FileVisitResult.CONTINUE
                        }
                    },
                )
            }
        }
        return result.distinct()
    }

    fun isJavaExecutable(name: String): Boolean =
        if (currentOs == Os.WINDOWS) name.equals("java.exe", ignoreCase = true) else name == "java"

    fun shouldSkipDirectory(name: String): Boolean {
        val lower = name.lowercase()
        if (lower.startsWith(".") && lower != ".jdks" && lower != ".sdkman") return true
        return lower in skipNames
    }

    @Suppress("CanConvertToMultiDollarString")
    private val skipNames = setOf(
        // 系统目录
        "\$recycle.bin", "system volume information", "windows", "windows.old",
        "winnt", "proc", "sys", "dev", "run", "recovery", "perflogs", "boot",
        "lost+found", "msocache",
        // 项目/构建产物
        "node_modules", ".git", "__pycache__", "venv",
        // 缓存与临时目录（Java 安装绝不会出现在这里）
        "temp", "tmp", "cache", "cacheddata", "code cache", "gpu cache",
        "inetcache", "crashdumps", "service worker", "cachestorage",
        "localcache", "tempstate", "logs", "log",
        // 驱动/厂商临时目录
        "intel", "amd", "nvidia", "package cache",
        // 云同步目录（云占位文件会阻塞目录枚举）
        "onedrive",
        // Windows 重复/联接目录
        "application data", "local settings", "documents and settings",
    )

    fun pathKey(path: String): String =
        if (currentOs == Os.WINDOWS) path.lowercase() else path

    /**
     * 运行 `java -version` 探测一个 Java 安装的详细信息。
     */
    fun probeJavaVersion(javaPath: String): JavaInstallation? = runCatching {
        val output = runCommand(javaPath, "-version") ?: return null
        val lines = output.trim().split('\n')
        val firstLine = lines.firstOrNull()?.trim() ?: return null
        var version = extractVersion(firstLine) ?: return null
        val home = File(javaPath).parentFile?.parentFile?.absolutePath ?: return null
        val release = readJavaRelease(home)
        if (!release["JAVA_VERSION"].isNullOrEmpty()) {
            version = release["JAVA_VERSION"]!!
        }
        JavaInstallation(
            path = File(javaPath).absolutePath,
            home = home,
            javaType = detectJavaType(lines, javaPath),
            version = version,
            vendor = firstNonEmpty(release["IMPLEMENTOR"], detectJavaVendor(lines)),
            architecture = release["OS_ARCH"].orEmpty(),
            lastVerified = OffsetDateTime.now().toString(),
        )
    }.getOrNull()

    fun sortJavaInstallations(items: MutableList<JavaInstallation>) {
        items.sortWith { a, b ->
            val byVersion = compareVersions(b.version, a.version)
            if (byVersion != 0) byVersion else a.path.compareTo(b.path)
        }
    }

    /**
     * 从版本字符串解析 Java 主版本号：1.8.0_392 -> 8，17.0.9 -> 17。
     */
    fun javaFeatureVersion(version: String): Int {
        val cleaned = version.trim().substringBefore('+').substringBefore('-')
        if (cleaned.startsWith("1.")) {
            return cleaned.substringAfter('.').substringBefore('.').toIntOrNull() ?: 8
        }
        return cleaned.substringBefore('.').toIntOrNull() ?: 8
    }

    private fun compareVersions(a: String, b: String): Int {
        fun parts(v: String): List<Int> = v.split('.', '_', '-', '+')
            .mapNotNull { it.toIntOrNull() }
        val pa = parts(a)
        val pb = parts(b)
        val count = maxOf(pa.size, pb.size)
        for (i in 0 until count) {
            val va = pa.getOrElse(i) { 0 }
            val vb = pb.getOrElse(i) { 0 }
            if (va != vb) return va - vb
        }
        return 0
    }

    private fun readJavaRelease(home: String): Map<String, String> {
        val file = File(home, "release")
        if (!file.isFile) return emptyMap()
        val values = mutableMapOf<String, String>()
        file.forEachLine { line ->
            val idx = line.indexOf('=')
            if (idx < 0) return@forEachLine
            values[line.substring(0, idx).trim()] = line.substring(idx + 1).trim().trim('"')
        }
        return values
    }

    private fun extractVersion(line: String): String? {
        val quoteStart = line.indexOf("version \"")
        if (quoteStart >= 0) {
            val rest = line.substring(quoteStart + "version \"".length)
            val end = rest.indexOf('"')
            if (end >= 0) {
                var version = rest.substring(0, end)
                val plus = version.indexOf('+')
                if (plus >= 0) version = version.substring(0, plus)
                return version
            }
        }
        val plainStart = line.indexOf("version ")
        if (plainStart >= 0) {
            val rest = line.substring(plainStart + "version ".length)
            val end = rest.indexOfFirst { !it.isDigit() && it != '.' && it != '_' && it != '-' }
            return if (end < 0) rest else rest.substring(0, end)
        }
        return null
    }

    private fun detectJavaType(lines: List<String>, javaPath: String): String {
        val hasJavac = runCatching {
            val javacName = "javac" + (File(javaPath).extension.takeIf { it.isNotBlank() }?.let { ".$it" } ?: "")
            File(File(javaPath).parentFile, javacName).isFile
        }.getOrDefault(false)
        if (hasJavac) return "JDK"
        val text = javaPath.lowercase() + "\n" + lines.joinToString("\n").lowercase()
        return when {
            "jdk" in text || "development kit" in text || "j2sdk" in text -> "JDK"
            else -> "JRE"
        }
    }

    private fun detectJavaVendor(lines: List<String>): String {
        val text = lines.joinToString("\n").lowercase()
        val vendors = listOf(
            "temurin" to "Eclipse Adoptium",
            "adoptium" to "Eclipse Adoptium",
            "corretto" to "Amazon Corretto",
            "zulu" to "Azul Systems",
            "liberica" to "BellSoft",
            "graalvm" to "GraalVM",
            "microsoft" to "Microsoft",
            "openjdk" to "OpenJDK",
            "oracle" to "Oracle",
            "ibm" to "IBM",
        )
        for ((keyword, name) in vendors) {
            if (keyword in text) return name
        }
        return "Unknown"
    }

    private fun firstNonEmpty(vararg values: String?): String {
        for (value in values) {
            if (!value.isNullOrEmpty()) return value
        }
        return ""
    }

    private fun runCommand(vararg command: String): String? {
        val process = runCatching {
            ProcessBuilder(*command).redirectErrorStream(true).start()
        }.getOrNull() ?: return null
        val outputFuture = CompletableFuture.supplyAsync {
            runCatching {
                process.inputStream.bufferedReader().use { it.readText() }
            }.getOrNull()
        }
        val finished = runCatching { process.waitFor(5, TimeUnit.SECONDS) }.getOrDefault(false)
        if (!finished) {
            process.destroyForcibly()
            return null
        }
        return runCatching { outputFuture.get(5, TimeUnit.SECONDS) }.getOrNull()
    }
}
