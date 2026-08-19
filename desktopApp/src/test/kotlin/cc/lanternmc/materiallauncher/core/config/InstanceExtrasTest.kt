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

    @Test
    fun `extras rows with empty instance_id are ignored`() {
        val path = tempFile()
        File(path).writeText(
            """
            version = 1
            [[instance]]
            id = "i-1"
            name = "A"
            version_id = "1.20.1"
            [[instance_extra]]
            instance_id = ""
            key = "ghostKey"
            value = "ghost"
            [[instance_extra]]
            instance_id = "i-1"
            key = "realKey"
            value = "real"
            """.trimIndent(),
        )
        val loaded = InstanceStore(path).load()
        assertEquals(1, loaded.size)
        // ghostKey 不会落到空 id 桶，也不会污染 i-1
        assertEquals(1, loaded[0].extras.size)
        assertEquals("real", loaded[0].extras["realKey"])
    }

    @Test
    fun `extras rows with empty key are ignored`() {
        val path = tempFile()
        File(path).writeText(
            """
            version = 1
            [[instance]]
            id = "i-1"
            name = "A"
            version_id = "1.20.1"
            [[instance_extra]]
            instance_id = "i-1"
            key = ""
            value = "should-not-appear"
            [[instance_extra]]
            instance_id = "i-1"
            key = "kept"
            value = "yes"
            """.trimIndent(),
        )
        val loaded = InstanceStore(path).load()
        assertEquals(1, loaded.size)
        assertEquals(1, loaded[0].extras.size)
        assertEquals("yes", loaded[0].extras["kept"])
    }

    @Test
    fun `extras rows with empty value are kept (empty value is valid)`() {
        val path = tempFile()
        File(path).writeText(
            """
            version = 1
            [[instance]]
            id = "i-1"
            name = "A"
            version_id = "1.20.1"
            [[instance_extra]]
            instance_id = "i-1"
            key = "feature"
            value = ""
            """.trimIndent(),
        )
        val loaded = InstanceStore(path).load()
        assertEquals(1, loaded[0].extras.size)
        assertEquals("", loaded[0].extras["feature"])
    }
}