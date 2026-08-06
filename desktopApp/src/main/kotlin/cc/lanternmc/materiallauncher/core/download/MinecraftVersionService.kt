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
import cc.lanternmc.materiallauncher.api.MinecraftVersionEntry
import cc.lanternmc.materiallauncher.core.model.MinecraftManifest
import cc.lanternmc.materiallauncher.core.util.HttpUtil
import cc.lanternmc.materiallauncher.core.util.compareMinecraftVersion

object MinecraftVersionService {
    private const val MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json"
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 拉取官方版本清单，过滤 release 且 ≥1.6，按版本号倒序。
     * [source] 控制是否优先使用镜像源（AUTO 时镜像失败会回退官方）。
     */
    suspend fun fetchVersions(source: DownloadMirrorSource = DownloadMirrorSource.AUTO): List<MinecraftVersionEntry> {
        val text = fetchWithMirrorFallback(MANIFEST_URL, source)
        val manifest = json.decodeFromString<MinecraftManifest>(text)
        return manifest.versions
            .filter { it.type == "release" && compareMinecraftVersion(it.id, "1.6") >= 0 }
            .sortedWith { a, b -> compareMinecraftVersion(b.id, a.id) }
            .map {
                MinecraftVersionEntry(
                    id = it.id,
                    type = it.type,
                    url = it.url,
                    releaseTime = it.releaseTime,
                )
            }
    }

    /** 按镜像策略依次尝试候选 URL，全部失败才抛异常。 */
    private suspend fun fetchWithMirrorFallback(url: String, source: DownloadMirrorSource): String {
        var lastError: Exception? = null
        for (candidate in MirrorUrlRewriter.candidates(url, source)) {
            try {
                return HttpUtil.getString(candidate)
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("所有下载源均失败: $url")
    }
}
