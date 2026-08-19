/*
 * Copyright (C) 2026 Mark <github@xxdmkbmark> & Pidan <github@bretren>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package cc.lanternmc.materiallauncher.core.config

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LauncherConfigStoreTest {

    private fun tempFile(): String = File.createTempFile("launcher", ".toml").let {
        it.delete()
        it.absolutePath
    }

    @Test
    fun `load on missing file returns defaults`() {
        val store = LauncherConfigStore(tempFile())
        val s = store.load()
        assertEquals("system", s.ui.theme)
        assertEquals("zh-CN", s.ui.language)
        assertEquals("auto", s.download.source)
        assertEquals(8, s.download.concurrency)
    }

    @Test
    fun `save then load round-trips`() {
        val path = tempFile()
        val store = LauncherConfigStore(path)
        val s = cc.lanternmc.materiallauncher.api.LauncherSettings(
            ui = cc.lanternmc.materiallauncher.api.UiSettings(theme = "dark", language = "en-US"),
            download = cc.lanternmc.materiallauncher.api.DownloadDefaults(source = "mirror", concurrency = 16),
        )
        assertTrue(store.save(s))
        val loaded = store.load()
        assertEquals("dark", loaded.ui.theme)
        assertEquals("en-US", loaded.ui.language)
        assertEquals("mirror", loaded.download.source)
        assertEquals(16, loaded.download.concurrency)
    }

    @Test
    fun `invalid concurrency falls back to default`() {
        val path = tempFile()
        File(path).writeText(
            """
            [ui]
            theme = "light"
            [download]
            source = "official"
            concurrency = "not-a-number"
            """.trimIndent(),
        )
        val store = LauncherConfigStore(path)
        val s = store.load()
        assertEquals("light", s.ui.theme)
        assertEquals("official", s.download.source)
        assertEquals(8, s.download.concurrency)
    }
}