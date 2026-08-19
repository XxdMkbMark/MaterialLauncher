/*
 * Copyright (C) 2026 Mark <github@xxdmkbmark> & Pidan <github@bretren>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cc.lanternmc.materiallauncher.core.launch

import java.util.concurrent.ConcurrentHashMap
import cc.lanternmc.materiallauncher.api.RunningGameInfo
import cc.lanternmc.materiallauncher.util.RuntimeInfo
import cc.lanternmc.materiallauncher.core.util.Logger

/**
 * 真正的游戏进程管理器：持有底层 [Process] 句柄，支持查询、优雅关闭与强制结束。
 *
 * 启动时由 [LaunchService] 注册；进程退出后自动移除并记录退出码。
 */
class GameProcessManager {

    private val processes = ConcurrentHashMap<Int, Process>()
    private val meta = ConcurrentHashMap<Int, Triple<String, String, String>>() // pid -> (gameDir, versionId, username)

    /** 注册一个已启动的游戏进程及其元信息。 */
    fun register(process: Process, gameDir: String, versionId: String, username: String) {
        val pid = process.pid().toInt()
        processes[pid] = process
        meta[pid] = Triple(gameDir, versionId, username)
        // 进程结束后自清理
        process.onExit().whenComplete { p, _ ->
            val exitCode = runCatching { p.exitValue() }.getOrDefault(-1)
            processes.remove(pid)
            meta.remove(pid)
            Logger.info("游戏进程 $pid 已退出, exit=$exitCode")
        }
    }

    /** 当前在运行的游戏进程列表。 */
    fun list(): List<RunningGameInfo> {
        val result = mutableListOf<RunningGameInfo>()
        for ((pid, process) in processes) {
            val m = meta[pid] ?: continue
            result.add(
                RunningGameInfo(
                    pid = pid,
                    gameDir = m.first,
                    versionId = m.second,
                    username = m.third,
                    alive = process.isAlive,
                ),
            )
        }
        return result.sortedBy { it.pid }
    }

    fun isRunning(pid: Int): Boolean = processes[pid]?.isAlive == true

    fun get(pid: Int): RunningGameInfo? {
        val process = processes[pid] ?: return null
        val m = meta[pid] ?: return null
        return RunningGameInfo(
            pid = pid,
            gameDir = m.first,
            versionId = m.second,
            username = m.third,
            alive = process.isAlive,
            exitCode = if (process.isAlive) null else runCatching { process.exitValue() }.getOrNull(),
        )
    }

    /** 优雅关闭游戏进程。返回是否能定位到该进程。 */
    fun stop(pid: Int): Boolean {
        val process = processes[pid] ?: return false
        return runCatching {
            if (process.isAlive) process.destroy()
            true
        }.getOrDefault(false)
    }

    /** 强制结束游戏进程。 */
    fun kill(pid: Int): Boolean {
        val process = processes[pid] ?: return false
        return runCatching {
            if (process.isAlive) process.destroyForcibly()
            true
        }.getOrDefault(false)
    }

    /**
     * 根据系统可用内存给出推荐的 `-Xmx` 建议值（MB）。
     * 规则：≥16GB→8G，8~15GB→6G，4~7GB→4G，其余→2G。
     */
    fun suggestMaxMemoryMb(): Int {
        val totalGb = RuntimeInfo.totalPhysicalMemoryGb()
        return when {
            totalGb >= 16 -> 8192
            totalGb >= 8 -> 6144
            totalGb >= 4 -> 4096
            else -> 2048
        }
    }
}
