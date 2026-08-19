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

import java.io.File
import java.security.MessageDigest

/** SHA-256 校验工具，用于 Java 归档等依赖官方 sha256 的下载场景。 */
object Sha256 {
    fun ofFile(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        // TODO Fix the repeated code segments
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * 校验文件 SHA-256 哈希。expectedHash 为空时跳过（视为未提供校验）。
     * 返回 true 表示校验通过（或无需校验），false 表示文件缺失或不匹配。
     */
    fun isFileValid(path: String, expectedHash: String?): Boolean {
        if (expectedHash.isNullOrBlank()) return true
        val file = File(path)
        if (!file.isFile) return false
        return ofFile(file).equals(expectedHash, ignoreCase = true)
    }
}
