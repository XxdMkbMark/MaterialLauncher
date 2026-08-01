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
import cc.lanternmc.materiallauncher.core.util.Logger

/**
 * 净化 options.txt，防止界面文字混乱或全屏卡死。
 */
object OptionsSanitizer {

    fun sanitize(gameDir: File) {
        val optionsPath = File(gameDir, "options.txt")
        if (!optionsPath.isFile) {
            val defaults = "fullscreen:false\noverrideWidth:0\noverrideHeight:0\nfov:0.5\nguiScale:3\n"
            optionsPath.writeText(defaults)
            return
        }
        val newLines = mutableListOf<String>()
        optionsPath.forEachLine { line ->
            var modified = line
            when {
                line.startsWith("fullscreen:") -> modified = "fullscreen:false"
                line.startsWith("exclusiveFullscreen:") -> modified = "exclusiveFullscreen:false"
                line.startsWith("overrideWidth:") -> modified = "overrideWidth:0"
                line.startsWith("overrideHeight:") -> modified = "overrideHeight:0"
                line.startsWith("fov:") && line == "fov:0.0" -> modified = "fov:0.5"
                line.startsWith("guiScale:") && line == "guiScale:0" -> modified = "guiScale:3"
            }
            newLines.add(modified)
        }
        optionsPath.writeText(newLines.joinToString("\n"))
        Logger.info("options.txt 已净化")
    }
}
