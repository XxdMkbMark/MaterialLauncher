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
package cc.lanternmc.materiallauncher.api

import kotlinx.coroutines.flow.SharedFlow

/**
 * 前端调用后端的能力接口（契约层）。
 *
 * 前端（ui 包）只依赖本接口，后端（core 包）负责实现。
 * suspend 方法会被阻塞在后台线程执行，不会卡住 UI。
 */
interface LauncherApi {
    // ---- 配置 ----
    suspend fun getDownloadConfig(): DownloadConfig
    suspend fun saveDownloadConfig(config: DownloadConfig)
    suspend fun getDefaultMinecraftDir(): String
    suspend fun getLauncherMinecraftDir(): String
    suspend fun getLauncherJavaDir(): String

    // ---- 已安装内容 ----
    suspend fun getInstalledMinecraftVersions(mcPath: String): List<String>
    suspend fun findJavaPaths(): List<JavaInstallation>
    fun refreshJavaIndex(): Boolean

    // ---- 远程版本列表 ----
    suspend fun getMinecraftVersions(): List<MinecraftVersionEntry>
    suspend fun getJavaVersions(): List<JavaReleaseInfo>

    // ---- 下载（异步，进度通过事件推送） ----
    fun startMinecraftDownload(versionId: String)
    fun startJavaDownload(javaVersionId: String)

    // ---- 游戏启动 ----
    suspend fun launchMinecraft(request: LaunchRequest): Int

    // ---- 系统集成 ----
    suspend fun openDirectoryDialog(title: String): String?

    // ---- 日志 ----
    fun logInfo(message: String)
    fun logWarn(message: String)
    fun logError(message: String)
}

/**
 * 后端事件总线。前端通过收集 [events] 获取后端推送的进度 / 状态。
 */
interface LauncherEventBus {
    val events: SharedFlow<LauncherEvent>
}
