/*
 * Copyright (C) 2026 Mark <github@xxdmkbmark> & Pidan <github@bretren>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package cc.lanternmc.materiallauncher.core.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RateMeterTest {

    @Test
    fun `computes speed from sliding window`() {
        var now = 0L
        val meter = RateMeter(now = { now })

        // 每秒 +1000 字节，共 5 秒
        var bytes = 0L
        var lastSpeed = 0L
        for (i in 1..5) {
            now = i * 1000L
            bytes += 1000
            lastSpeed = meter.record(bytes)
        }
        // 前两个采样点无速度，之后应接近 1000 B/s
        assertTrue(lastSpeed in 800..1200, "expected ~1000, got $lastSpeed")
    }

    @Test
    fun `resets on byte regression`() {
        var now = 0L
        val meter = RateMeter(now = { now })
        now = 1000
        meter.record(1000) // 首个采样，0
        now = 2000
        val speedBefore = meter.record(2000) // 1000 B/s
        assertTrue(speedBefore > 0)

        // 字节回退（镜像切换从头下）→ 清空采样
        now = 3000
        val afterReset = meter.record(500)
        assertEquals(0, afterReset)
        // 恢复后重新累积
        now = 4000
        meter.record(1500)
        now = 5000
        val again = meter.record(2500)
        assertTrue(again > 0)
    }

    @Test
    fun `first record returns zero`() {
        val meter = RateMeter(now = { 0L })
        assertEquals(0, meter.record(100))
    }
}
