/*
 * Copyright (C) 2026 Mark <github@xxdmkbmark> & Pidan <github@bretren>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package cc.lanternmc.materiallauncher.core.util

import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class HttpUtilResumeTest {

    private val content = ByteArray(200_000) { (it % 251).toByte() }

    /** 支持 Range 的本地服务器。 */
    private fun startRangeServer(): HttpServer {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/file") { exchange ->
            val range = exchange.requestHeaders.getFirst("Range")
            if (range == null) {
                exchange.sendResponseHeaders(200, content.size.toLong())
                exchange.responseBody.use { it.write(content) }
            } else {
                val start = range.removePrefix("bytes=").substringBefore('-').toInt()
                val body = content.copyOfRange(start, content.size)
                exchange.responseHeaders.add("Content-Range", "bytes $start-${content.size - 1}/${content.size}")
                exchange.sendResponseHeaders(206, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
        }
        server.start()
        return server
    }

    @Test
    fun `resumes from existing part file`() = runBlocking {
        val server = startRangeServer()
        val dest = File.createTempFile("resume", ".bin").absolutePath
        try {
            // 第一次调用前预置 100_000 字节的部分文件（模拟上次中断）
            val part = File("$dest.part")
            part.writeBytes(content.copyOfRange(0, 100_000))

            var lastProgress = 0L
            HttpUtil.downloadFile("http://127.0.0.1:${server.address.port}/file", dest, onProgress = { done, _ ->
                lastProgress = done
            })

            // 最终文件完整
            val finalFile = File(dest)
            assertTrue(finalFile.isFile)
            assertContentEquals(content, finalFile.readBytes())
            // 进度包含续传部分
            assertEquals(content.size.toLong(), lastProgress)
            // 部分文件已清理
            assertFalse(File("$dest.part").exists())
        } finally {
            server.stop(0)
            File(dest).delete()
            File("$dest.part").delete()
        }
    }

    @Test
    fun `fresh download works without part file`() = runBlocking {
        val server = startRangeServer()
        val dest = File.createTempFile("fresh", ".bin").absolutePath
        try {
            HttpUtil.downloadFile("http://127.0.0.1:${server.address.port}/file", dest, onProgress = { _, _ -> })
            assertContentEquals(content, File(dest).readBytes())
        } finally {
            server.stop(0)
            File(dest).delete()
            File("$dest.part").delete()
        }
    }
}
