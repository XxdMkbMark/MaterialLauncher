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

import kotlinx.serialization.Serializable

/**
 * 前后端共享的数据模型（契约层）。
 * 前端只依赖 `api` 包，后端在 `core` 包中实现。
 */

@Serializable
data class JavaInstallation(
    val path: String = "",
    val home: String = "",
    val javaType: String = "",
    val version: String = "",
    val vendor: String = "",
    val architecture: String = "",
    val lastVerified: String = "",
)

@Serializable
data class DownloadPathConfig(
    val path: String = "",
    val source: String = "default",
)

@Serializable
data class DownloadConfig(
    val minecraft: DownloadPathConfig = DownloadPathConfig(),
    val java: DownloadPathConfig = DownloadPathConfig(source = "launcher"),
    val username: String = "TestUser",
    val accountId: String = "",
    /** 下载源策略：official / mirror / auto。 */
    val mirrorSource: String = "auto",
    /** 自定义 JVM 启动参数（空格分隔），追加到默认参数之后。 */
    val jvmArgs: String = "",
    /** 自定义游戏参数（空格分隔），追加到默认游戏参数之后。 */
    val gameArgs: String = "",
)

@Serializable
data class MinecraftVersionEntry(
    val id: String,
    val type: String,
    val url: String,
    val releaseTime: String,
)

@Serializable
data class JavaReleaseInfo(
    val id: String,
    val version: String,
    val featureVersion: Int,
    val downloadUrl: String,
    val downloadSize: Long,
    val sha256: String = "",
)

@Serializable
data class DownloadProgress(
    val id: String,
    val type: String,
    val item: String,
    val status: String,
    val downloaded: Long = 0,
    val total: Long = 0,
    val error: String? = null,
)

/**
 * 启动器账户。type = "offline" | "online"。
 */
@Serializable
data class Account(
    val id: String = "",
    val type: String = "offline",
    val username: String = "",
    val uuid: String = "",
    val accessToken: String = "",
    val userType: String = "legacy",
    val msToken: String = "",
    val refreshToken: String = "",
    val msExpiresAt: Long = 0,
    val lastRefreshed: String = "",
)

@Serializable
data class DeviceCodeInfo(
    val deviceCode: String = "",
    val userCode: String = "",
    val verificationUri: String = "",
    val expiresIn: Long = 0,
    val interval: Long = 5,
    val message: String = "",
)

data class LaunchRequest(
    val javaPath: String,
    val gameDir: String,
    val versionId: String,
    val username: String,
    val maxMemory: String,
    val isolateVersion: Boolean,
    val accessToken: String = "0",
    val uuid: String = "00000000-0000-0000-0000-000000000000",
    val userType: String = "legacy",
    /** 可选：正版账户的 id。提供时后端会按此精确定位账户并自动续期 token。 */
    val accountId: String = "",
)

/** 一个正在运行（或已退出）的游戏进程快照。 */
@Serializable
data class RunningGameInfo(
    val pid: Int,
    val gameDir: String = "",
    val versionId: String = "",
    val username: String = "",
    val alive: Boolean = false,
    val exitCode: Int? = null,
)

/**
 * 游戏实例：一个独立命名的隔离游戏环境。
 * 每个实例拥有自己的 gameDir（独立存档/设置），并锁定一个 MC 版本。
 */
@Serializable
data class GameInstance(
    val id: String = "",
    val name: String = "",
    val versionId: String = "",
    /** 实例独立游戏目录（绝对路径）。 */
    val gameDir: String = "",
    val javaPath: String = "",
    val maxMemory: String = "2048M",
    val jvmArgs: String = "",
    val createdAt: String = "",
    val lastLaunched: String = "",
)
