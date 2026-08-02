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
import cc.lanternmc.materiallauncher.api.DownloadConfig
import cc.lanternmc.materiallauncher.api.DownloadPathConfig
import cc.lanternmc.materiallauncher.core.util.Logger
import cc.lanternmc.materiallauncher.core.util.Os
import cc.lanternmc.materiallauncher.core.util.Toml
import cc.lanternmc.materiallauncher.core.util.currentOs

/**
 * download.toml 配置的读写。
 */
class DownloadConfigStore(private val paths: AppDataPaths) {

    fun defaultMinecraftDir(): String = when (currentOs) {
        Os.WINDOWS -> System.getenv("APPDATA")?.let { File(it, ".minecraft").absolutePath }.orEmpty()
        Os.MAC -> File(System.getProperty("user.home") ?: "", "Library/Application Support/minecraft").absolutePath
        else -> File(System.getProperty("user.home") ?: "", ".minecraft").absolutePath
    }

    fun launcherMinecraftDir(): String = File(paths.directory, "minecraft").absolutePath

    fun launcherJavaDir(): String = File(paths.directory, "java").absolutePath

    fun load(): DownloadConfig {
        val configFile = File(paths.config)
        if (!configFile.isFile) return defaults()
        val doc = runCatching { Toml.parse(configFile.readText()) }.getOrNull()
        if (doc == null) {
            Logger.warn("解析配置文件失败，使用默认配置: ${paths.config}")
            return defaults()
        }
        val defaults = defaults()
        val mc = doc.section("minecraft")
        val java = doc.section("java")
        val account = doc.section("account")
        return DownloadConfig(
            minecraft = DownloadPathConfig(
                path = mc.values["path"] ?: defaults.minecraft.path,
                source = mc.values["source"] ?: "default",
            ),
            java = DownloadPathConfig(
                path = java.values["path"] ?: defaults.java.path,
                source = java.values["source"] ?: "launcher",
            ),
            username = account.values["username"] ?: defaults.username,
            accountId = account.values["account_id"] ?: defaults.accountId,
        )
    }

    fun save(config: DownloadConfig): Boolean = runCatching {
        val content = buildString {
            appendLine("# Material Launcher download paths")
            appendLine()
            appendLine("[minecraft]")
            appendLine("path = ${Toml.quote(config.minecraft.path)}")
            appendLine("source = ${Toml.quote(config.minecraft.source)}")
            appendLine()
            appendLine("[java]")
            appendLine("path = ${Toml.quote(config.java.path)}")
            appendLine("source = ${Toml.quote(config.java.source)}")
            appendLine()
            appendLine("[account]")
            appendLine("username = ${Toml.quote(config.username)}")
            appendLine("account_id = ${Toml.quote(config.accountId)}")
        }
        val tmp = File("${paths.config}.tmp")
        tmp.writeText(content)
        val target = File(paths.config)
        if (target.exists() && !target.delete()) {
            throw IllegalStateException("无法替换旧配置文件")
        }
        if (!tmp.renameTo(target)) {
            throw IllegalStateException("无法写入配置文件")
        }
        true
    }.getOrElse {
        Logger.error("保存配置文件失败: ${it.message}")
        false
    }

    private fun defaults() = DownloadConfig(
        minecraft = DownloadPathConfig(path = defaultMinecraftDir(), source = "default"),
        java = DownloadPathConfig(path = launcherJavaDir(), source = "launcher"),
    )
}
