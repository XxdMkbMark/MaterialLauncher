/*
 * Copyright (C) 2026 Mark <github@xxdmkbmark> & Pidan <github@bretren>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package cc.lanternmc.materiallauncher.core.util

import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoggerListenerTest {

    @Test
    fun `listener receives info and warn entries`() {
        val received = CopyOnWriteArrayList<Logger.Entry>()
        val listener: (Logger.Entry) -> Unit = { received.add(it) }
        Logger.addListener(listener)
        try {
            Logger.info("hello info")
            Logger.warn("hello warn")
            // 等一小会确保异步无影响（实际是同步调用）
            Thread.sleep(50)
            assertTrue(received.size >= 2)
            val info = received.first { it.level == Logger.Level.INFO }
            assertEquals("hello info", info.message)
            val warn = received.first { it.level == Logger.Level.WARN }
            assertEquals("hello warn", warn.message)
            // 时间戳非空
            assertTrue(info.time.isNotBlank())
        } finally {
            Logger.removeListener(listener)
        }
    }

    @Test
    fun `removed listener stops receiving`() {
        val received = CopyOnWriteArrayList<Logger.Entry>()
        val listener: (Logger.Entry) -> Unit = { received.add(it) }
        Logger.addListener(listener)
        Logger.removeListener(listener)
        Logger.info("should not arrive")
        Thread.sleep(50)
        assertTrue(received.isEmpty())
    }
}
