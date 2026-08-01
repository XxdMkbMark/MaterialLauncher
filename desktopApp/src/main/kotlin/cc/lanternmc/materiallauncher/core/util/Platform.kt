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

enum class Os { WINDOWS, MAC, LINUX, OTHER }

val currentOs: Os
    get() {
        val name = System.getProperty("os.name").lowercase()
        return when {
            name.contains("win") -> Os.WINDOWS
            name.contains("mac") || name.contains("darwin") -> Os.MAC
            name.contains("linux") -> Os.LINUX
            else -> Os.OTHER
        }
    }

val isArm64: Boolean
    get() {
        val arch = System.getProperty("os.arch").lowercase()
        return arch.contains("aarch64") || arch.contains("arm64")
    }

val is32Bit: Boolean
    get() {
        val arch = System.getProperty("os.arch").lowercase()
        return (arch.contains("x86") || arch.contains("i386")) && !arch.contains("x86_64") && !arch.contains("amd64")
    }
