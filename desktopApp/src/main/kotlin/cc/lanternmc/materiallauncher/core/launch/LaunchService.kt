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
package cc.lanternmc.materiallauncher.core.launch

import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import cc.lanternmc.materiallauncher.api.LauncherEvent
import cc.lanternmc.materiallauncher.core.download.AssetDownloader
import cc.lanternmc.materiallauncher.core.download.LibraryDownloader
import cc.lanternmc.materiallauncher.core.model.VersionJson
import cc.lanternmc.materiallauncher.core.util.Logger
import cc.lanternmc.materiallauncher.core.util.Os
import cc.lanternmc.materiallauncher.core.util.compareMinecraftVersion
import cc.lanternmc.materiallauncher.core.util.currentOs

class LauncherException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * 游戏启动服务：净化 options.txt → 补齐 Assets → 补齐 Libraries → 拉起 Java 进程。
 */
class LaunchService(
    private val assetDownloader: AssetDownloader,
    private val libraryDownloader: LibraryDownloader,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * @return 游戏进程 PID
     */
    suspend fun fullLaunch(
        javaPath: String,
        gameDir: String,
        versionId: String,
        username: String,
        maxMem: String,
        isolateVersion: Boolean,
        emit: (LauncherEvent) -> Unit,
    ): Int = withContext(Dispatchers.IO) {
        val actualGameDir = if (isolateVersion) File(gameDir, "versions/$versionId").absolutePath else gameDir
        if (!File(actualGameDir).mkdirs() && !File(actualGameDir).isDirectory) {
            throw LauncherException("创建游戏目录失败: $actualGameDir")
        }

        val versionDir = File(gameDir, "versions/$versionId")
        val versionJsonFile = File(versionDir, "$versionId.json")
        if (!versionJsonFile.isFile) {
            throw LauncherException("找不到版本 JSON 配置文件: $versionJsonFile")
        }
        val versionInfo = json.decodeFromString<VersionJson>(versionJsonFile.readText())

        Logger.info("[1/4] 正在检查并修复 options.txt...")
        OptionsSanitizer.sanitize(if (isolateVersion) File(actualGameDir) else File(gameDir))

        Logger.info("[2/4] 正在多线程下载/补齐 Assets 资源...")
        assetDownloader.downloadAssets(gameDir, versionInfo.assetIndex)

        Logger.info("[3/4] 正在检查并补齐 Libraries 依赖库...")
        val nativesDir = File(versionDir, "natives").absolutePath
        val classpaths = libraryDownloader.downloadLibraries(gameDir, nativesDir, versionInfo.libraries)
            .toMutableList()
        classpaths.add(File(versionDir, "$versionId.jar").absolutePath)

        val separator = if (currentOs == Os.WINDOWS) ";" else ":"
        val classpathArg = classpaths.joinToString(separator)

        Logger.info("[4/4] 正在拉起游戏...")
        val pid = launchGame(
            javaPath = javaPath,
            gameDir = gameDir,
            actualGameDir = actualGameDir,
            versionInfo = versionInfo,
            classpathArg = classpathArg,
            maxMem = maxMem,
            username = username,
            versionId = versionId,
            emit = emit,
        )
        pid
    }

    private suspend fun launchGame(
        javaPath: String,
        gameDir: String,
        actualGameDir: String,
        versionInfo: VersionJson,
        classpathArg: String,
        maxMem: String,
        username: String,
        versionId: String,
        emit: (LauncherEvent) -> Unit,
    ): Int = withContext(Dispatchers.IO) {
        val nativesPath = File(gameDir, "versions/$versionId/natives").absolutePath
        val jvmArgs = mutableListOf(
            "-Xmx$maxMem",
            "-Djava.library.path=$nativesPath",
            "-Dfile.encoding=UTF-8",
            "-Dsun.jnu.encoding=UTF-8",
            "-Dstdout.encoding=UTF-8",
            "-Dstderr.encoding=UTF-8",
        )
        val javaMajor = detectJavaMajor(javaPath)
        if (usesLwjgl2(versionId)) {
            addLwjgl2CompatibilityArgs(javaPath, javaMajor, jvmArgs)
        } else {
            val optionalArgs = listOf(
                "--enable-native-access=ALL-UNNAMED",
                "--sun-misc-unsafe-memory-access=allow",
            )
            for (option in optionalArgs) {
                if (javaSupportsOption(javaPath, option)) {
                    jvmArgs.add(option)
                } else {
                    Logger.warn("跳过当前 Java 不支持的 JVM 参数: $option")
                }
            }
        }

        val mcArgs = listOf(
            "--username", username,
            "--version", versionId,
            "--gameDir", actualGameDir,
            "--assetsDir", File(gameDir, "assets").absolutePath,
            "--assetIndex", versionInfo.assetIndex.id,
            "--accessToken", "0",
            "--uuid", "00000000-0000-0000-0000-000000000000",
            "--userType", "legacy",
            "--versionType", "release",
        )

        val command = listOf(javaPath) + jvmArgs + listOf("-cp", classpathArg, versionInfo.mainClass) + mcArgs
        val builder = ProcessBuilder(command)
        builder.directory(File(gameDir))
        builder.environment()["JAVA_TOOL_OPTIONS"] = "-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"

        val process = try {
            builder.start()
        } catch (e: Exception) {
            throw LauncherException("进程拉起失败: ${e.message}", e)
        }

        val pid = process.pid().toInt()
        Logger.info("Minecraft 启动成功, pid=$pid")

        val ready = AtomicBoolean(false)
        fun markReady() {
            if (ready.compareAndSet(false, true)) {
                emit(LauncherEvent.GameReady(pid))
            }
        }

        Thread {
            process.inputStream.bufferedReader(Charsets.UTF_8).forEachLine { line ->
                Logger.info("[MC] $line")
                if (line.contains("Setting user:")
                    || line.contains("Launched game")
                    || line.contains("Backend library:")
                    || line.contains("Created:")
                ) {
                    markReady()
                }
            }
            Logger.info("[MC] stdout 已关闭")
        }.apply { isDaemon = true }.start()

        Thread {
            process.errorStream.bufferedReader(Charsets.UTF_8).forEachLine { line ->
                Logger.warn("[MC-ERR] $line")
            }
        }.apply { isDaemon = true }.start()

        // 兜底：若 stdout 里没有可识别的就绪关键字，进程存活一段时间即视为已启动。
        Thread {
            try {
                Thread.sleep(10_000)
                if (process.isAlive) markReady()
            } catch (_: InterruptedException) {
            }
        }.apply { isDaemon = true }.start()

        Thread {
            val code = process.waitFor()
            if (code != 0) {
                Logger.error("Minecraft 游戏进程非正常退出, exit=$code")
            } else {
                Logger.info("Minecraft 游戏进程已结束")
            }
            emit(LauncherEvent.GameExited(pid))
        }.apply { isDaemon = true }.start()

        pid
    }

    private fun javaSupportsOption(javaPath: String, option: String): Boolean = runCatching {
        val process = ProcessBuilder(javaPath, option, "-version").redirectErrorStream(true).start()
        val ok = process.waitFor(3, TimeUnit.SECONDS)
        if (!ok) {
            process.destroyForcibly()
            false
        } else {
            process.exitValue() == 0
        }
    }.getOrDefault(false)

    /**
     * 1.13 之前的版本使用 LWJGL 2，在现代 JVM 上需要 --add-opens 才能反射到 JDK 内部。
     */
    private fun usesLwjgl2(versionId: String): Boolean = compareMinecraftVersion(versionId, "1.13") < 0

    private fun addLwjgl2CompatibilityArgs(javaPath: String, javaMajor: Int, jvmArgs: MutableList<String>) {
        if (javaMajor <= 8) return
        Logger.info("LWJGL2 版本 + Java $javaMajor，正在追加兼容参数...")
        val addOpens = listOf(
            "java.base/java.lang",
            "java.base/java.lang.reflect",
            "java.base/java.nio",
            "java.base/sun.nio.ch",
            "java.base/java.util",
            "java.base/java.util.concurrent",
            "java.base/java.util.zip",
            "java.base/java.math",
            "java.base/java.io",
            "java.base/java.net",
            "java.base/jdk.internal.loader",
            "java.desktop/java.awt",
            "java.desktop/java.beans",
            "java.desktop/sun.awt",
            "java.desktop/sun.awt.image",
        )
        for (module in addOpens) {
            val arg = "--add-opens=$module=ALL-UNNAMED"
            if (javaSupportsOption(javaPath, arg)) {
                jvmArgs.add(arg)
            }
        }
        for (option in listOf(
            "--enable-native-access=ALL-UNNAMED",
            "--sun-misc-unsafe-memory-access=allow",
        )) {
            if (javaSupportsOption(javaPath, option)) {
                jvmArgs.add(option)
            }
        }
    }

    private fun detectJavaMajor(javaPath: String): Int = runCatching {
        val process = ProcessBuilder(javaPath, "-version").redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        process.waitFor(5, TimeUnit.SECONDS)
        val match = Regex("version \"(\\d+)").find(output) ?: return@runCatching 8
        val major = match.groupValues[1].toInt()
        if (major == 1) 8 else major
    }.getOrDefault(8)
}
