/*
 * Copyright (C) 2026 Mark <github@xxdmkbmark> & Pidan <github@bretren>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package cc.lanternmc.materiallauncher.core.util

import cc.lanternmc.materiallauncher.core.createTempDir
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArchiveExtractorTest {

    private fun writeZip(path: String, entries: Map<String, String>) {
        ZipOutputStream(File(path).outputStream()).use { zip ->
            for ((name, content) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
    }

    @Test
    fun `extracts flat zip entries`() {
        val tmp = File.createTempFile("arc", ".zip")
        val dest = createTempDir()
        try {
            writeZip(tmp.absolutePath, mapOf("a.txt" to "AAA", "b.txt" to "BBB"))
            ArchiveExtractor.extractArchive(tmp.absolutePath, dest.absolutePath)
            assertEquals("AAA", File(dest, "a.txt").readText())
            assertEquals("BBB", File(dest, "b.txt").readText())
        } finally {
            tmp.delete()
            dest.deleteRecursively()
        }
    }

    @Test
    fun `strips single top-level directory`() {
        val tmp = File.createTempFile("arc", ".zip")
        val dest = createTempDir()
        try {
            writeZip(tmp.absolutePath, mapOf("jdkRoot/BUILD.txt" to "build", "jdkRoot/bin/java" to "elf"))
            ArchiveExtractor.extractArchive(tmp.absolutePath, dest.absolutePath)
            // 顶层目录被剥掉：dest 直接包含 bin/java
            assertEquals("elf", File(dest, "bin/java").readText())
            assertFalse(File(dest, "jdkRoot").exists())
        } finally {
            tmp.delete()
            dest.deleteRecursively()
        }
    }

    @Test
    fun `rejects traversal outside destination`() {
        val tmp = File.createTempFile("evil", ".zip")
        val dest = createTempDir()
        try {
            // 恶意条目名含 ../，extractNativeJar 采取 fail-fast：直接抛异常而非写出目录外
            writeZip(tmp.absolutePath, mapOf("../escaped.txt" to "should not appear"))
            assertFailsWith<IllegalArgumentException> {
                ArchiveExtractor.extractNativeJar(tmp.absolutePath, dest.absolutePath)
            }
            assertFalse(File(dest, "../escaped.txt").exists())
            assertFalse(File(dest.parentFile, "escaped.txt").exists())
        } finally {
            tmp.delete()
            dest.deleteRecursively()
        }
    }

    @Test
    fun `extractNativeJar skips meta-inf and excludes`() {
        val tmp = File.createTempFile("native", ".zip")
        val dest = createTempDir()
        try {
            writeZip(
                tmp.absolutePath,
                mapOf(
                    "META-INF/MANIFEST.MF" to "skip",
                    "jni.dll" to "dll",
                    "other/keep.txt" to "keep",
                ),
            )
            ArchiveExtractor.extractNativeJar(tmp.absolutePath, dest.absolutePath, listOf("META-INF/"))
            assertEquals("dll", File(dest, "jni.dll").readText())
            assertEquals("keep", File(dest, "other/keep.txt").readText())
            assertFalse(File(dest, "META-INF").exists())
        } finally {
            tmp.delete()
            dest.deleteRecursively()
        }
    }

    @Test
    fun `unsupported format throws`() {
        val dest = createTempDir()
        try {
            assertFailsWith<IllegalArgumentException> {
                ArchiveExtractor.extractArchive("/no/file.rar", dest.absolutePath)
            }
        } finally {
            dest.deleteRecursively()
        }
    }
}
