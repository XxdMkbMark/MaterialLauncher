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

import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.ArrayDeque
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 简单的彩色无依赖日志。
 *
 * 支持注册 [LogListener]，每条日志（INFO 及以上）都会推送给监听器，
 * 用于把日志转发到 UI 面板。
 */
object Logger {
    enum class Level { DEBUG, INFO, WARN, ERROR }

    @Volatile
    var level: Level = Level.INFO

    /** 日志条目（含时间戳），供 UI 展示。 */
    data class Entry(val level: Level, val message: String, val time: String)

    /** 历史缓冲最大条数（UI 面板首次打开时先回放最近日志）。 */
    const val MAX_HISTORY = 500

    private val fmt = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    private val listeners = CopyOnWriteArrayList<(Entry) -> Unit>()

    private val historyLock = Any()
    private val history = ArrayDeque<Entry>()

    init {
        // Windows 下 JVM 默认按平台编码(GBK)输出，中文在 UTF-8 控制台/IDE 里会乱码，这里强制 UTF-8。
        System.setOut(PrintStream(FileOutputStream(FileDescriptor.out), true, Charsets.UTF_8))
        System.setErr(PrintStream(FileOutputStream(FileDescriptor.err), true, Charsets.UTF_8))
    }

    /** 注册日志监听器（UI 面板用）。 */
    fun addListener(listener: (Entry) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (Entry) -> Unit) {
        listeners.remove(listener)
    }

    /** 返回最近 [MAX_HISTORY] 条日志（按时间正序），供 UI 回放。 */
    fun history(): List<Entry> = synchronized(historyLock) {
        history.toList()
    }

    /** 清空历史缓冲（不影响已注册监听器）。 */
    fun clearHistory() {
        synchronized(historyLock) {
            history.clear()
        }
    }

    fun debug(message: String) = log(Level.DEBUG, message)
    fun info(message: String) = log(Level.INFO, message)
    fun warn(message: String) = log(Level.WARN, message)
    fun error(message: String) = log(Level.ERROR, message)

    private fun log(level: Level, message: String) {
        if (level.ordinal < this.level.ordinal) return
        val time = LocalDateTime.now().format(fmt)
        val line = "[$time] [${level.name}] $message"
        if (level >= Level.WARN) System.err.println(line) else println(line)
        if (level >= Level.INFO) {
            val entry = Entry(level, message, time)
            synchronized(historyLock) {
                history.addLast(entry)
                while (history.size > MAX_HISTORY) history.removeFirst()
            }
            for (listener in listeners) {
                runCatching { listener(entry) }
            }
        }
    }
}
