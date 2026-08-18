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
import cc.lanternmc.materiallauncher.api.GlobalLaunchSettings
import cc.lanternmc.materiallauncher.core.util.Logger
import cc.lanternmc.materiallauncher.core.util.Os
import cc.lanternmc.materiallauncher.core.util.Toml
import cc.lanternmc.materiallauncher.core.util.currentOs

/**
 * global.toml：跨实例共享的全局启动默认值。
 * 旧版 config.toml 的同名 / 兼容字段由 [ConfigMigration] 在启动时迁移过来。
 *
 * 当前结构（扁平键，未分组）：
 *   minecraft_path / java_path / default_account_id / default_username /
 *   default_max_memory / default_jvm_args / default_game_args / download_source
 *
 * 字段以顶级 KV 形式持久化，便于后续无缝扩展（加字段不动 data class）。
 */
class GlobalConfigStore(private val path: String) {

    fun load(): GlobalLaunchSettings {
        val file = File(path)
        if (!file.isFile) return defaults()
        val doc = runCatching { Toml.parse(file.readText()) }.getOrNull()
        if (doc == null) {
            Logger.warn("解析 global.toml 失败，使用默认配置: $path")
            return defaults()
        }
        val v = doc.section("").values
        val def = defaults()
        return GlobalLaunchSettings(
            minecraftPath = v["minecraft_path"] ?: def.minecraftPath,
            javaPath = v["java_path"] ?: def.javaPath,
            defaultAccountId = v["default_account_id"] ?: def.defaultAccountId,
            defaultUsername = v["default_username"] ?: def.defaultUsername,
            defaultMaxMemory = v["default_max_memory"] ?: def.defaultMaxMemory,
            defaultJvmArgs = v["default_jvm_args"] ?: def.defaultJvmArgs,
            defaultGameArgs = v["default_game_args"] ?: def.defaultGameArgs,
            downloadSource = v["download_source"] ?: def.downloadSource,
        )
    }

    fun save(settings: GlobalLaunchSettings): Boolean = runCatching {
        val content = buildString {
            appendLine("# Material Launcher — global launch defaults (shared across instances)")
            appendLine()
            appendLine("minecraft_path = ${Toml.quote(settings.minecraftPath)}")
            appendLine("java_path = ${Toml.quote(settings.javaPath)}")
            appendLine("default_account_id = ${Toml.quote(settings.defaultAccountId)}")
            appendLine("default_username = ${Toml.quote(settings.defaultUsername)}")
            appendLine("default_max_memory = ${Toml.quote(settings.defaultMaxMemory)}")
            appendLine("default_jvm_args = ${Toml.quote(settings.defaultJvmArgs)}")
            appendLine("default_game_args = ${Toml.quote(settings.defaultGameArgs)}")
            appendLine("download_source = ${Toml.quote(settings.downloadSource)}")
        }
        File(path).parentFile?.mkdirs()
        val tmp = File("$path.tmp")
        tmp.writeText(content)
        val target = File(path)
        if (target.exists() && !target.delete()) {
            throw IllegalStateException("无法替换旧 global.toml")
        }
        if (!tmp.renameTo(target)) {
            throw IllegalStateException("无法写入 global.toml")
        }
        true
    }.getOrElse {
        Logger.error("保存 global.toml 失败: ${it.message}")
        false
    }

    fun defaultMinecraftDir(): String = when (currentOs) {
        Os.WINDOWS -> System.getenv("APPDATA")?.let { File(it, ".minecraft").absolutePath }.orEmpty()
        Os.MAC -> File(System.getProperty("user.home") ?: "", "Library/Application Support/minecraft").absolutePath
        else -> File(System.getProperty("user.home") ?: "", ".minecraft").absolutePath
    }

    fun defaults(): GlobalLaunchSettings = GlobalLaunchSettings(
        minecraftPath = defaultMinecraftDir(),
        javaPath = "",
        defaultAccountId = "",
        defaultUsername = "TestUser",
        defaultMaxMemory = "2048M",
        defaultJvmArgs = "",
        defaultGameArgs = "",
        downloadSource = "auto",
    )
}