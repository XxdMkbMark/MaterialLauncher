/*
 * Copyright (C) 2026 Mark <github@xxdmkbmark> & Pidan <github@bretren>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package cc.lanternmc.materiallauncher.core.config

import cc.lanternmc.materiallauncher.api.Account
import cc.lanternmc.materiallauncher.api.DownloadConfig
import cc.lanternmc.materiallauncher.api.DownloadPathConfig
import cc.lanternmc.materiallauncher.core.createTempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthStoreTest {

    private fun tempFile(): String = File.createTempFile("auth", ".toml").let {
        it.delete()
        it.absolutePath
    }

    @Test
    fun `add then load returns the account`() {
        val path = tempFile()
        val store = AuthStore(path)
        val account = Account(
            id = "acc-1",
            type = "online",
            username = "Bret",
            uuid = "u1",
            accessToken = "tok-1",
            userType = "msa",
            refreshToken = "refresh-1",
            msExpiresAt = 1234567890123L,
            lastRefreshed = "2026-01-01T00:00:00Z",
        )
        store.add(account)
        val loaded = store.load()
        assertEquals(1, loaded.size)
        assertEquals("acc-1", loaded[0].id)
        assertEquals("msa", loaded[0].userType)
        assertEquals("refresh-1", loaded[0].refreshToken)
        assertEquals(1234567890123L, loaded[0].msExpiresAt)
    }

    @Test
    fun `add replaces same id`() {
        val path = tempFile()
        val store = AuthStore(path)
        store.add(Account(id = "acc", type = "offline", username = "old"))
        store.add(Account(id = "acc", type = "offline", username = "new"))
        val loaded = store.load()
        assertEquals(1, loaded.size)
        assertEquals("new", loaded[0].username)
    }

    @Test
    fun `remove deletes by id`() {
        val path = tempFile()
        val store = AuthStore(path)
        store.add(Account(id = "a", username = "A"))
        store.add(Account(id = "b", username = "B"))
        store.remove("a")
        val loaded = store.load()
        assertEquals(1, loaded.size)
        assertEquals("b", loaded[0].id)
    }

    @Test
    fun `load on missing file returns empty`() {
        val store = AuthStore(File.createTempFile("auth", ".toml").absolutePath + ".nonexistent")
        assertTrue(store.load().isEmpty())
    }
}

class DownloadConfigStoreTest {

    @Test
    fun `save then load round-trips`() {
        val dir = createTempDir()
        try {
            val paths = AppDataPaths(
                directory = dir.absolutePath,
                config = File(dir, "config.toml").absolutePath,
                javaIndex = File(dir, "java-index.toml").absolutePath,
            )
            val store = DownloadConfigStore(paths)
            val config = DownloadConfig(
                minecraft = DownloadPathConfig(path = "D:\\games\\mc", source = "custom"),
                java = DownloadPathConfig(path = "D:\\java", source = "launcher"),
                username = "Bret",
                accountId = "acc-99",
                mirrorSource = "mirror",
                jvmArgs = "-XX:+UseG1GC -Xmn512M",
                gameArgs = "--quickPlayMultiplayer 127.0.0.1",
            )
            assertTrue(store.save(config))
            val loaded = store.load()
            assertEquals("D:\\games\\mc", loaded.minecraft.path)
            assertEquals("custom", loaded.minecraft.source)
            assertEquals("D:\\java", loaded.java.path)
            assertEquals("Bret", loaded.username)
            assertEquals("acc-99", loaded.accountId)
            assertEquals("mirror", loaded.mirrorSource)
            assertEquals("-XX:+UseG1GC -Xmn512M", loaded.jvmArgs)
            assertEquals("--quickPlayMultiplayer 127.0.0.1", loaded.gameArgs)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `load on missing config returns defaults`() {
        val dir = createTempDir()
        try {
            val paths = AppDataPaths(
                directory = dir.absolutePath,
                config = File(dir, "config.toml").absolutePath,
                javaIndex = File(dir, "java-index.toml").absolutePath,
            )
            val store = DownloadConfigStore(paths)
            val def = store.load()
            assertEquals("default", def.minecraft.source)
            assertEquals("launcher", def.java.source)
        } finally {
            dir.deleteRecursively()
        }
    }
}
