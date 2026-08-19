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

/** Buffer size for the streaming digest loop: balances syscall cost and memory. */
private const val DIGEST_BUFFER_SIZE = 64 * 1024

/**
 * Stream [this] file through a [MessageDigest] identified by [algorithm]
 * (e.g. "SHA-1" / "SHA-256") and return the digest as a lowercase
 * hexadecimal string.
 *
 * Reads the file in fixed-size chunks via [File.inputStream] so the entire
 * file is never loaded into memory; safe for multi-gigabyte downloads.
 *
 * Throws [java.security.NoSuchAlgorithmException] if [algorithm] is not
 * available on the running JVM.
 */
fun File.digest(algorithm: String): String {
    val digest = MessageDigest.getInstance(algorithm)
    inputStream().use { input ->
        val buffer = ByteArray(DIGEST_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}