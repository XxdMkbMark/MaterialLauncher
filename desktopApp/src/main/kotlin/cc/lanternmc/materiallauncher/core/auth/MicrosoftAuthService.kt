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
package cc.lanternmc.materiallauncher.core.auth

import java.time.OffsetDateTime
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import cc.lanternmc.materiallauncher.api.Account
import cc.lanternmc.materiallauncher.api.DeviceCodeInfo
import cc.lanternmc.materiallauncher.core.model.MinecraftLoginResponse
import cc.lanternmc.materiallauncher.core.model.MinecraftProfileResponse
import cc.lanternmc.materiallauncher.core.model.XboxAuthResponse
import cc.lanternmc.materiallauncher.core.model.XstsResponse
import cc.lanternmc.materiallauncher.core.util.HttpUtil
import kotlin.time.Duration.Companion.milliseconds

/**
 * Microsoft 正版验证（OAuth2 设备码流）：
 * 设备码 → 轮询 OAuth token → Xbox Live → XSTS → Minecraft login_with_xbox → 玩家资料。
 */
object MicrosoftAuthService {

    // Material Launcher 自有 Azure 应用（已通过 Mojang/Xbox Live 注册许可名单，
    // 2026-08 批准）。公共客户端，设备码流无需 client secret。
    // 自建 Azure 应用未注册 Xbox Live，会在 login_with_xbox 阶段返回 403 "Invalid app registration"。
    private const val CLIENT_ID = "496b4e7d-7115-4ffb-bdb9-04108320023d"

    private const val DEVICE_CODE_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode"
    private const val TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token"
    private const val XBOX_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate"
    private const val XSTS_AUTH_URL = "https://xsts.auth.xboxlive.com/xsts/authorize"
    private const val MINECRAFT_AUTH_URL = "https://api.minecraftservices.com/authentication/login_with_xbox"
    private const val PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile"

    private const val SCOPE = "XboxLive.signin offline_access"
    private val json = Json { ignoreUnknownKeys = true }

    data class OAuthToken(
        val accessToken: String,
        val refreshToken: String,
        val expiresIn: Long,
    )

    /**
     * ① 请求设备码，返回用户需要访问的链接 + 输入码。
     */
    suspend fun requestDeviceCode(): DeviceCodeInfo {
        val body = HttpUtil.postForm(
            DEVICE_CODE_URL,
            mapOf(
                "client_id" to CLIENT_ID,
                "scope" to SCOPE,
            ),
        )
        val doc = json.parseToJsonElement(body).jsonObject
        return DeviceCodeInfo(
            deviceCode = doc["device_code"]?.jsonPrimitive?.content.orEmpty(),
            userCode = doc["user_code"]?.jsonPrimitive?.content.orEmpty(),
            verificationUri = doc["verification_uri"]?.jsonPrimitive?.content.orEmpty(),
            expiresIn = doc["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: 900,
            interval = doc["interval"]?.jsonPrimitive?.content?.toLongOrNull() ?: 5,
            message = doc["message"]?.jsonPrimitive?.content.orEmpty(),
        )
    }

    /**
     * 按 interval 轮询 token 接口，直到用户完成授权或设备码过期。
     * [onStatus] 在每次轮询前回调，用于向 UI 推送等待状态。
     */
    suspend fun pollForToken(info: DeviceCodeInfo, onStatus: suspend (String) -> Unit): OAuthToken {
        val deadline = System.currentTimeMillis() + info.expiresIn * 1000
        val intervalMillis = info.interval.coerceAtLeast(1) * 1000
        while (System.currentTimeMillis() < deadline) {
            onStatus("waiting_user")
            val result = HttpUtil.postFormResult(
                TOKEN_URL,
                mapOf(
                    "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
                    "client_id" to CLIENT_ID,
                    "device_code" to info.deviceCode,
                ),
            )
            val doc = runCatching { json.parseToJsonElement(result.body).jsonObject }.getOrNull()
            val error = doc?.get("error")?.jsonPrimitive?.content
            when (error) {
                null, "", "authorization_pending" -> {
                    val accessToken = doc?.get("access_token")?.jsonPrimitive?.content
                    if (!accessToken.isNullOrBlank()) {
                        return OAuthToken(
                            accessToken = accessToken,
                            refreshToken = doc["refresh_token"]?.jsonPrimitive?.content.orEmpty(),
                            expiresIn = doc["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: 3600,
                        )
                    }
                }
                "authorization_declined" -> throw IllegalStateException("用户拒绝了授权")
                "expired_token" -> throw IllegalStateException("设备码已过期，请重新登录")
                else -> throw IllegalStateException("登录失败: $error")
            }
            delay(intervalMillis.milliseconds)
        }
        throw IllegalStateException("设备码已过期，请重新登录")
    }

    /**
     * ②~⑤ 把微软 access token 逐级换成 Minecraft 正版账户。
     */
    suspend fun exchangeToAccount(msAccessToken: String, msRefreshToken: String): Account {
        val xboxResp = HttpUtil.postJson(
            XBOX_AUTH_URL,
            buildJsonObject {
                put(
                    "Properties",
                    buildJsonObject {
                        put("AuthMethod", "RPS")
                        put("SiteName", "user.auth.xboxlive.com")
                        put("RpsTicket", "d=$msAccessToken")
                    },
                )
                put("RelyingParty", "http://auth.xboxlive.com")
                put("TokenType", "JWT")
            }.toString(),
        )
        val xbox = json.decodeFromString<XboxAuthResponse>(xboxResp)
        if (xbox.Token.isBlank()) throw IllegalStateException("Xbox Live 认证失败")

        val xstsResp = HttpUtil.postJson(
            XSTS_AUTH_URL,
            buildJsonObject {
                put(
                    "Properties",
                    buildJsonObject {
                        put("SandboxId", "RETAIL")
                        put("UserTokens", buildJsonArray { add(JsonPrimitive(xbox.Token)) })
                    },
                )
                put("RelyingParty", "rp://api.minecraftservices.com/")
                put("TokenType", "JWT")
            }.toString(),
        )
        val xsts = json.decodeFromString<XstsResponse>(xstsResp)
        val uhs = xsts.DisplayClaims.xui.firstOrNull()?.uhs
            ?: throw IllegalStateException("XSTS 未返回用户标识 (uhs)")

        val mcResp = HttpUtil.postJson(
            MINECRAFT_AUTH_URL,
            buildJsonObject { put("identityToken", "XBL3.0 x=$uhs;${xsts.Token}") }.toString(),
        )
        val mc = json.decodeFromString<MinecraftLoginResponse>(mcResp)

        val profileResp = HttpUtil.getResult(PROFILE_URL, mapOf("Authorization" to "Bearer ${mc.access_token}"))
        if (profileResp.statusCode == 403) {
            throw IllegalStateException("该账户未拥有 Minecraft Java 版")
        }
        if (profileResp.statusCode !in 200..299) {
            throw IllegalStateException("获取玩家资料失败: HTTP ${profileResp.statusCode}")
        }
        val profile = json.decodeFromString<MinecraftProfileResponse>(profileResp.body)

        return Account(
            id = profile.id,
            type = "online",
            username = profile.name,
            uuid = profile.id,
            accessToken = mc.access_token,
            userType = "msa",
            msToken = msAccessToken,
            refreshToken = msRefreshToken,
            msExpiresAt = System.currentTimeMillis() + mc.expires_in * 1000,
            lastRefreshed = OffsetDateTime.now().toString(),
        )
    }

    /**
     * 用 refresh token 续期微软 token 后重走 ②~⑤。
     */
    suspend fun refreshAccount(account: Account): Account {
        if (account.refreshToken.isBlank()) {
            throw IllegalStateException("账户缺少 refresh token，请重新登录")
        }
        val result = HttpUtil.postFormResult(
            TOKEN_URL,
            mapOf(
                "grant_type" to "refresh_token",
                "client_id" to CLIENT_ID,
                "refresh_token" to account.refreshToken,
                "scope" to SCOPE,
            ),
        )
        if (result.statusCode !in 200..299) {
            throw IllegalStateException("刷新 token 失败: HTTP ${result.statusCode} ${result.body}")
        }
        val doc = json.parseToJsonElement(result.body).jsonObject
        val newMsToken = doc["access_token"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("刷新 token 响应缺少 access_token")
        val newRefreshToken = doc["refresh_token"]?.jsonPrimitive?.content
            ?.takeIf { it.isNotBlank() }
            ?: account.refreshToken
        return exchangeToAccount(newMsToken, newRefreshToken)
    }
}
