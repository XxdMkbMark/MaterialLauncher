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
 * 下载速率计算器：根据累计已下载字节数推算瞬时速度（字节/秒）。
 *
 * 用最近 [WINDOW] 个采样点的滑动窗口求平均，避免单个采样抖动。
 */
class RateMeter(private val now: () -> Long = { System.currentTimeMillis() }) {

    companion object {
        private const val WINDOW = 8
    }

    private data class Sample(val bytes: Long, val timeMs: Long)

    private val samples = ArrayDeque<Sample>()
    private var lastBytes = 0L

    /**
     * 记录一次进度（累计下载字节数），返回瞬时速度（字节/秒）。
     * 首次调用或字节数回退时返回 0。
     */
    fun record(totalBytes: Long): Long {
        val time = now()
        if (totalBytes < lastBytes) {
            samples.clear()
            lastBytes = totalBytes
            return 0
        }

        val speed = if (samples.isNotEmpty()) {
            val first = samples.first()
            val elapsedMs = time - first.timeMs
            if (elapsedMs > 0) {
                val bytes = totalBytes - first.bytes
                bytes * 1000L / elapsedMs
            } else 0L
        } else 0L

        samples.addLast(Sample(totalBytes, time))
        while (samples.size > WINDOW) samples.removeFirst()
        lastBytes = totalBytes

        return speed
    }

    /** 重置（新下载开始时调用）。 */
    fun reset() {
        samples.clear()
        lastBytes = 0L
    }
}
