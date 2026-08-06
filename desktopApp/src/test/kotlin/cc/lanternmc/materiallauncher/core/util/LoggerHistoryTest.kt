/*
 * Copyright (C) 2026 Mark <github@xxdmkbmark> & Pidan <github@bretren>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package cc.lanternmc.materiallauncher.core.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Logger 历史缓冲测试：UI 日志面板首次打开时回放最近日志，
 * 必须包含订阅事件流之前产生的日志（否则启动日志会丢失）。
 */
class LoggerHistoryTest {

    @Test
    fun `history contains entries logged before listener attached`() {
        Logger.clearHistory()
        Logger.info("before-1")
        Logger.warn("before-2")

        val history = Logger.history()
        assertEquals(2, history.size)
        assertEquals("before-1", history[0].message)
        assertEquals("before-2", history[1].message)
        assertEquals(Logger.Level.WARN, history[1].level)
    }

    @Test
    fun `history is capped at MAX_HISTORY`() {
        Logger.clearHistory()
        repeat(Logger.MAX_HISTORY + 50) { i -> Logger.info("msg-$i") }

        val history = Logger.history()
        assertEquals(Logger.MAX_HISTORY, history.size)
        // 最老的 50 条被挤出，剩余从 msg-50 开始
        assertEquals("msg-50", history.first().message)
        assertEquals("msg-${Logger.MAX_HISTORY + 49}", history.last().message)
    }

    @Test
    fun `debug entries are excluded from history at INFO level`() {
        Logger.clearHistory()
        Logger.level = Logger.Level.INFO
        Logger.debug("not-recorded")
        Logger.info("recorded")

        val history = Logger.history()
        assertEquals(1, history.size)
        assertEquals("recorded", history[0].message)
        assertTrue(history.none { it.message == "not-recorded" })
    }
}
