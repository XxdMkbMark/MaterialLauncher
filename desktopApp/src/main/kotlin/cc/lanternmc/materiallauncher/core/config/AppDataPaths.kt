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
 * 优先使用可执行程序同级 data/ 目录；不可写时回退到系统配置目录。
 */
object AppDataPathsResolver {
    fun resolve(): AppDataPaths {
        val primary = executableDir()?.let { File(it, "data").absolutePath }
        if (primary != null) {
            val prepared = runCatching { prepare(primary) }.getOrNull()
            if (prepared != null) return prepared
        }
        val fallback = File(userConfigDir(), "MaterialLauncher").absolutePath
        return prepare(fallback)
    }

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
