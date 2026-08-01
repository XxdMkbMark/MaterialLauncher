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
 * 前端只依赖 [api] 包，后端在 [core] 包中实现。
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

data class LaunchRequest(
    val javaPath: String,
    val gameDir: String,
    val versionId: String,
    val username: String,
    val maxMemory: String,
    val isolateVersion: Boolean,
)
