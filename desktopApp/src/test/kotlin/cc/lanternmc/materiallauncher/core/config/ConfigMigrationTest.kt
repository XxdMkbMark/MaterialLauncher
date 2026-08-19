/*
 * Copyright (C) 2026 Mark <github@xxdmkbmark> & Pidan <github@bretren>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package cc.lanternmc.materiallauncher.core.config

import cc.lanternmc.materiallauncher.core.createTempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigMigrationTest {

    @Test
    fun `migrates legacy config toml into global toml and archives original`() {
        val dir = createTempDir()
        try {
            File(dir, "config.toml").writeText(
                """
                [minecraft]
                path = "D:\\games\\mc"
                source = "custom"
                [java]
                path = "C:\\java\\17"
                source = "launcher"
                [account]
                username = "Bret"
                account_id = "acc-99"
                [download]
                mirror_source = "mirror"
                [launch]
                jvm_args = "-XX:+UseG1GC"
                game_args = "--fullscreen"
                """.trimIndent(),
            )
            ConfigMigration.migrateIfNeeded(dir.absolutePath)

            val globalFile = File(dir, "global.toml")
            assertTrue(globalFile.isFile, "global.toml 应被创建")
            val global = GlobalConfigStore(globalFile.absolutePath).load()
            assertEquals("D:\\games\\mc", global.minecraftPath)
            assertEquals("C:\\java\\17", global.javaPath)
            assertEquals("Bret", global.defaultUsername)
            assertEquals("acc-99", global.defaultAccountId)
            assertEquals("-XX:+UseG1GC", global.defaultJvmArgs)
            assertEquals("--fullscreen", global.defaultGameArgs)
            assertEquals("mirror", global.downloadSource)

            val backup = File(dir, "config.toml.migrated")
            assertTrue(backup.isFile, "旧文件应被改名为备份")
            assertFalse(File(dir, "config.toml").exists(), "旧文件不应再以原名存在")
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `migration is idempotent`() {
        val dir = createTempDir()
        try {
            File(dir, "config.toml").writeText(
                """
                [minecraft]
                path = "P"
                """.trimIndent(),
            )
            ConfigMigration.migrateIfNeeded(dir.absolutePath)
            val firstGlobal = File(dir, "global.toml").readText()

            // 第二次调用：不应重复写入
            ConfigMigration.migrateIfNeeded(dir.absolutePath)
            val secondGlobal = File(dir, "global.toml").readText()
            assertEquals(firstGlobal, secondGlobal)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `skips migration when no legacy file exists`() {
        val dir = createTempDir()
        try {
            ConfigMigration.migrateIfNeeded(dir.absolutePath)
            assertFalse(File(dir, "global.toml").exists())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `archives legacy file when global already exists`() {
        val dir = createTempDir()
        try {
            File(dir, "config.toml").writeText("[minecraft]\npath = \"old\"")
            File(dir, "global.toml").writeText("minecraft_path = \"new\"")
            ConfigMigration.migrateIfNeeded(dir.absolutePath)
            // global.toml 内容应保持用户已写的版本
            val global = GlobalConfigStore(File(dir, "global.toml").absolutePath).load()
            assertEquals("new", global.minecraftPath)
            // 旧文件被改名归档
            assertTrue(File(dir, "config.toml.migrated").isFile)
        } finally {
            dir.deleteRecursively()
        }
    }
}