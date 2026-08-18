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

class InstanceExtrasTest {

    private fun tempFile(): String = File.createTempFile("instances", ".toml").let {
        it.delete()
        it.absolutePath
    }

    @Test
    fun `extras round-trip through save and load`() {
        val path = tempFile()
        val store = InstanceStore(path)
        val inst = GameInstance(
            id = "i-1",
            name = "生存服",
            versionId = "1.20.1",
            gameDir = "D:\\mc",
            extras = linkedMapOf(
                "forgeVersion" to "47.2.0",
                "fabricLoader" to "0.15.6",
            ),
        )
        store.add(inst)
        val loaded = store.load()
        assertEquals(1, loaded.size)
        val it = loaded[0]
        assertEquals("i-1", it.id)
        assertEquals(2, it.extras.size)
        assertEquals("47.2.0", it.extras["forgeVersion"])
        assertEquals("0.15.6", it.extras["fabricLoader"])
    }

    @Test
    fun `legacy instance without extras loads with empty map`() {
        val path = tempFile()
        File(path).writeText(
            """
            version = 1
            [[instance]]
            id = "i-old"
            name = "old"
            version_id = "1.16.5"
            max_memory = "2048M"
            """.trimIndent(),
        )
        val store = InstanceStore(path)
        val loaded = store.load()
        assertEquals(1, loaded.size)
        assertTrue(loaded[0].extras.isEmpty())
    }

    @Test
    fun `extras from multiple instances stay isolated`() {
        val path = tempFile()
        val store = InstanceStore(path)
        store.add(GameInstance(id = "a", name = "A", extras = mapOf("k" to "1")))
        store.add(GameInstance(id = "b", name = "B", extras = mapOf("k" to "2", "extra" to "z")))
        val loaded = store.load().associateBy { it.id }
        assertEquals("1", loaded["a"]?.extras?.get("k"))
        assertEquals(1, loaded["a"]?.extras?.size)
        assertEquals("2", loaded["b"]?.extras?.get("k"))
        assertEquals("z", loaded["b"]?.extras?.get("extra"))
    }

    @Test
    fun `updating extras preserves other instance fields`() {
        val path = tempFile()
        val store = InstanceStore(path)
        val original = GameInstance(
            id = "i-1",
            name = "name",
            versionId = "1.20.1",
            maxMemory = "4096M",
            jvmArgs = "-Dfoo=bar",
        )
        store.add(original)
        val reloaded = store.load().single()
        val updated = reloaded.copy(extras = reloaded.extras + ("k" to "v"))
        store.add(updated)

        val final = store.load().single()
        assertEquals("name", final.name)
        assertEquals("1.20.1", final.versionId)
        assertEquals("4096M", final.maxMemory)
        assertEquals("-Dfoo=bar", final.jvmArgs)
        assertEquals("v", final.extras["k"])
    }
}