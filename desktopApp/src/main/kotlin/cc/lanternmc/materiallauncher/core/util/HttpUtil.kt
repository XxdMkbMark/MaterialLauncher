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

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class HttpResult(val statusCode: Int, val body: String)

/**
 * 基于 java.net.http 的轻量 HTTP 工具，全部在 IO 线程执行。
 */
object HttpUtil {
    val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    suspend fun getString(url: String, timeoutSeconds: Long = 60): String =
        retryTransient {
            withContext(Dispatchers.IO) {
                val request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .GET()
                    .build()
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                check(response.statusCode() in 200..299) { "HTTP ${response.statusCode()}" }
                response.body()
            }
        }

    /**
     * 携带自定义请求头的 GET，返回原始状态码与响应体。
     */
    suspend fun getResult(
        url: String,
        headers: Map<String, String> = emptyMap(),
        timeoutSeconds: Long = 60,
    ): HttpResult = withContext(Dispatchers.IO) {
        val builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(timeoutSeconds))
        for ((name, value) in headers) builder.header(name, value)
        val request = builder.GET().build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        HttpResult(response.statusCode(), response.body())
    }

    /**
     * application/x-www-form-urlencoded 表单 POST，非 2xx 抛异常。
     */
    suspend fun postForm(
        url: String,
        params: Map<String, String>,
        timeoutSeconds: Long = 60,
    ): String {
        val result = postFormResult(url, params, timeoutSeconds)
        check(result.statusCode in 200..299) { "HTTP ${result.statusCode}: ${result.body}" }
        return result.body
    }

    /**
     * 表单 POST，返回原始状态码与响应体（轮询授权等场景需自行判断错误码）。
     */
    suspend fun postFormResult(
        url: String,
        params: Map<String, String>,
        timeoutSeconds: Long = 60,
    ): HttpResult = withContext(Dispatchers.IO) {
        val form = params.entries.joinToString("&") { (key, value) ->
            URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8")
        }
        val request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        HttpResult(response.statusCode(), response.body())
    }

    /**
     * application/json POST，非 2xx 抛异常。
     */
    suspend fun postJson(
        url: String,
        json: String,
        headers: Map<String, String> = emptyMap(),
        timeoutSeconds: Long = 60,
    ): String {
        val result = postJsonResult(url, json, headers, timeoutSeconds)
        check(result.statusCode in 200..299) { "HTTP ${result.statusCode}: ${result.body}" }
        return result.body
    }

    /**
     * application/json POST，返回原始状态码与响应体。
     */
    suspend fun postJsonResult(
        url: String,
        json: String,
        headers: Map<String, String> = emptyMap(),
        timeoutSeconds: Long = 60,
    ): HttpResult = withContext(Dispatchers.IO) {
        val builder = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
        for ((name, value) in headers) builder.header(name, value)
        val request = builder.POST(HttpRequest.BodyPublishers.ofString(json)).build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        HttpResult(response.statusCode(), response.body())
    }

    suspend fun getBytes(url: String, timeoutSeconds: Long = 120): ByteArray =
        retryTransient {
            withContext(Dispatchers.IO) {
                val request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .GET()
                    .build()
                val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())
                check(response.statusCode() in 200..299) { "HTTP ${response.statusCode()}" }
                response.body()
            }
        }

    /**
     * 下载到临时文件后原子重命名，下载过程中以 ≤100ms 频率回调进度。
     * 网络抖动（连接失败/超时）时自动重试，最多 [MAX_RETRIES] 次。
     */
    suspend fun downloadFile(
        url: String,
        dest: String,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ): Unit = retryTransient {
        withContext(Dispatchers.IO) {
            val tmp = Path.of("$dest.tmp")
            try {
                val request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(120))
                    .GET()
                    .build()
                val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
                check(response.statusCode() in 200..299) { "HTTP ${response.statusCode()}" }
                val total = response.headers().firstValueAsLong("Content-Length").orElse(-1L)
                Files.createDirectories(tmp.parent)
                var downloaded = 0L
                var lastReport = System.currentTimeMillis()
                response.body().use { input ->
                    Files.newOutputStream(tmp).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            val now = System.currentTimeMillis()
                            if (now - lastReport >= 100) {
                                lastReport = now
                                onProgress(downloaded, total)
                            }
                        }
                    }
                }
                onProgress(downloaded, total)
                Files.move(tmp, Path.of(dest), StandardCopyOption.REPLACE_EXISTING)
            } finally {
                Files.deleteIfExists(tmp)
            }
        }
    }

    /** 临时性网络异常（连接失败/超时/IO 错误）时的最大重试次数。 */
    private const val MAX_RETRIES = 3

    /**
     * 仅在抛出网络层异常时重试（指数退避：500ms/1s/2s），
     * 非 2xx 状态码由调用方自行 check，不在此重试。
     */
    private suspend fun <T> retryTransient(block: suspend () -> T): T {
        var attempt = 0
        while (true) {
            try {
                return block()
            } catch (e: Exception) {
                val retryable = e !is IllegalArgumentException &&
                    e !is IllegalStateException &&
                    !isHttpStatusError(e)
                if (!retryable || attempt >= MAX_RETRIES) throw e
                attempt++
                Logger.warn("网络请求失败，第 $attempt 次重试: ${e.message}")
                delay(500L * attempt)
            }
        }
    }

    private fun isHttpStatusError(e: Exception): Boolean {
        val msg = e.message ?: return false
        return msg.startsWith("HTTP ")
    }
}
