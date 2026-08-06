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
package cc.lanternmc.materiallauncher.core.download

import kotlinx.serialization.json.Json
import cc.lanternmc.materiallauncher.api.JavaReleaseInfo
import cc.lanternmc.materiallauncher.core.model.JavaFeatureRelease
import cc.lanternmc.materiallauncher.core.util.HttpUtil
import cc.lanternmc.materiallauncher.core.util.Os
import cc.lanternmc.materiallauncher.core.util.currentOs
import cc.lanternmc.materiallauncher.core.util.isArm64

object JavaVersionService {
    private val ltsFeatureVersions = listOf(8, 11, 17, 21)
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 从 Adoptium API 拉取 LTS JDK（8/11/17/21）最新版本。
     */
    suspend fun fetchVersions(): List<JavaReleaseInfo> {
        val arch = if (isArm64) "arm64" else "x64"
        val osName = when (currentOs) {
            Os.WINDOWS -> "windows"
            Os.MAC -> "mac"
            else -> "linux"
        }
        val result = mutableListOf<JavaReleaseInfo>()
        for (featureVer in ltsFeatureVersions) {
            val url = "https://api.adoptium.net/v3/assets/latest/$featureVer/hotspot" +
                "?architecture=$arch&image_type=jdk&os=$osName&jvm_impl=hotspot&page_size=1"
            val releases = runCatching {
                json.decodeFromString<List<JavaFeatureRelease>>(HttpUtil.getString(url))
            }.getOrNull() ?: continue
            val first = releases.firstOrNull() ?: continue
            val binary = first.binary ?: continue
            val semver = first.version.semver
            result.add(
                JavaReleaseInfo(
                    id = "Java $featureVer ($semver)",
                    version = semver,
                    featureVersion = featureVer,
                    downloadUrl = binary.archive.link,
                    downloadSize = binary.archive.size,
                    sha256 = extractSha256(binary.archive.checksum),
                ),
            )
        }
        return result
    }

    /** Adoptium checksum 形如 "sha256:3f2...  " 或纯 hex，剥掉前缀与空白。 */
    fun extractSha256(checksum: String): String {
        val cleaned = checksum.trim().substringAfterLast(':').trim()
        if (cleaned.matches(Regex("^[0-9a-fA-F]{64}$"))) return cleaned.lowercase()
        return ""
    }
}
