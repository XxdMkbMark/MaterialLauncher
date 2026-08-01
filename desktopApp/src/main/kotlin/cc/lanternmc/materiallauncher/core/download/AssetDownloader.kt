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

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import cc.lanternmc.materiallauncher.core.model.AssetIndex
import cc.lanternmc.materiallauncher.core.model.AssetIndexInfo
import cc.lanternmc.materiallauncher.core.util.HttpUtil
import cc.lanternmc.materiallauncher.core.util.Logger
import cc.lanternmc.materiallauncher.core.util.Sha1

/**
 * 并发下载 / 校验 Minecraft 的 assets 资源（官方 resources.download.minecraft.net）。
 */
class AssetDownloader {
    companion object {
        private const val ASSET_BASE_URL = "https://resources.download.minecraft.net"
        private const val MAX_CONCURRENCY = 20
    }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun downloadAssets(gameDir: String, assetIndex: AssetIndexInfo) {
        val assetsDir = File(gameDir, "assets")
        val indexesDir = File(assetsDir, "indexes")
        val objectsDir = File(assetsDir, "objects")
        indexesDir.mkdirs()
        objectsDir.mkdirs()

        val indexFile = File(indexesDir, "${assetIndex.id}.json")
        if (!indexFile.isFile) {
            val bytes = HttpUtil.getBytes(assetIndex.url)
            indexFile.writeBytes(bytes)
        }

        val indexData = json.decodeFromString<AssetIndex>(indexFile.readText())
        Logger.info("正在并发检查/下载 Asset 文件，共 ${indexData.objects.size} 个")

        val semaphore = Semaphore(MAX_CONCURRENCY)
        coroutineScope {
            indexData.objects.forEach { (_, asset) ->
                launch(Dispatchers.IO) {
                    semaphore.withPermit {
                        val hashPrefix = asset.hash.take(2)
                        val targetDir = File(objectsDir, hashPrefix)
                        val targetFile = File(targetDir, asset.hash)
                        if (Sha1.isFileValid(targetFile.absolutePath, asset.hash, asset.size)) return@withPermit
                        targetDir.mkdirs()
                        val url = "$ASSET_BASE_URL/$hashPrefix/${asset.hash}"
                        try {
                            HttpUtil.downloadFile(url, targetFile.absolutePath) { _, _ -> }
                        } catch (e: Exception) {
                            Logger.warn("下载 Asset 失败: ${e.message}")
                        }
                    }
                }
            }
        }
        Logger.info("官方 Assets 资源全部校验/补齐完毕！")
    }
}
