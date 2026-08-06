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
     * 断点续传下载：先写 `dest.part`，中断时保留该部分文件，下次调用自动
     * 携带 Range 头从断点继续；全部完成后原子重命名为最终文件。
     *
     * 服务器不支持 Range（返回 200 而非 206）时自动从头重下。
     * [force] 为 true 时忽略已存在的最终文件（调用方校验失败后强制重下）。
     * 网络抖动（连接失败/超时）时自动重试，最多 [MAX_RETRIES] 次。
     * [onProgress] 报告整个文件的累计下载进度（含续传部分）。
     */
    suspend fun downloadFile(
        url: String,
        dest: String,
        onProgress: (downloaded: Long, total: Long) -> Unit,
        force: Boolean = false,
    ): Unit = retryTransient {
        withContext(Dispatchers.IO) {
            val part = Path.of("$dest.part")
            Files.createDirectories(part.parent)
            var existing = if (Files.exists(part)) Files.size(part) else 0L
            // 已完成的部分恰为完整文件（上次移动失败）时直接复用；空文件不算完成。
            // 注意：校验失败的调用方必须传 force=true，否则损坏的 dest 会被当作"已下载"跳过。
            val finalPath = Path.of(dest)
            if (!force && existing == 0L && Files.exists(finalPath) && Files.size(finalPath) > 0) {
                // 清理可能残留的 .part（目标已完整，无需续传）
                if (Files.exists(part)) Files.deleteIfExists(part)
                return@withContext
            }

            val builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(120))
            if (existing > 0) {
                builder.header("Range", "bytes=$existing-")
            }
            val response = client.send(builder.GET().build(), HttpResponse.BodyHandlers.ofInputStream())

            val status = response.statusCode()
            // 206 Partial Content：续传；200：服务器忽略 Range，从头下
            val resume = status == 206 && existing > 0
            if (!resume) existing = 0L
            check(status in 200..299) { "HTTP $status" }

            val contentRangeTotal = response.headers().firstValue("Content-Range")
                .orElse(null)?.substringAfter('/')?.toLongOrNull()
            val totalHeader = response.headers().firstValueAsLong("Content-Length").orElse(-1L)
            val total = when {
                contentRangeTotal != null && contentRangeTotal > 0 -> contentRangeTotal
                totalHeader > 0 && existing > 0 -> existing + totalHeader
                totalHeader > 0 -> totalHeader
                else -> -1L
            }

            var downloaded = existing
            var lastReport = System.currentTimeMillis()
            response.body().use { input ->
                // 续传时追加；从头下载时截断（覆盖 .part 里残留的旧数据，避免拼接损坏）
                val openOptions = if (resume) {
                    arrayOf(
                        java.nio.file.StandardOpenOption.APPEND,
                        java.nio.file.StandardOpenOption.CREATE,
                    )
                } else {
                    arrayOf(
                        java.nio.file.StandardOpenOption.WRITE,
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                    )
                }
                Files.newOutputStream(part, *openOptions).use { output ->
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
            // 完成后原子移动为最终文件；若用户期望完整文件则校验由调用方负责
            Files.move(part, finalPath, StandardCopyOption.REPLACE_EXISTING)
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
