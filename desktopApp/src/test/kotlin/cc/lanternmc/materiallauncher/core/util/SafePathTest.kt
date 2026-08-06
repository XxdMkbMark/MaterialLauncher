/*
 * Copyright (C) 2026 Mark <github@xxdmkbmark> & Pidan <github@bretren>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package cc.lanternmc.materiallauncher.core.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SafePathTest {

    @Test
    fun `safe relative paths pass`() {
        assertTrue(SafePath.isSafeRelativePath("org/example/lib.jar"))
        assertTrue(SafePath.isSafeRelativePath("a/b/c.txt"))
        assertTrue(SafePath.isSafeRelativePath("main"))
    }

    @Test
    fun `traversal paths rejected`() {
        assertFalse(SafePath.isSafeRelativePath("../escape.jar"))
        assertFalse(SafePath.isSafeRelativePath("a/../../escape.jar"))
        assertFalse(SafePath.isSafeRelativePath(".."))
        assertFalse(SafePath.isSafeRelativePath("a\\..\\..\\b"))
    }

    @Test
    fun `absolute and drive paths rejected`() {
        assertFalse(SafePath.isSafeRelativePath("/etc/passwd"))
        assertFalse(SafePath.isSafeRelativePath("C:\\Users\\pidan\\evil.jar"))
        assertFalse(SafePath.isSafeRelativePath("C:/Users/pidan/evil.jar"))
        assertFalse(SafePath.isSafeRelativePath(""))
    }

    @Test
    fun `asset hash must be 40 lowercase hex`() {
        assertTrue(SafePath.isSafeAssetHash("0123456789abcdef0123456789abcdef01234567"))
        assertFalse(SafePath.isSafeAssetHash("../../etc/passwd"))
        assertFalse(SafePath.isSafeAssetHash("SHORT"))
        assertFalse(SafePath.isSafeAssetHash("g123456789abcdef0123456789abcdef0123456"))
        assertFalse(SafePath.isSafeAssetHash(""))
    }
}
