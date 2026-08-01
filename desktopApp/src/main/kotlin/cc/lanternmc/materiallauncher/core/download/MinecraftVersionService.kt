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
     */
    suspend fun fetchVersions(): List<MinecraftVersionEntry> {
        val text = HttpUtil.getString(MANIFEST_URL)
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
}
