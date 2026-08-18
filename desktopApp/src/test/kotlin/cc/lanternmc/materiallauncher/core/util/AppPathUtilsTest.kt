/*
 * Copyright (C) 2026 Mark <github@xxdmkbmark> & Pidan <github@bretren>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package cc.lanternmc.materiallauncher.core.util

import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * AppPathUtils.getAppDir() 行为测试：
 * - 三策略回退都应保证返回一个存在/有效的目录
 * - 不为 null（必须有兜底）
 */
class AppPathUtilsTest {

    @Test
    fun `getAppDir returns a non-null directory`() {
        val dir = AppPathUtils.getAppDir()
        assertNotNull(dir, "getAppDir() 在任何场景下都不应返回 null（策略 3 是兜底）")
        assertTrue(dir.isAbsolute, "返回的路径必须是绝对路径: $dir")
    }

    @Test
    fun `getAppDir exists on disk (or reasonable cwd equivalent)`() {
        val dir = AppPathUtils.getAppDir()
        assertTrue(dir.exists() || File(dir.absolutePath).exists(), "目录应该存在: $dir")
    }
}
