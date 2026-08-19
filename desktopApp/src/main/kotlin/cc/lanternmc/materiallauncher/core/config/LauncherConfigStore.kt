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
import cc.lanternmc.materiallauncher.api.LauncherSettings
import cc.lanternmc.materiallauncher.core.util.Logger
import cc.lanternmc.materiallauncher.core.util.Toml

/**
 * launcher.toml：启动器本体配置（与具体实例/游戏无关）。
 * 当前结构：
 *   ui      theme / language
 *   download source / concurrency
 *
 * 文件不存在或解析失败时回退到 [LauncherSettings] 默认值，保证启动器始终可用。
 */
class LauncherConfigStore(private val path: String) {

    fun load(): LauncherSettings {
        val file = File(path)
        if (!file.isFile) return LauncherSettings()
        val doc = runCatching { Toml.parse(file.readText()) }.getOrNull()
        if (doc == null) {
            Logger.warn("解析 launcher.toml 失败，使用默认配置: $path")
            return LauncherSettings()
        }
        val defaults = LauncherSettings()
        val ui = doc.section("ui")
        val download = doc.section("download")
        return LauncherSettings(
            ui = defaults.ui.copy(
                theme = ui.values["theme"] ?: defaults.ui.theme,
                language = ui.values["language"] ?: defaults.ui.language,
            ),
            download = defaults.download.copy(
                source = download.values["source"] ?: defaults.download.source,
                concurrency = download.values["concurrency"]?.toIntOrNull() ?: defaults.download.concurrency,
            ),
        )
    }

    fun save(settings: LauncherSettings): Boolean = runCatching {
        val content = buildString {
            appendLine("# Material Launcher — launcher-level settings")
            appendLine()
            appendLine("[ui]")
            appendLine("theme = ${Toml.quote(settings.ui.theme)}")
            appendLine("language = ${Toml.quote(settings.ui.language)}")
            appendLine()
            appendLine("[download]")
            appendLine("source = ${Toml.quote(settings.download.source)}")
            appendLine("concurrency = ${settings.download.concurrency}")
        }
        ConfigIO.writeAtomically(File(path), content)
        true
    }.getOrElse {
        Logger.error("保存 launcher.toml 失败: ${it.message}")
        false
    }
}