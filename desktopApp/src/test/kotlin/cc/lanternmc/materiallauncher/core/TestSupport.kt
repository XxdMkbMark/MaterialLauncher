/*
 * Copyright (C) 2026 Mark <github@xxdmkbmark> & Pidan <github@bretren>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package cc.lanternmc.materiallauncher.core

import java.io.File

/** 创建一个已存在的空临时目录；JVM 无内置 createTempDir，各测试类共用。 */
internal fun createTempDir(): File {
    val dir = File.createTempFile("testdir", "")
    dir.delete()
    dir.mkdirs()
    return dir
}
