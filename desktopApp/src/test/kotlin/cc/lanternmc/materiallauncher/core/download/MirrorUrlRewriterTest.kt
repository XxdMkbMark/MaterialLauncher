/*
 * Copyright (C) 2026 Mark <github@xxdmkbmark> & Pidan <github@bretren>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package cc.lanternmc.materiallauncher.core.download

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MirrorUrlRewriterTest {

    @Test
    fun `rewrites known official domains to bmclapi`() {
        assertEquals(
            "https://bmclapi2.bangbang93.com/assets/aa/abcdef",
            MirrorUrlRewriter.toMirrorUrl("https://resources.download.minecraft.net/aa/abcdef"),
        )
        assertEquals(
            "https://bmclapi2.bangbang93.com/mc/game/version_manifest.json",
            MirrorUrlRewriter.toMirrorUrl("https://launchermeta.mojang.com/mc/game/version_manifest.json"),
        )
        assertEquals(
            "https://bmclapi2.bangbang93.com/maven/org/example/lib.jar",
            MirrorUrlRewriter.toMirrorUrl("https://libraries.minecraft.net/org/example/lib.jar"),
        )
        assertEquals(
            "https://bmclapi2.bangbang93.com/v1/objects/xxx/client.jar",
            MirrorUrlRewriter.toMirrorUrl("https://piston-data.mojang.com/v1/objects/xxx/client.jar"),
        )
    }

    @Test
    fun `unknown url returns null`() {
        assertNull(MirrorUrlRewriter.toMirrorUrl("https://example.com/whatever"))
        assertNull(MirrorUrlRewriter.toMirrorUrl("https://api.adoptium.net/v3/assets"))
    }

    @Test
    fun `candidates ordering follows source policy`() {
        val url = "https://launchermeta.mojang.com/mc/game/version_manifest.json"
        val mirror = "https://bmclapi2.bangbang93.com/mc/game/version_manifest.json"

        // OFFICIAL: 只有官方
        assertEquals(listOf(url), MirrorUrlRewriter.candidates(url, DownloadMirrorSource.OFFICIAL))
        // MIRROR: 只有镜像
        assertEquals(listOf(mirror), MirrorUrlRewriter.candidates(url, DownloadMirrorSource.MIRROR))
        // AUTO: 镜像优先，官方兜底
        assertEquals(listOf(mirror, url), MirrorUrlRewriter.candidates(url, DownloadMirrorSource.AUTO))
    }

    @Test
    fun `fromConfig parses values with auto default`() {
        assertEquals(DownloadMirrorSource.OFFICIAL, DownloadMirrorSource.fromConfig("official"))
        assertEquals(DownloadMirrorSource.MIRROR, DownloadMirrorSource.fromConfig("mirror"))
        assertEquals(DownloadMirrorSource.AUTO, DownloadMirrorSource.fromConfig("auto"))
        assertEquals(DownloadMirrorSource.AUTO, DownloadMirrorSource.fromConfig(null))
        assertEquals(DownloadMirrorSource.AUTO, DownloadMirrorSource.fromConfig("garbage"))
    }

    @Test
    fun `unmirrorable url in mirror mode falls back to itself`() {
        val url = "https://example.com/x.jar"
        assertEquals(listOf(url), MirrorUrlRewriter.candidates(url, DownloadMirrorSource.MIRROR))
        assertTrue(MirrorUrlRewriter.toMirrorUrl(url) == null)
    }
}
