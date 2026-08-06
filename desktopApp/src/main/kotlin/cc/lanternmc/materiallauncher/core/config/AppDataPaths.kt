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
package cc.lanternmc.materiallauncher.core.config

import java.io.File
import cc.lanternmc.materiallauncher.core.util.Os
import cc.lanternmc.materiallauncher.core.util.currentOs

data class AppDataPaths(
    val directory: String,
    val config: String,
    val javaIndex: String,
)

/**
 * 解析启动器数据目录：
 * 1. 显式覆盖目录（用户设置，存于 data-dir.txt）优先
 * 2. 可执行程序同级 data/ 目录
 * 3. 不可写时回退到系统配置目录（%APPDATA%/MaterialLauncher）
 */
object AppDataPathsResolver {
    fun resolve(): AppDataPaths {
        // 1. 用户显式指定的数据目录（data-dir.txt 记录）
        val overrideDir = readDataDirOverride()
        if (overrideDir != null) {
            val prepared = runCatching { prepare(overrideDir) }.getOrNull()
            if (prepared != null) return prepared
        }
        // 2. exe 同级 data/
        val primary = executableDir()?.let { File(it, "data").absolutePath }
        if (primary != null) {
            val prepared = runCatching { prepare(primary) }.getOrNull()
            if (prepared != null) return prepared
        }
        // 3. 系统配置目录回退
        val fallback = File(userConfigDir(), "MaterialLauncher").absolutePath
        return prepare(fallback)
    }

    /** 数据目录标志文件名（存放用户自定义的数据目录绝对路径）。 */
    private const val DATA_DIR_FLAG = "data-dir.txt"

    /** 读取用户自定义数据目录（若存在且非空）。 */
    fun readDataDirOverride(): String? = runCatching {
        // 标志文件可能位于 exe 同级或系统配置目录
        val candidates = listOfNotNull(
            executableDir()?.let { File(it, DATA_DIR_FLAG) },
            File(userConfigDir(), "MaterialLauncher/$DATA_DIR_FLAG"),
        )
        candidates.firstOrNull { it.isFile }?.readText()?.trim()?.takeIf { it.isNotEmpty() }
    }.getOrNull()

    /**
     * 设置自定义数据目录：写入 exe 同级 data-dir.txt（不可写则写系统配置目录）。
     * 生效需重启启动器。
     */
    fun writeDataDirOverride(dir: String): Boolean = runCatching {
        val target = executableDir()?.let { File(it, DATA_DIR_FLAG) }
            ?: File(userConfigDir(), "MaterialLauncher/$DATA_DIR_FLAG")
        target.parentFile?.mkdirs()
        target.writeText(dir.trim())
        true
    }.getOrDefault(false)

    private fun prepare(directory: String): AppDataPaths {
        val dir = File(directory)
        if (!dir.exists() && !dir.mkdirs()) {
            throw IllegalStateException("无法创建数据目录: $directory")
        }
        val probe = File(dir, ".write-test-${System.nanoTime()}")
        try {
            probe.createNewFile()
        } finally {
            probe.delete()
        }
        val paths = AppDataPaths(
            directory = dir.absolutePath,
            config = File(dir, "config.toml").absolutePath,
            javaIndex = File(dir, "java-index.toml").absolutePath,
        )
        val configFile = File(paths.config)
        if (!configFile.exists()) {
            configFile.writeText("# Material Launcher configuration placeholder.\n")
        }
        return paths
    }

    private fun executableDir(): String? = runCatching {
        val cp = System.getProperty("java.class.path")
        cp.split(File.pathSeparator)
            .firstOrNull { it.isNotBlank() && !it.endsWith(".jar") }
            ?.let { File(it).absoluteFile.parentFile?.absolutePath }
            ?: File(cp).absoluteFile.parentFile?.absolutePath
    }.getOrNull()

    private fun userConfigDir(): String {
        val home = System.getProperty("user.home") ?: "."
        return when (currentOs) {
            Os.WINDOWS -> System.getenv("APPDATA") ?: File(home, "AppData/Roaming").absolutePath
            Os.MAC -> File(home, "Library/Application Support").absolutePath
            else -> File(home, ".config").absolutePath
        }
    }
}
