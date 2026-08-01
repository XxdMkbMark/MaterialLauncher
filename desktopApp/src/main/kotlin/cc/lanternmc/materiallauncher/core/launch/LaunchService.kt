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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import cc.lanternmc.materiallauncher.api.LauncherEvent
import cc.lanternmc.materiallauncher.core.download.AssetDownloader
import cc.lanternmc.materiallauncher.core.download.LibraryDownloader
import cc.lanternmc.materiallauncher.core.model.VersionJson
import cc.lanternmc.materiallauncher.core.util.Logger
import cc.lanternmc.materiallauncher.core.util.Os
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
        OptionsSanitizer.sanitize(File(gameDir))

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

        Thread {
            process.inputStream.bufferedReader(Charsets.UTF_8).forEachLine { line ->
                Logger.info("[MC] $line")
                if (line.contains("Setting user:") || line.contains("Launched game")) {
                    emit(LauncherEvent.GameReady(pid))
                }
            }
            Logger.info("[MC] stdout 已关闭")
        }.apply { isDaemon = true }.start()

        Thread {
            process.errorStream.bufferedReader(Charsets.UTF_8).forEachLine { line ->
                Logger.warn("[MC-ERR] $line")
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
}
