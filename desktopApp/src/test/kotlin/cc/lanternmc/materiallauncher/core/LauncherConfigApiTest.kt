/*
 * Copyright (C) 2026 Mark <github@xxdmkbmark> & Pidan <github@bretren>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package cc.lanternmc.materiallauncher.core

import cc.lanternmc.materiallauncher.api.GameInstance
import cc.lanternmc.materiallauncher.api.GlobalLaunchSettings
import cc.lanternmc.materiallauncher.api.LauncherSettings
import cc.lanternmc.materiallauncher.api.UiSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LauncherConfigApiTest {

    private val testDataDir = createTempDir()
    private val backend = LauncherBackend(dataDirectory = testDataDir.absolutePath)

    @Test
    fun `getLauncherSettings returns defaults when no file`() = runBlocking {
        val s = backend.getLauncherSettings()
        assertEquals("system", s.ui.theme)
        assertEquals("zh-CN", s.ui.language)
    }

    @Test
    fun `saveLauncherSettings persists across backend instances`() = runBlocking {
        backend.saveLauncherSettings(
            LauncherSettings(ui = UiSettings(theme = "dark", language = "en-US")),
        )
        // 新 backend 实例必须读到相同值（持久化生效）
        val other = LauncherBackend(dataDirectory = testDataDir.absolutePath)
        val s = other.getLauncherSettings()
        assertEquals("dark", s.ui.theme)
        assertEquals("en-US", s.ui.language)
    }

    @Test
    fun `getGlobalSettings returns defaults when no file`() = runBlocking {
        val s = backend.getGlobalSettings()
        assertEquals("2048M", s.defaultMaxMemory)
        assertEquals("auto", s.downloadSource)
    }

    @Test
    fun `saveGlobalSettings round-trips`() = runBlocking {
        backend.saveGlobalSettings(
            GlobalLaunchSettings(
                minecraftPath = "D:\\games\\mc",
                javaPath = "C:\\java\\17",
                defaultMaxMemory = "4096M",
                downloadSource = "mirror",
            ),
        )
        val s = backend.getGlobalSettings()
        assertEquals("D:\\games\\mc", s.minecraftPath)
        assertEquals("C:\\java\\17", s.javaPath)
        assertEquals("4096M", s.defaultMaxMemory)
        assertEquals("mirror", s.downloadSource)
    }

    @Test
    fun `instance extras read write and remove`() = runBlocking {
        val inst = GameInstance(id = "i-1", name = "X", versionId = "1.20.1")
        backend.instanceStore.add(inst)

        assertNull(backend.getInstanceExtra("i-1", "forgeVersion"))
        backend.setInstanceExtra("i-1", "forgeVersion", "47.2.0")
        backend.setInstanceExtra("i-1", "fabricLoader", "0.15.6")
        assertEquals("47.2.0", backend.getInstanceExtra("i-1", "forgeVersion"))
        assertEquals("0.15.6", backend.getInstanceExtra("i-1", "fabricLoader"))

        backend.removeInstanceExtra("i-1", "forgeVersion")
        assertNull(backend.getInstanceExtra("i-1", "forgeVersion"))
        // 其它键应保留
        assertEquals("0.15.6", backend.getInstanceExtra("i-1", "fabricLoader"))
    }

    @Test
    fun `getInstanceExtra returns null for unknown instance`() = runBlocking {
        assertNull(backend.getInstanceExtra("does-not-exist", "any"))
    }
}