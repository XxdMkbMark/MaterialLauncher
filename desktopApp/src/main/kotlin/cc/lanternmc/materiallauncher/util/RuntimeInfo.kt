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
package cc.lanternmc.materiallauncher.util

import java.lang.management.ManagementFactory

/** JVM / 宿主机运行时信息，用于内存建议等功能。 */
object RuntimeInfo {

    /**
     * 宿主机物理内存总量（GB，向下取整）。
     * 通过 OperatingSystemMXBean 获取；失败时回退到 JVM 最大堆内存的近似值。
     */
    fun totalPhysicalMemoryGb(): Int {
        val bytes = runCatching {
            val bean = ManagementFactory.getOperatingSystemMXBean()
            if (bean is com.sun.management.OperatingSystemMXBean) {
                bean.totalPhysicalMemorySize
            } else {
                -1L
            }
        }.getOrDefault(-1L)
        if (bytes > 0) return (bytes / (1024L * 1024L * 1024L)).toInt().coerceAtLeast(1)
        // 回退：按 JVM 可用处理器与默认堆估算，避免返回 0。
        return ((Runtime.getRuntime().maxMemory() / (1024L * 1024L * 1024L)).toInt() + 1).coerceAtLeast(2)
    }
}
