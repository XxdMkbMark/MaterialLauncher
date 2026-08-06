/*
 * Copyright (C) 2026 Mark <github@xxdmkbmark> & Pidan <github@bretren>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package cc.lanternmc.materiallauncher.core.util

import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 断点续传下载的两个回归测试：
 * 1. 服务器忽略 Range（返回 200）时，不能把新内容追加到旧 .part 后面（拼接损坏）；
 * 2. 目标文件已存在但损坏时，force=true 必须真正重新下载（不被"已存在"逻辑跳过）。
 */
class HttpUtilResumeBugTest {

    private val content = "The quick brown fox jumps over the lazy dog. ".repeat(100).toByteArray()

    /** 起一个忽略 Range 的本地服务器（总是返回 200 全量）。 */
    private fun startNoRangeServer(): HttpServer {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/no-range") { exchange ->
            val body = content
            exchange.responseHeaders.add("Content-Length", body.size.toString())
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        return server
    }

    /** 起一个支持 Range 的本地服务器（206 续传）。 */
    private fun startRangeServer(): HttpServer {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/range") { exchange ->
            val range = exchange.requestHeaders.getFirst("Range")
            if (range == null) {
                exchange.responseHeaders.add("Content-Length", content.size.toString())
                exchange.sendResponseHeaders(200, content.size.toLong())
                exchange.responseBody.use { it.write(content) }
            } else {
                val from = range.removePrefix("bytes=").substringBefore('-').toLong()
                val slice = content.copyOfRange(from.toInt(), content.size)
                exchange.responseHeaders.add("Content-Range", "bytes $from-${content.size - 1}/${content.size}")
                exchange.responseHeaders.add("Content-Length", slice.size.toString())
                exchange.sendResponseHeaders(206, slice.size.toLong())
                exchange.responseBody.use { it.write(slice) }
            }
        }
        server.start()
        return server
    }

    @Test
    fun `ignored Range must truncate part file not append`() {
        val server = startNoRangeServer()
        val dest = File.createTempFile("resume-bug1", ".bin").absolutePath
        try {
            runBlocking {
                // 预置一个"残留部分文件"：旧数据只有前 100 字节（模拟上次中断）
                File("$dest.part").writeBytes(content.copyOfRange(0, 100))

                HttpUtil.downloadFile("http://127.0.0.1:${server.address.port}/no-range", dest, onProgress = { _, _ -> })

                // 服务器忽略 Range 返回 200 全量：最终文件必须是完整内容，长度 == content.size
                val result = File(dest).readBytes()
                assertEquals(content.size, result.size, "文件被拼接损坏：大小应为 ${content.size}")
                assertTrue(content.contentEquals(result), "文件内容应与服务器一致")
                assertFalse(File("$dest.part").exists(), ".part 应已被移动")
            }
        } finally {
            server.stop(0)
            File(dest).delete()
            File("$dest.part").delete()
        }
    }

    @Test
    fun `force redownloads when dest exists but is corrupt`() {
        val server = startRangeServer()
        val dest = File.createTempFile("resume-bug2", ".bin").absolutePath
        try {
            runBlocking {
                // 预置一个"已下载但损坏"的目标文件
                File(dest).writeBytes("corrupt-data".toByteArray())

                // force=true：必须重新下载，覆盖损坏文件
                HttpUtil.downloadFile(
                    "http://127.0.0.1:${server.address.port}/range",
                    dest,
                    onProgress = { _, _ -> },
                    force = true,
                )

                val result = File(dest).readBytes()
                assertTrue(content.contentEquals(result), "force 重下后文件应与服务器一致")
            }
        } finally {
            server.stop(0)
            File(dest).delete()
            File("$dest.part").delete()
        }
    }

    @Test
    fun `normal resume works with Range server`() {
        val server = startRangeServer()
        val dest = File.createTempFile("resume-ok", ".bin").absolutePath
        try {
            runBlocking {
                // 预置部分下载（前 100 字节），模拟中断后续传
                File("$dest.part").writeBytes(content.copyOfRange(0, 100))

                HttpUtil.downloadFile("http://127.0.0.1:${server.address.port}/range", dest, onProgress = { _, _ -> })

                val result = File(dest).readBytes()
                assertEquals(content.size, result.size)
                assertTrue(content.contentEquals(result), "206 续传后文件应完整")
            }
        } finally {
            server.stop(0)
            File(dest).delete()
            File("$dest.part").delete()
        }
    }
}
