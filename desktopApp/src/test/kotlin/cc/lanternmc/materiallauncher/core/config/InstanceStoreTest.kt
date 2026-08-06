/*
 * Copyright (C) 2026 Mark <github@xxdmkbmark> & Pidan <github@bretren>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package cc.lanternmc.materiallauncher.core.config

import cc.lanternmc.materiallauncher.api.GameInstance
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class InstanceStoreTest {

    private fun tempFile(): String = File.createTempFile("instances", ".toml").let {
        it.delete()
        it.absolutePath
    }

    @Test
    fun `add then load round-trips all fields`() {
        val path = tempFile()
        val store = InstanceStore(path)
        store.add(
            GameInstance(
                id = "i-1",
                name = "生存服",
                versionId = "1.20.1",
                gameDir = "D:\\instances\\survival",
                javaPath = "C:\\java\\17",
                maxMemory = "4096M",
                jvmArgs = "-Dfoo=bar",
                createdAt = "2026-01-01T00:00:00Z",
            ),
        )
        val loaded = store.load()
        assertEquals(1, loaded.size)
        val it = loaded[0]
        assertEquals("i-1", it.id)
        assertEquals("生存服", it.name)
        assertEquals("1.20.1", it.versionId)
        assertEquals("D:\\instances\\survival", it.gameDir)
        assertEquals("4096M", it.maxMemory)
        assertEquals("-Dfoo=bar", it.jvmArgs)
    }

    @Test
    fun `add replaces same id and remove deletes`() {
        val path = tempFile()
        val store = InstanceStore(path)
        store.add(GameInstance(id = "a", name = "A", versionId = "1.20"))
        store.add(GameInstance(id = "a", name = "A2", versionId = "1.21"))
        assertEquals(1, store.load().size)
        assertEquals("A2", store.load()[0].name)
        store.remove("a")
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun `load on missing file returns empty`() {
        val store = InstanceStore(File.createTempFile("instances", ".toml").absolutePath + ".nonexistent")
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun `newInstance generates unique id and isolated game dir`() {
        val a = InstanceStore.newInstance("测试 实例", "1.20.1", "D:\\launcher-data")
        val b = InstanceStore.newInstance("测试 实例", "1.20.1", "D:\\launcher-data")
        assertTrue(a.id != b.id)
        assertTrue(a.gameDir != b.gameDir)
        // 实例目录位于 baseDir/instances/ 下
        assertTrue(a.gameDir.startsWith("D:\\launcher-data\\instances\\"))
        assertEquals("1.20.1", a.versionId)
        assertFalse(a.gameDir.contains(" "))
    }
}
