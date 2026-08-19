/*
 * Copyright (C) 2026 Mark <github@xxdmkbmark> & Pidan <github@bretren>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package cc.lanternmc.materiallauncher.core.config

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GlobalConfigStoreTest {

    private fun tempFile(): String = File.createTempFile("global", ".toml").let {
        it.delete()
        it.absolutePath
    }

    @Test
    fun `load on missing file returns defaults`() {
        val store = GlobalConfigStore(tempFile())
        val s = store.load()
        assertEquals("2048M", s.defaultMaxMemory)
        assertEquals("auto", s.downloadSource)
        assertEquals("TestUser", s.defaultUsername)
    }

    @Test
    fun `save then load round-trips all fields`() {
        val path = tempFile()
        val store = GlobalConfigStore(path)
        val s = cc.lanternmc.materiallauncher.api.GlobalLaunchSettings(
            minecraftPath = "D:\\games\\mc",
            javaPath = "C:\\java\\17",
            defaultAccountId = "acc-1",
            defaultUsername = "Bret",
            defaultMaxMemory = "4096M",
            defaultJvmArgs = "-XX:+UseG1GC",
            defaultGameArgs = "--fullscreen",
            downloadSource = "mirror",
        )
        assertTrue(store.save(s))
        val loaded = store.load()
        assertEquals("D:\\games\\mc", loaded.minecraftPath)
        assertEquals("C:\\java\\17", loaded.javaPath)
        assertEquals("acc-1", loaded.defaultAccountId)
        assertEquals("Bret", loaded.defaultUsername)
        assertEquals("4096M", loaded.defaultMaxMemory)
        assertEquals("-XX:+UseG1GC", loaded.defaultJvmArgs)
        assertEquals("--fullscreen", loaded.defaultGameArgs)
        assertEquals("mirror", loaded.downloadSource)
    }
}