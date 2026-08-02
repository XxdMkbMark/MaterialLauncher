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
package cc.lanternmc.materiallauncher.core.util

/**
 * 极简 TOML 读写，仅支持本项目用到的语法：
 * `key = "value"` / `section` / `array`。
 */
object Toml {
    class Document {
        private val table = LinkedHashMap<String, Section>()
        fun section(name: String): Section = table.getOrPut(name) { Section() }
    }

    class Section {
        val values = LinkedHashMap<String, String>()
        val items = mutableListOf<LinkedHashMap<String, String>>()
    }

    fun parse(text: String): Document {
        val doc = Document()
        var currentSection: Section? = doc.section("")
        var currentItem: MutableMap<String, String>? = null
        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            when {
                line.startsWith("[[") && line.endsWith("]]") -> {
                    val name = line.substring(2, line.length - 2).trim()
                    val section = doc.section(name)
                    section.items.add(LinkedHashMap())
                    currentSection = section
                    currentItem = section.items.last()
                }
                line.startsWith("[") && line.endsWith("]") -> {
                    val name = line.substring(1, line.length - 1).trim()
                    val section = doc.section(name)
                    currentSection = section
                    currentItem = null
                }
                else -> {
                    val idx = line.indexOf('=')
                    if (idx < 0) continue
                    val key = line.substring(0, idx).trim()
                    val raw = line.substring(idx + 1).trim()
                    val value = unquote(raw)
                    if (currentItem != null) {
                        currentItem[key] = value
                    } else {
                        currentSection?.values?.put(key, value)
                    }
                }
            }
        }
        return doc
    }

    fun quote(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private fun unquote(value: String): String {
        val v = value.trim()
        if (v.length >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
            return v.substring(1, v.length - 1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
        }
        return v
    }
}
