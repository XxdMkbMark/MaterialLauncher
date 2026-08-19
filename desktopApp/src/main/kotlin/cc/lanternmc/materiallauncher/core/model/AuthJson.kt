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
package cc.lanternmc.materiallauncher.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 微软 / Xbox / Minecraft 认证接口的响应模型（后端专用）。
 */

@Serializable
data class XboxAuthResponse(
    @SerialName("Token")
    val token: String = "",

    @SerialName("DisplayClaims")
    val displayClaims: XboxDisplayClaims = XboxDisplayClaims(),
)

@Serializable
data class XstsResponse(
    @SerialName("Token")
    val token: String = "",

    @SerialName("DisplayClaims")
    val displayClaims: XboxDisplayClaims = XboxDisplayClaims(),
)

@Serializable
data class XboxDisplayClaims(
    val xui: List<XboxUserHash> = emptyList(),
)

@Serializable
data class XboxUserHash(
    val uhs: String = "",
)

@Serializable
data class MinecraftLoginResponse(
    @SerialName("access_token")
    val accessToken: String = "",

    @SerialName("expires_in")
    val expiresIn: Long = 0,

    val username: String = "",
)

@Serializable
data class MinecraftProfileResponse(
    val id: String = "",
    val name: String = "",
)
