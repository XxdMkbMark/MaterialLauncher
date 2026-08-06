/*
 * Copyright (C) 2026 Mark <github@xxdmkbmark> & Pidan <github@bretren>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package cc.lanternmc.materiallauncher.core.launch

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 测试用长驻进程：睡 60 秒，供 GameProcessManager 测试注册/停止/结束。 */
object TestSleeper {
    @JvmStatic
    fun main(args: Array<String>) {
        Thread.sleep(60_000)
    }
}

class GameProcessManagerTest {

    private fun spawnSleeper(): Process {
        val javaBin = File(System.getProperty("java.home"), "bin/java").absolutePath
        val cp = System.getProperty("java.class.path")
        return ProcessBuilder(javaBin, "-cp", cp, TestSleeper::class.java.name)
            .redirectErrorStream(true)
            .start()
    }

    @Test
    fun `register then list shows running game`() {
        val manager = GameProcessManager()
        val process = spawnSleeper()
        try {
            manager.register(process, "D:\\games\\mc", "1.20.1", "Bret")
            val list = manager.list()
            assertTrue(list.isNotEmpty())
            val entry = list.first { it.pid == process.pid().toInt() }
            assertEquals("1.20.1", entry.versionId)
            assertEquals("Bret", entry.username)
            assertTrue(entry.alive)
            assertTrue(manager.isRunning(process.pid().toInt()))
            assertNotNull(manager.get(process.pid().toInt()))
        } finally {
            process.destroyForcibly()
        }
    }

    @Test
    fun `kill terminates the game process`() {
        val manager = GameProcessManager()
        val process = spawnSleeper()
        val pid = process.pid().toInt()
        try {
            manager.register(process, "g", "1.20", "u")
            assertTrue(manager.kill(pid))
            // 等待进程真正终止
            process.waitFor()
            Thread.sleep(300) // 等 onExit 回调清理注册表
            assertFalse(manager.isRunning(pid))
            assertNull(manager.get(pid))
        } finally {
            process.destroyForcibly()
        }
    }

    @Test
    fun `stop and kill on unknown pid return false`() {
        val manager = GameProcessManager()
        assertFalse(manager.stop(999999))
        assertFalse(manager.kill(999999))
        assertNull(manager.get(999999))
        assertTrue(manager.list().isEmpty())
    }

    @Test
    fun `suggestMaxMemoryMb returns a sane value`() {
        val manager = GameProcessManager()
        val mb = manager.suggestMaxMemoryMb()
        assertTrue(mb in listOf(2048, 4096, 6144, 8192), "unexpected suggestion: $mb")
    }
}
