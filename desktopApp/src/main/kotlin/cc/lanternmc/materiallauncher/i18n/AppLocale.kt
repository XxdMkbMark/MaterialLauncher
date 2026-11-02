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
package cc.lanternmc.materiallauncher.i18n

import cc.lanternmc.materiallauncher.core.config.AppDataPathsResolver
import cc.lanternmc.materiallauncher.core.util.Toml
import java.io.File
import java.util.Locale

/**
 * 界面语言。只支持 zh-CN 与 en-US。
 * launcher.toml 里写了 ui.language 时用那个值；没写时跟随系统语言。
 * 系统或配置都不是中/英时回退中文。启动时写入 JVM 默认 Locale，
 * Compose 字符串资源据此选择 values/（中文）或 values-en/。改语言后需要重启。
 */
object AppLocale {
    const val CHINESE = "zh-CN"
    const val ENGLISH = "en-US"

    fun applyFromConfig() {
        val system = Locale.getDefault()
        Locale.setDefault(chooseLocale(readExplicitLanguageTag(), system))
    }

    /** launcher.toml 的 [ui] language。文件或键不存在时返回 null，表示跟随系统。 */
    fun readExplicitLanguageTag(): String? = runCatching {
        val file = File(AppDataPathsResolver.resolve().launcher)
        if (!file.isFile) return@runCatching null
        Toml.parse(file.readText()).section("ui").values["language"]?.trim()?.takeIf { it.isNotEmpty() }
    }.getOrNull()

    fun chooseLocale(explicitTag: String?, system: Locale): Locale {
        val tag = explicitTag?.trim()?.takeIf { it.isNotEmpty() } ?: system.toLanguageTag()
        return toJvmLocale(tag)
    }

    fun toJvmLocale(tag: String): Locale {
        val normalized = tag.trim().replace('_', '-')
        return when {
            normalized.equals("en", ignoreCase = true) ||
                normalized.startsWith("en-", ignoreCase = true) ->
                Locale.forLanguageTag(ENGLISH)
            normalized.equals("zh", ignoreCase = true) ||
                normalized.startsWith("zh-", ignoreCase = true) ->
                Locale.forLanguageTag(CHINESE)
            else -> Locale.forLanguageTag(CHINESE)
        }
    }
}
