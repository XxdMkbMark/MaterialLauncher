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

    /** 卸载已安装的 Minecraft 版本（删除 versions/<id> 目录）。返回是否删除成功。 */
    suspend fun deleteMinecraftVersion(gameDir: String, versionId: String): Boolean

    /** 卸载启动器自带的 Java（删除其 home 目录）。返回是否删除成功。 */
    suspend fun deleteJavaInstallation(javaPath: String): Boolean

    // ---- 远程版本列表 ----
    suspend fun getMinecraftVersions(): List<MinecraftVersionEntry>
    suspend fun getJavaVersions(): List<JavaReleaseInfo>

    // ---- 下载（异步，进度通过事件推送） ----
    fun startMinecraftDownload(versionId: String)
    fun startJavaDownload(javaVersionId: String)

    /** 取消进行中的下载任务，taskKey 形如 "minecraft:<id>" / "java:<id>"。 */
    fun cancelDownload(taskKey: String)

    // ---- 游戏启动 ----
    suspend fun launchMinecraft(request: LaunchRequest): Int
    suspend fun resolveLaunchJava(gameDir: String, versionId: String, preferred: String): String

    // ---- 游戏进程管理 ----
    fun listRunningGames(): List<RunningGameInfo>
    fun stopGame(pid: Int): Boolean
    fun killGame(pid: Int): Boolean

    // ---- 内存建议 ----
    fun suggestMaxMemoryMb(): Int

    // ---- 多实例管理 ----
    suspend fun listInstances(): List<GameInstance>
    suspend fun createInstance(name: String, versionId: String): GameInstance
    suspend fun saveInstance(instance: GameInstance)
    suspend fun deleteInstance(instanceId: String): Boolean

    /** 启动一个实例（使用实例自身的 gameDir/版本/Java/内存配置）。返回 PID。 */
    suspend fun launchInstance(instanceId: String, username: String, accessToken: String, uuid: String, userType: String): Int

    // ---- 账户 ----
    suspend fun getAccounts(): List<Account>
    suspend fun addOfflineAccount(username: String): Account
    suspend fun removeAccount(accountId: String)
    suspend fun refreshAccount(accountId: String): Account?
    fun startMicrosoftLogin()

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
