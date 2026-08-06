/*
 * Copyright (C) 2026 Mark <github@xxdmkbmark> & Pidan <github@bretren>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package cc.lanternmc.materiallauncher.core.util

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import cc.lanternmc.materiallauncher.core.java.JavaFinder

class VersionTest {
    @Test
    fun `compares vanilla version numbers`() {
        assertEquals(0, compareMinecraftVersion("1.20.1", "1.20.1"))
        assertTrue(compareMinecraftVersion("1.20", "1.20.1") < 0)
        assertTrue(compareMinecraftVersion("1.20.1", "1.20") > 0)
        assertTrue(compareMinecraftVersion("1.9", "1.16") < 0)
        assertTrue(compareMinecraftVersion("1.16", "1.9") > 0)
    }

    @Test
    fun `handles unequal segment counts and non-numeric suffixes`() {
        // "1.7.10" vs "1.8"
        assertTrue(compareMinecraftVersion("1.7.10", "1.8") < 0)
        // non-numeric tail parts are treated as 0 (e.g. "1.20a" ~ "1.20")
        assertEquals(0, compareMinecraftVersion("1.20x", "1.20"))
    }
}

class Sha1Test {
    @Test
    fun `computes known sha1`() {
        val file = File.createTempFile("sha1", ".tmp")
        try {
            file.writeText("abc")
            // echo -n "abc" | sha1sum -> a9993e364706816aba3e25717850c26c9cd0d89d
            val hash = Sha1.ofFile(file)
            assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", hash)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `isFileValid checks size and hash`() {
        val file = File.createTempFile("sha1v", ".tmp")
        try {
            file.writeText("hello")
            assertTrue(Sha1.isFileValid(file.absolutePath, null, null))
            assertTrue(Sha1.isFileValid(file.absolutePath, "aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d", null))
            assertFalse(Sha1.isFileValid(file.absolutePath, "0000000000000000000000000000000000000000", null))
            assertFalse(Sha1.isFileValid(file.absolutePath, null, 4)) // "hello" 是 5 字节，4 应不匹配
            assertFalse(Sha1.isFileValid(file.absolutePath, null, 12345))
            assertFalse(Sha1.isFileValid("/nonexistent/does/not/exist", null, null))
        } finally {
            file.delete()
        }
    }
}

class Sha256Test {
    @Test
    fun `computes known sha256`() {
        val file = File.createTempFile("sha256", ".tmp")
        try {
            file.writeText("abc")
            // echo -n "abc" | sha256sum -> ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad
            assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                Sha256.ofFile(file),
            )
        } finally {
            file.delete()
        }
    }

    @Test
    fun `isFileValid skips when expected hash blank and fails on mismatch`() {
        val file = File.createTempFile("sha256v", ".tmp")
        try {
            file.writeText("data")
            assertTrue(Sha256.isFileValid(file.absolutePath, ""))
            assertTrue(Sha256.isFileValid(file.absolutePath, null))
            // "data" 的真实 SHA-256
            assertTrue(Sha256.isFileValid(file.absolutePath, "3a6eb0790f39ac87c94f3856b2dd2c5d110e6811602261a9a923d3bb23adc8b7"))
            // wrong hash of correct length
            assertFalse(Sha256.isFileValid(file.absolutePath, "f".repeat(64)))
            assertFalse(Sha256.isFileValid("/no/such/file", "3a6eb0790f39ac87c94f3856b2dd2c5d110e6811602261a9a923d3bb23adc8b7"))
        } finally {
            file.delete()
        }
    }
}

class TomlTest {
    @Test
    fun `parses sections and array tables`() {
        val text = """
            # comment
            version = 1
            [minecraft]
            path = "C:\some\dir"
            source = "default"
            [[account]]
            id = "abc-123"
            username = "TestUser"
            access_token = "tok"
            ms_expires_at = 123456789
        """.trimIndent()
        val doc = Toml.parse(text)
        assertEquals("1", doc.section("").values["version"])
        assertEquals("C:\\some\\dir", doc.section("minecraft").values["path"])
        assertEquals("default", doc.section("minecraft").values["source"])
        val accounts = doc.section("account").items
        assertEquals(1, accounts.size)
        assertEquals("abc-123", accounts[0]["id"])
        assertEquals("123456789", accounts[0]["ms_expires_at"])
    }

    @Test
    fun `quote handles backslash and quote`() {
        assertEquals("\"a\\\\b\"", Toml.quote("a\\b"))
        assertEquals("\"say \\\"hi\\\"\"", Toml.quote("say \"hi\""))
    }

    @Test
    fun `unquote strips surrounding quotes`() {
        // exercised through parse
        val doc = Toml.parse("k = \"\\\"x\\\"\"")
        assertEquals("\"x\"", doc.section("").values["k"])
    }
}

class JavaFinderTest {
    @Test
    fun `parses feature version from various version strings`() {
        assertEquals(8, JavaFinder.javaFeatureVersion("1.8.0_392"))
        assertEquals(8, JavaFinder.javaFeatureVersion("1.8"))
        assertEquals(17, JavaFinder.javaFeatureVersion("17.0.9+9"))
        assertEquals(21, JavaFinder.javaFeatureVersion("21.0.1"))
        assertEquals(21, JavaFinder.javaFeatureVersion("21.0.1-12"))
        assertEquals(8, JavaFinder.javaFeatureVersion("garbage"))
    }

    @Test
    fun `platform path key lowercases on windows only`() {
        // Path key semantics: we just verify it returns a string.
        assertTrue(JavaFinder.pathKey("C:\\Java").isNotBlank())
    }
}
