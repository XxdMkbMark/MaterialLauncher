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

    @Test
    fun `adoptium github archive maps to tsinghua mirror`() {
        val official = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9%2B9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.9_9.zip"
        val mirror = MirrorUrlRewriter.toAdoptiumMirrorUrl(official)
        assertEquals(
            "https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jdk/x64/windows/hotspot/OpenJDK17U-jdk_x64_windows_hotspot_17.0.9_9.zip",
            mirror,
        )
    }

    @Test
    fun `adoptium candidates follow source policy`() {
        val official = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.1/OpenJDK21U-jdk_aarch64_linux_hotspot_21.0.1_12.tar.gz"
        val mirror = "https://mirrors.tuna.tsinghua.edu.cn/Adoptium/21/jdk/aarch64/linux/hotspot/OpenJDK21U-jdk_aarch64_linux_hotspot_21.0.1_12.tar.gz"

        assertEquals(listOf(official), MirrorUrlRewriter.adoptiumCandidates(official, DownloadMirrorSource.OFFICIAL))
        assertEquals(listOf(mirror), MirrorUrlRewriter.adoptiumCandidates(official, DownloadMirrorSource.MIRROR))
        assertEquals(listOf(mirror, official), MirrorUrlRewriter.adoptiumCandidates(official, DownloadMirrorSource.AUTO))
    }

    @Test
    fun `adoptium mirror returns null for non-matching urls`() {
        assertTrue(MirrorUrlRewriter.toAdoptiumMirrorUrl("https://api.adoptium.net/v3/binary/xxx") == null)
        assertTrue(MirrorUrlRewriter.toAdoptiumMirrorUrl("https://github.com/other/repo/releases/download/v1/x.zip") == null)
    }
}
