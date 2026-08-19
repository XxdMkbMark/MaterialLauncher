/*
 * Copyright (C) 2026 Mark <github@xxdmkbmark> & Pidan <github@bretren>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package cc.lanternmc.materiallauncher.core.config

import cc.lanternmc.materiallauncher.core.createTempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConfigIOTest {

    @Test
    fun `writeAtomically creates file when target does not exist`() {
        val dir = createTempDir()
        try {
            val target = File(dir, "out.toml")
            ConfigIO.writeAtomically(target, "hello")
            assertEquals("hello", target.readText())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `writeAtomically replaces existing file without prior delete`() {
        val dir = createTempDir()
        try {
            val target = File(dir, "out.toml")
            target.writeText("old")
            ConfigIO.writeAtomically(target, "new")
            assertEquals("new", target.readText())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `writeAtomically leaves no stray tmp file on success`() {
        val dir = createTempDir()
        try {
            val target = File(dir, "out.toml")
            ConfigIO.writeAtomically(target, "x")
            assertFalse(File(dir, "out.toml.tmp").exists())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `writeAtomically creates parent directories as needed`() {
        val dir = createTempDir()
        try {
            val target = File(dir, "nested/deeper/out.toml")
            ConfigIO.writeAtomically(target, "x")
            assertTrue(target.isFile)
            assertEquals("x", target.readText())
        } finally {
            dir.deleteRecursively()
        }
    }
}