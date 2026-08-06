/*
 * Copyright (C) 2026 Mark <github@xxdmkbmark> & Pidan <github@bretren>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package cc.lanternmc.materiallauncher.core.util

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class HttpUtilRetryTest {

    @Test
    fun `getString retries after transient connection failure`() = runBlocking {
        // 本地服务器：第一次请求直接关闭连接（模拟网络抖动），之后正常返回。
        val server = HttpServer.create(InetSocketAddress(0), 0)
        var requests = 0
        server.createContext("/flaky") { exchange ->
            requests++
            if (requests == 1) {
                exchange.close() // 不写响应直接断开
            } else {
                val body = "ok-${requests}".toByteArray()
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
        }
        server.start()
        try {
            val port = server.address.port
            val result = HttpUtil.getString("http://127.0.0.1:$port/flaky", timeoutSeconds = 5)
            assertEquals("ok-2", result)
            assertEquals(2, requests) // 第一次失败 + 第二次成功
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `getString does not retry on http error status`() = runBlocking {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        var requests = 0
        server.createContext("/error") { exchange ->
            requests++
            exchange.sendResponseHeaders(404, -1)
            exchange.close()
        }
        server.start()
        try {
            val port = server.address.port
            val failed = runCatching {
                HttpUtil.getString("http://127.0.0.1:$port/error", timeoutSeconds = 5)
            }.exceptionOrNull()
            assertEquals("HTTP 404", failed?.message)
            assertEquals(1, requests) // 4xx 不重试
        } finally {
            server.stop(0)
        }
    }
}
