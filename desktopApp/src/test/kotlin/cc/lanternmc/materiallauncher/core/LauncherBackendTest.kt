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

    /**
     * 关键安全措施：backend 必须运行在独立临时数据目录上，
     * config/auth/instances 全部隔离，绝不触碰真实 .minecraft 或用户数据。
     */
    private val testDataDir: File = createTempDir()
    private val testMcDir: File = createTempDir()
    private val backend = LauncherBackend(dataDirectory = testDataDir.absolutePath)

    init {
        // 双重保险：把 backend 的 minecraft 路径也重定向到临时目录
        val cfg = backend.configStore.load()
        backend.configStore.save(
            cfg.copy(
                minecraft = cc.lanternmc.materiallauncher.api.DownloadPathConfig(
                    path = testMcDir.absolutePath,
                    source = "custom",
                ),
            ),
        )
    }

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

    // ---- 多实例：异常路径 ----

    @Test
    fun `launchInstance with unknown id throws`() {
        runBlocking {
            assertFailsWith<LauncherException> {
                backend.launchInstance(
                    instanceId = "no-such-instance",
                    username = "Bret",
                    accessToken = "0",
                    uuid = "00000000-0000-0000-0000-000000000000",
                    userType = "legacy",
                )
            }
        }
    }

    // ---- 离线账户：配置同步 ----

    @Test
    fun `addOfflineAccount syncs config username and accountId`() = runBlocking {
        val account = backend.addOfflineAccount("BretNew")
        val cfg = backend.configStore.load()
        assertEquals(account.id, cfg.accountId)
        assertEquals("BretNew", cfg.username)
    }

    // ---- 实例 = 命名版本文件夹（统一模型） ----

    @Test
    fun `createInstance creates named folder under versions and rejects duplicates`() {
        runBlocking {
            val cfg = backend.configStore.load()
            val mcPath = cfg.minecraft.path
            val versionDir = File(mcPath, "versions")
            versionDir.mkdirs()
            try {
                val inst = backend.createInstance("我的生存服", "1.20.1")
                assertEquals("我的生存服", inst.name)
                assertEquals(mcPath, inst.gameDir)
                assertTrue(File(versionDir, "我的生存服").isDirectory)

                // 重名应被拒绝
                assertFailsWith<LauncherException> {
                    backend.createInstance("我的生存服", "1.21")
                }
            } finally {
                // 清理：删实例 + 目录（不删 mcPath 本身）
                backend.listInstances().forEach { backend.deleteInstance(it.id) }
                File(versionDir, "我的生存服").deleteRecursively()
            }
        }
    }

    @Test
    fun `deleteInstance removes versions folder but keeps gameDir`() = runBlocking {
        val cfg = backend.configStore.load()
        val mcPath = cfg.minecraft.path
        val versionDir = File(mcPath, "versions")
        versionDir.mkdirs()
        try {
            val inst = backend.createInstance("待删实例", "1.20.1")
            val instanceDir = File(versionDir, "待删实例")
            assertTrue(instanceDir.isDirectory)
            File(instanceDir, "saves").mkdirs()

            assertTrue(backend.deleteInstance(inst.id))
            // 实例目录被删除，但 mcPath 根目录仍在
            assertFalse(instanceDir.exists())
            assertTrue(File(mcPath).isDirectory)
            // 实例记录已移除
            assertTrue(backend.listInstances().none { it.id == inst.id })
        } finally {
            backend.listInstances().forEach { backend.deleteInstance(it.id) }
        }
    }

    // ---- 旧版本文件夹自动并入实例列表 ----

    @Test
    fun `legacy version folder without registration appears in instances and is deletable`() = runBlocking {
        val cfg = backend.configStore.load()
        val mcPath = cfg.minecraft.path
        val versionDir = File(mcPath, "versions")
        versionDir.mkdirs()
        val legacyDir = File(versionDir, "1.16.5")
        try {
            // 模拟旧版启动器下载的版本：只有文件夹 + json，没有实例注册
            legacyDir.mkdirs()
            File(legacyDir, "1.16.5.json").writeText("{}")
            File(legacyDir, "1.16.5.jar").writeText("jar")

            val instances = backend.listInstances()
            val legacy = instances.firstOrNull { it.name == "1.16.5" }
            assertTrue(legacy != null, "versions 下的旧版本文件夹应出现在实例列表")
            assertEquals("legacy-1.16.5", legacy!!.id)

            // legacy 实例可删除（只删自己的文件夹）
            assertTrue(backend.deleteInstance(legacy.id))
            assertFalse(legacyDir.exists())
            assertTrue(File(mcPath).isDirectory)
        } finally {
            runCatching { legacyDir.deleteRecursively() }
            backend.listInstances().forEach { backend.deleteInstance(it.id) }
        }
    }

    // ---- 路径安全：非法名字必须被拒绝，绝不触碰 versions 根目录 ----

    @Test
    fun `deleteMinecraftVersion rejects empty and traversal names`() = runBlocking {
        val cfg = backend.configStore.load()
        val mcPath = cfg.minecraft.path
        val versionDir = File(mcPath, "versions")
        versionDir.mkdirs()
        val sentinel = File(versionDir, "sentinel.txt")
        sentinel.writeText("keep")

        try {
            // 空名、.. 、带斜杠的名字一律拒绝，versions 目录本身必须安然无恙
            assertFalse(backend.deleteMinecraftVersion(mcPath, ""))
            assertFalse(backend.deleteMinecraftVersion(mcPath, ".."))
            assertFalse(backend.deleteMinecraftVersion(mcPath, "a/b"))
            assertFalse(backend.deleteMinecraftVersion(mcPath, "../escape"))
            assertTrue(versionDir.isDirectory)
            assertTrue(sentinel.exists(), "versions 目录内容不得被误删")
        } finally {
            sentinel.delete()
        }
    }

    @Test
    fun `sanitizeFolderName neutralizes dot and device names`() = runBlocking {
        // 通过 createInstance 验证：. / .. / 设备名会被清洗为安全名字
        val cfg = backend.configStore.load()
        val mcPath = cfg.minecraft.path
        val versionDir = File(mcPath, "versions")
        versionDir.mkdirs()
        try {
            val inst = backend.createInstance("..", "1.20.1")
            // 清洗后不会是 ".."，而应是 "instance" 或别的安全名
            assertFalse(inst.name == "..")
            assertFalse(inst.name == ".")
            assertFalse(inst.name.contains('/'))
            assertFalse(inst.name.contains('\\'))
            // 目录创建在 versions 下，且不在 mcPath 根
            assertTrue(File(versionDir, inst.name).isDirectory)
        } finally {
            backend.listInstances().forEach { backend.deleteInstance(it.id) }
        }
    }

    // ---- 数据目录配置 ----

    @Test
    fun `data directory get and set round-trip`() {
        // 默认数据目录 = 隔离的临时目录（dataDirectory 参数）
        assertEquals(testDataDir.absolutePath, backend.getDataDirectory())
        // 设置空目录应失败
        assertFalse(backend.setDataDirectory(""))
        assertFalse(backend.setDataDirectory("   "))
        // 设置合法目录应成功（写入标志文件）
        val newDir = createTempDir()
        try {
            assertTrue(backend.setDataDirectory(newDir.absolutePath))
        } finally {
            newDir.deleteRecursively()
            // 清理标志文件，避免影响后续测试
            cc.lanternmc.materiallauncher.core.config.AppDataPathsResolver
                .writeDataDirOverride(testDataDir.absolutePath)
        }
    }

    // ---- 异步启动 API 存在且不阻塞 ----

    @Test
    fun `launchMinecraftAsync returns immediately without crashing`() = runBlocking {
        // 异步启动不应挂起调用方协程；这里只验证 API 可调用（空 gameDir 会走校验后失败，但不抛到调用方）
        backend.launchMinecraftAsync(
            LaunchRequest(
                javaPath = "",
                gameDir = "",
                versionId = "",
                username = "TestUser",
                maxMemory = "2048M",
                isolateVersion = true,
            ),
        )
        delay(200)
        assertTrue(true) // 调用本身不抛异常即可
    }
}
