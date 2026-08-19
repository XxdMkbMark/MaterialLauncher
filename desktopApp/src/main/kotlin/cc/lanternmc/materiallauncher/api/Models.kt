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
    /** 默认内存（如 2048M / 4G）。 */
    val defaultMaxMemory: String = "2048M",
)

/** 启动器本体配置（launcher.toml）：与具体实例/游戏无关的应用层选项。 */
@Serializable
data class LauncherSettings(
    val ui: UiSettings = UiSettings(),
    val download: DownloadDefaults = DownloadDefaults(),
)

@Serializable
data class UiSettings(
    /** 主题：light / dark / system。 */
    val theme: String = "system",
    /** 语言：zh-CN / en-US 等。 */
    val language: String = "zh-CN",
)

@Serializable
data class DownloadDefaults(
    /** 下载源策略：auto / mirror / official。 */
    val source: String = "auto",
    /** asset / library 下载并发数。 */
    val concurrency: Int = 8,
)

/** 全局游戏启动默认值（global.toml）：跨实例共享的兜底配置。 */
@Serializable
data class GlobalLaunchSettings(
    val minecraftPath: String = "",
    val javaPath: String = "",
    val defaultAccountId: String = "",
    val defaultUsername: String = "TestUser",
    val defaultMaxMemory: String = "2048M",
    val defaultJvmArgs: String = "",
    val defaultGameArgs: String = "",
    val downloadSource: String = "auto",
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
    /** 瞬时下载速度（字节/秒），仅 downloading 状态有意义。 */
    val speedBytesPerSec: Long = 0,
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
 *
 * `extras` 用于存放扩展配置（mod 路径、整合包元数据、Forge/Fabric 注入参数等）。
 * 该字段是 String→String KV 形式，新增扩展项无需修改 data class。
 *
 * 持久化格式：`extras` 不嵌入 `[[instance]]` 内部，而是作为独立的
 * `[[instance_extra]]` 数组表持久化（每行带 `instance_id` / `key` / `value` 三列），
 * 由 [cc.lanternmc.materiallauncher.core.config.InstanceStore] 在加载时按
 * `instance_id` 反向归并回对应的 [GameInstance.extras]。这种布局是因为 TOML
 * 规范禁止在数组表内部再嵌套子表；将 extras 拍平为同级的 `[[instance_extra]]`
 * 既保留了 KV 扩展能力，又对外部解析器友好。
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
    /** 扩展配置：mod / 整合包 / Forge / Fabric 等未来字段的 KV 存储。 */
    val extras: Map<String, String> = emptyMap(),
)
