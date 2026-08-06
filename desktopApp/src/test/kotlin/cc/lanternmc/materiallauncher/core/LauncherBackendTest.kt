/*
 * Copyright (C) 2026 Mark <github@xxdmkbmark> & Pidan <github@bretren>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package cc.lanternmc.materiallauncher.core

import cc.lanternmc.materiallauncher.api.Account
import cc.lanternmc.materiallauncher.api.LaunchRequest
import cc.lanternmc.materiallauncher.core.launch.LauncherException
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class LauncherBackendTest {

    private val backend = LauncherBackend()

    // ---- 离线账户：放行占位 token ----

    @Test
    fun `offline request passes through token and uuid`() = runBlocking {
        val (token, uuid) = backend.ensureValidLaunchAccount(
            LaunchRequest(
                javaPath = "j",
                gameDir = "g",
                versionId = "1.20",
                username = "Bret",
                maxMemory = "2048M",
                isolateVersion = true,
                accessToken = "0",
                uuid = "00000000-0000-0000-0000-000000000000",
                userType = "legacy",
            ),
        )
        assertEquals("0", token)
        assertEquals("00000000-0000-0000-0000-000000000000", uuid)
    }

    // ---- 正版账户严格校验 ----

    @Test
    fun `online request with no matching account throws`() {
        runBlocking {
            assertFailsWith<LauncherException> {
                backend.ensureValidLaunchAccount(
                    LaunchRequest(
                        javaPath = "j", gameDir = "g", versionId = "1.20", username = "X",
                        maxMemory = "2048M", isolateVersion = true,
                        accessToken = "tok", uuid = "no-such-uuid", userType = "msa",
                    ),
                )
            }
        }
    }

    @Test
    fun `online request matching account returns cached token when not expiring`() = runBlocking {
        // 注入一个未到期（未来 msExpiresAt）的正版账户
        backend.authStore.add(
            Account(
                id = "online-1",
                type = "online",
                username = "Bret",
                uuid = "profile-1",
                accessToken = "valid-token",
                userType = "msa",
                refreshToken = "refresh-token",
                msExpiresAt = System.currentTimeMillis() + 30 * 60 * 60 * 1000L, // 30h 后过期
            ),
        )
        val (token, uuid) = backend.ensureValidLaunchAccount(
            LaunchRequest(
                javaPath = "j", gameDir = "g", versionId = "1.20", username = "Bret",
                maxMemory = "2048M", isolateVersion = true,
                accountId = "online-1",
                accessToken = "valid-token", uuid = "profile-1", userType = "msa",
            ),
        )
        assertEquals("valid-token", token)
        assertEquals("profile-1", uuid)
    }

    @Test
    fun `online request with placeholder token throws`() = runBlocking {
        backend.authStore.add(
            Account(
                id = "online-2",
                type = "online",
                username = "Bret2",
                uuid = "profile-2",
                accessToken = "valid-token",
                userType = "msa",
                refreshToken = "refresh-2",
                msExpiresAt = System.currentTimeMillis() + 60 * 60 * 1000L,
            ),
        )
        assertFailsWith<LauncherException> {
            backend.ensureValidLaunchAccount(
                LaunchRequest(
                    javaPath = "j", gameDir = "g", versionId = "1.20", username = "Bret2",
                    maxMemory = "2048M", isolateVersion = true,
                    accountId = "online-2",
                    accessToken = "0", uuid = "profile-2", userType = "msa",
                ),
            )
        }
        Unit
    }

    // ---- 下载去重与取消 ----

    @Test
    fun `startDownload deduplicates same task key`() = runBlocking {
        backend.activeDownloads.clear()
        var started = 0
        // 第一次启动：任务持续挂起（delay 为协作式中断点），不让协程立刻结束
        backend.startDownload("minecraft:dedup") {
            started++
            delay(500)
        }
        // 第二次（同 key）应被去重忽略
        backend.startDownload("minecraft:dedup") {
            started++
            delay(500)
        }
        assertEquals(1, started)
        assertTrue(backend.activeDownloads.containsKey("minecraft:dedup"))
        // 等待任务自然结束并自清理
        delay(800)
        assertTrue(!backend.activeDownloads.containsKey("minecraft:dedup"))
    }

    @Test
    fun `cancelDownload removes and cancels active task`() = runBlocking {
        backend.activeDownloads.clear()
        var finished = false
        backend.startDownload("java:cancel-me") {
            delay(2000) // delay 是协作式中断点，可被取消
            finished = true
        }
        assertTrue(backend.activeDownloads.containsKey("java:cancel-me"))
        backend.cancelDownload("java:cancel-me")
        delay(200)
        // 任务被取消（delay 抛 CancellationException）并从表中移除
        assertTrue(!backend.activeDownloads.containsKey("java:cancel-me"))
        assertTrue(!finished)
    }

    // ---- 卸载已安装版本 ----

    @Test
    fun `deleteMinecraftVersion removes version dir`() = runBlocking {
        val dir = createTempDir()
        try {
            val versionDir = File(dir, "versions/1.20.1")
            versionDir.mkdirs()
            File(versionDir, "1.20.1.json").writeText("{}")
            File(versionDir, "1.20.1.jar").writeText("jar")

            assertTrue(backend.deleteMinecraftVersion(dir.absolutePath, "1.20.1"))
            assertFalse(versionDir.exists())

            // 不存在的版本返回 false
            assertFalse(backend.deleteMinecraftVersion(dir.absolutePath, "1.19"))
        } finally {
            dir.deleteRecursively()
        }
    }
}
