/*
 * Copyright (C) 2026 Mark <github@xxdmkbmark> & Pidan <github@bretren>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package cc.lanternmc.materiallauncher.core.download

import cc.lanternmc.materiallauncher.core.launch.OptionsSanitizer
import cc.lanternmc.materiallauncher.core.model.ClientDownloads
import cc.lanternmc.materiallauncher.core.model.VersionJson
import cc.lanternmc.materiallauncher.core.model.XboxAuthResponse
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class JavaVersionServiceTest {
    @Test
    fun `extractSha256 strips prefix and validates length`() {
        val hex = "3f2" + "a".repeat(61)
        assertEquals(hex, JavaVersionService.extractSha256("sha256:$hex  "))
        assertEquals(hex, JavaVersionService.extractSha256("sha256:$hex"))
        assertEquals(hex, JavaVersionService.extractSha256(hex))
    }

    @Test
    fun `extractSha256 rejects invalid`() {
        assertEquals("", JavaVersionService.extractSha256(""))
        assertEquals("", JavaVersionService.extractSha256("sha256:tooshort"))
        assertEquals("", JavaVersionService.extractSha256("garbage"))
        // 非 hex 字符
        assertEquals("", JavaVersionService.extractSha256("z".repeat(64)))
    }
}

class OptionsSanitizerTest {
    @Test
    fun `creates defaults when options missing`() {
        val dir = createTempDir()
        try {
            OptionsSanitizer.sanitize(dir)
            val text = File(dir, "options.txt").readText()
            assertEquals("fullscreen:false", text.lineSequence().first { it.startsWith("fullscreen:") })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `forces fullscreen off and normalizes fov and guiScale`() {
        val dir = createTempDir()
        try {
            File(dir, "options.txt").writeText(
                "fullscreen:true\nexclusiveFullscreen:true\noverrideWidth:1920\nfov:0.0\nguiScale:0\nsoundCategory.master:1.0\n"
            )
            OptionsSanitizer.sanitize(dir)
            val modified = File(dir, "options.txt").readText()
            assertEquals("fullscreen:false", modified.lineSequence().first { it.startsWith("fullscreen:") })
            assertEquals("exclusiveFullscreen:false", modified.lineSequence().first { it.startsWith("exclusiveFullscreen:") })
            assertEquals("overrideWidth:0", modified.lineSequence().first { it.startsWith("overrideWidth:") })
            assertEquals("fov:0.5", modified.lineSequence().first { it.startsWith("fov:") })
            assertEquals("guiScale:3", modified.lineSequence().first { it.startsWith("guiScale:") })
            // 无关设置保留
            assertTrue(modified.contains("soundCategory.master:1.0"))
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun createTempDir(): File {
        val dir = File.createTempFile("opts", "")
        dir.delete()
        dir.mkdirs()
        return dir
    }
}

class MinecraftModelSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes version json with client downloads`() {
        val raw = """
            {
              "mainClass": "net.minecraft.client.main.Main",
              "assetIndex": {"id": "15", "url": "https://e/a.json"},
              "downloads": {
                "client": {"sha1": "abc123", "size": 100, "url": "https://e/client.jar"}
              },
              "javaVersion": {"majorVersion": 21, "component": "java-runtime-delta"},
              "libraries": []
            }
        """.trimIndent()
        val v = json.decodeFromString<VersionJson>(raw)
        assertEquals("net.minecraft.client.main.Main", v.mainClass)
        assertEquals("15", v.assetIndex.id)
        val client: ClientDownloads? = v.downloads
        assertEquals(100L, client?.client?.size)
        assertEquals(21, v.javaVersion?.majorVersion)
    }

    @Test
    fun `decodes xbox response with display claims`() {
        val raw = """
            {"Token":"t0ken","DisplayClaims":{"xui":[{"uhs":"userhash123"}]}}
        """.trimIndent()
        val resp = json.decodeFromString<XboxAuthResponse>(raw)
        assertEquals("t0ken", resp.token)
        assertEquals("userhash123", resp.displayClaims.xui.first().uhs)
    }
}
