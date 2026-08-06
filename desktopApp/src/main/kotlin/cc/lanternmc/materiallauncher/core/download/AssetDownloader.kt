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
import cc.lanternmc.materiallauncher.core.util.SafePath
import cc.lanternmc.materiallauncher.core.util.Sha1

/**
 * 并发下载 / 校验 Minecraft 的 assets 资源（官方 resources.download.minecraft.net）。
 */
class AssetDownloader {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun downloadAssets(
        gameDir: String,
        assetIndex: AssetIndexInfo,
        source: DownloadMirrorSource = DownloadMirrorSource.AUTO,
    ) {
        val assetsDir = File(gameDir, "assets")
        val indexesDir = File(assetsDir, "indexes")
        val objectsDir = File(assetsDir, "objects")
        indexesDir.mkdirs()
        objectsDir.mkdirs()

        // 路径穿越防护：来自版本 JSON 的索引 id 必须合法
        if (!SafePath.isSafeRelativePath(assetIndex.id)) {
            Logger.warn("拒绝非法资产索引 id: ${assetIndex.id}")
            return
        }
        val indexFile = File(indexesDir, "${assetIndex.id}.json")
        if (!indexFile.isFile) {
            // 资源索引也走镜像回退
            val bytes = downloadWithMirrorFallback(assetIndex.url, source)
            indexFile.writeBytes(bytes)
        }

        val indexData = json.decodeFromString<AssetIndex>(indexFile.readText())
        Logger.info("正在并发检查/下载 Asset 文件，共 ${indexData.objects.size} 个")

        val semaphore = Semaphore(MAX_CONCURRENCY)
        val failures = java.util.concurrent.atomic.AtomicInteger(0)
        coroutineScope {
            indexData.objects.forEach { (_, asset) ->
                launch(Dispatchers.IO) {
                    semaphore.withPermit {
                        val hashPrefix = asset.hash.take(2)
                        // 路径穿越防护：asset hash 必须是 40 位 hex，否则拒绝
                        if (!SafePath.isSafeAssetHash(asset.hash)) {
                            Logger.warn("拒绝非法 asset hash: ${asset.hash.take(8)}...")
                            return@withPermit
                        }
                        val targetDir = File(objectsDir, hashPrefix)
                        val targetFile = File(targetDir, asset.hash)
                        if (Sha1.isFileValid(targetFile.absolutePath, asset.hash, asset.size)) return@withPermit
                        targetDir.mkdirs()
                        val url = "$ASSET_BASE_URL/$hashPrefix/${asset.hash}"
                        // 失败自动重下：最多尝试 3 次，且按镜像策略回退
                        var attempts = 0
                        var lastError: String = ""
                        while (attempts < MAX_ASSET_ATTEMPTS) {
                            attempts++
                            try {
                                // 校验失败后重试必须 force：损坏的 dest 不能被"断点续传已存在"逻辑跳过
                                downloadWithMirrorFallback(url, source, targetFile.absolutePath, force = attempts > 1)
                                if (Sha1.isFileValid(targetFile.absolutePath, asset.hash, asset.size)) {
                                    return@withPermit
                                }
                                lastError = "SHA-1 校验不匹配"
                            } catch (e: Exception) {
                                // 协程取消必须传播，不能当作下载失败重试
                                if (e is kotlinx.coroutines.CancellationException) throw e
                                lastError = "${e.javaClass.simpleName}: ${e.message}"
                                if (attempts < MAX_ASSET_ATTEMPTS) {
                                    Logger.warn("Asset 下载失败（第 $attempts 次，将重试）: ${e.message}")
                                }
                            }
                        }
                        failures.incrementAndGet()
                        Logger.warn("下载 Asset 失败（已重试 $MAX_ASSET_ATTEMPTS 次）: $url  原因: $lastError")
                    }
                }
            }
        }
        if (failures.get() > 0) {
            throw IllegalStateException("${failures.get()} 个资源文件下载失败，无法启动")
        }

        if (indexData.virtual) {
            Logger.info("资源索引 ${assetIndex.id} 为 legacy 虚拟布局，正在生成 virtual 目录...")
            val virtualRoot = File(assetsDir, "virtual/${assetIndex.id}")
            indexData.objects.forEach { (path, asset) ->
                val src = File(File(objectsDir, asset.hash.take(2)), asset.hash)
                if (!src.isFile) {
                    throw IllegalStateException("资源缺失，无法生成 virtual 目录: $path")
                }
                val target = File(virtualRoot, path)
                if (target.isFile && target.length() == asset.size) return@forEach
                target.parentFile?.mkdirs()
                src.copyTo(target, overwrite = true)
            }
        }
        Logger.info("官方 Assets 资源全部校验/补齐完毕！")
    }

    /** 按镜像策略尝试下载；[dest] 非空时写入文件，否则返回字节。 */
    private suspend fun downloadWithMirrorFallback(
        url: String,
        source: DownloadMirrorSource,
        dest: String? = null,
        force: Boolean = false,
    ): ByteArray {
        var lastError: Exception? = null
        for (candidate in MirrorUrlRewriter.candidates(url, source)) {
            try {
                if (dest != null) {
                    // 强制重下时先删掉可能损坏的旧文件与残留 .part，避免续传拼接旧数据
                    if (force) {
                        File(dest).delete()
                        File("$dest.part").delete()
                    }
                    HttpUtil.downloadFile(candidate, dest, { _, _ -> }, force = force)
                    return ByteArray(0)
                }
                return HttpUtil.getBytes(candidate)
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("所有下载源均失败: $url")
    }

    companion object {
        private const val ASSET_BASE_URL = "https://resources.download.minecraft.net"
        private const val MAX_CONCURRENCY = 20

        /** 单个 Asset 文件的最大下载尝试次数（含重试）。 */
        internal const val MAX_ASSET_ATTEMPTS = 3
    }
}
