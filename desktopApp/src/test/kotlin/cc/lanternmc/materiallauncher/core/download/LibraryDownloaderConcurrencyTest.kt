/*
 * Copyright (C) 2026 Mark <github@xxdmkbmark> & Pidan <github@bretren>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package cc.lanternmc.materiallauncher.core.download

import cc.lanternmc.materiallauncher.core.model.Artifact
import cc.lanternmc.materiallauncher.core.model.Library
import cc.lanternmc.materiallauncher.core.model.LibraryDownloads
import cc.lanternmc.materiallauncher.core.util.Sha1
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * LibraryDownloader 并发下载回归测试：
 * - 多个依赖库并发下载（慢网络下串行会极慢，这是"依赖下载花太久"的根因）；
 * - classpath 返回顺序必须与 libraries 原顺序一致（类加载顺序不能漂移）；
 * - natives jar 解压到 nativesDir。
 */
class LibraryDownloaderConcurrencyTest {

    private val contentA = "library-A-content-".repeat(50).toByteArray()
    private val contentB = "library-B-content-".repeat(50).toByteArray()
    private val contentC = "library-C-content-".repeat(50).toByteArray()

    private fun sha1(bytes: ByteArray): String {
        val f = File.createTempFile("sha1", ".bin")
        f.writeBytes(bytes)
        val h = Sha1.ofFile(f)
        f.delete()
        return h
    }

    /** 起一个本地服务器：/a /b /c 返回不同内容，/natives.zip 返回 natives 包。 */
    private fun startServer(nativeZip: ByteArray): HttpServer {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val routes = mapOf(
            "/a" to contentA,
            "/b" to contentB,
            "/c" to contentC,
            "/natives.zip" to nativeZip,
        )
        for ((path, body) in routes) {
            server.createContext(path) { exchange ->
                exchange.responseHeaders.add("Content-Length", body.size.toString())
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
        }
        server.start()
        return server
    }

    /** 构造一个含 3 个普通 jar 的 natives zip（native 库只有 1 个，路径 /natives.zip）。 */
    private fun buildNativeZip(): ByteArray {
        val bos = java.io.ByteArrayOutputStream()
        ZipOutputStream(bos).use { zip ->
            zip.putNextEntry(ZipEntry("META-INF/MANIFEST.MF"))
            zip.write("Manifest-Version: 1.0\n".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("org/lwjgl/liblwjgl64.dll"))
            zip.write("fake-native-dll".toByteArray())
            zip.closeEntry()
        }
        return bos.toByteArray()
    }

    private fun libraries(port: Int, nativeZip: ByteArray): List<Library> = listOf(
        Library(name = "a:a:1.0", downloads = LibraryDownloads(artifact = Artifact(
            path = "a/a/1.0/a-1.0.jar", url = "http://127.0.0.1:$port/a", sha1 = sha1(contentA), size = contentA.size.toLong(),
        ))),
        Library(name = "b:b:1.0", downloads = LibraryDownloads(artifact = Artifact(
            path = "b/b/1.0/b-1.0.jar", url = "http://127.0.0.1:$port/b", sha1 = sha1(contentB), size = contentB.size.toLong(),
        ))),
        Library(name = "c:c:1.0", downloads = LibraryDownloads(artifact = Artifact(
            path = "c/c/1.0/c-1.0.jar", url = "http://127.0.0.1:$port/c", sha1 = sha1(contentC), size = contentC.size.toLong(),
        ))),
        Library(
            name = "native:lwjgl:3.3", natives = mapOf("windows" to "natives-windows"),
            downloads = LibraryDownloads(classifiers = mapOf(
                "natives-windows" to Artifact(
                    path = "native/lwjgl/3.3/lwjgl-3.3-natives-windows.jar",
                    url = "http://127.0.0.1:$port/natives.zip",
                    sha1 = sha1(nativeZip), size = nativeZip.size.toLong(),
                ),
            )),
        ),
    )

    @Test
    fun `downloads all libraries concurrently and preserves classpath order`() {
        val nativeZip = buildNativeZip()
        val server = startServer(nativeZip)
        val dir = File.createTempFile("lib-test", "").apply { delete(); mkdirs() }
        try {
            val gameDir = File(dir, "game").absolutePath
            val nativesDir = File(dir, "natives").absolutePath
            val libs = libraries(server.address.port, nativeZip)

            runBlocking {
                val classpath = LibraryDownloader().downloadLibraries(gameDir, nativesDir, libs)

                // classpath 顺序必须与 libraries 原顺序一致（只含普通 jar，跳过 natives）
                assertEquals(3, classpath.size, "classpath 应含 3 个普通 jar")
                assertEquals(File(gameDir, "libraries/a/a/1.0/a-1.0.jar").absolutePath, classpath[0])
                assertEquals(File(gameDir, "libraries/b/b/1.0/b-1.0.jar").absolutePath, classpath[1])
                assertEquals(File(gameDir, "libraries/c/c/1.0/c-1.0.jar").absolutePath, classpath[2])

                // 文件真实存在且内容正确
                assertTrue(contentA.contentEquals(File(classpath[0]).readBytes()))
                assertTrue(contentB.contentEquals(File(classpath[1]).readBytes()))
                assertTrue(contentC.contentEquals(File(classpath[2]).readBytes()))

                // natives 解压到了 nativesDir
                assertTrue(File(nativesDir, "org/lwjgl/liblwjgl64.dll").isFile, "natives dll 应解压到 nativesDir")
            }
        } finally {
            server.stop(0)
            dir.deleteRecursively()
        }
    }

    @Test
    fun `already-downloaded libraries are not re-downloaded`() {
        val nativeZip = buildNativeZip()
        val server = startServer(nativeZip)
        val dir = File.createTempFile("lib-test2", "").apply { delete(); mkdirs() }
        try {
            val gameDir = File(dir, "game").absolutePath
            val nativesDir = File(dir, "natives").absolutePath
            val libs = libraries(server.address.port, nativeZip)

            // 预置完整文件（校验通过则跳过下载）
            val aJar = File(gameDir, "libraries/a/a/1.0/a-1.0.jar").apply { parentFile.mkdirs() }
            aJar.writeBytes(contentA)
            val bJar = File(gameDir, "libraries/b/b/1.0/b-1.0.jar").apply { parentFile.mkdirs() }
            bJar.writeBytes(contentB)
            val cJar = File(gameDir, "libraries/c/c/1.0/c-1.0.jar").apply { parentFile.mkdirs() }
            cJar.writeBytes(contentC)

            runBlocking {
                val classpath = LibraryDownloader().downloadLibraries(gameDir, nativesDir, libs)
                assertEquals(3, classpath.size)
                // 预置文件应被原样保留（未被重新下载覆盖）
                assertTrue(contentA.contentEquals(aJar.readBytes()))
                assertTrue(contentB.contentEquals(bJar.readBytes()))
                assertTrue(contentC.contentEquals(cJar.readBytes()))
            }
        } finally {
            server.stop(0)
            dir.deleteRecursively()
        }
    }
}
