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

/**
 * 下载源策略。
 *
 * - [OFFICIAL]：只走官方源（Mojang / Adoptium）。
 * - [MIRROR]：只走镜像源（BMCLAPI 等）。
 * - [AUTO]：优先镜像源，失败时回退官方源（国内网络加速的推荐选择）。
 */
enum class DownloadMirrorSource(val configValue: String) {
    OFFICIAL("official"),
    MIRROR("mirror"),
    AUTO("auto");

    companion object {
        fun fromConfig(value: String?): DownloadMirrorSource =
            entries.firstOrNull { it.configValue == value } ?: AUTO
    }
}

/**
 * 官方下载 URL → 镜像 URL 的重写器。
 *
 * 覆盖 Minecraft 生态最常见的域名：
 * - launchermeta.mojang.com  版本清单 / 版本 JSON
 * - launcher.s3.amazonaws.com 版本 JSON（历史）
 * - piston-meta.mojang.com   版本 JSON（新）
 * - piston-data.mojang.com   client jar / server jar
 * - resources.download.minecraft.net 资源文件
 * - libraries.minecraft.net  依赖库
 *
 * BMCLAPI 镜像路径与官方一致，直接替换域名即可。
 */
object MirrorUrlRewriter {

    private data class Rule(val prefix: String, val mirrorBase: String)

    // 官方域名 → BMCLAPI 镜像。按最长前缀优先匹配。
    private val rules = listOf(
        Rule("https://resources.download.minecraft.net/", "https://bmclapi2.bangbang93.com/assets/"),
        Rule("https://launchermeta.mojang.com/", "https://bmclapi2.bangbang93.com/"),
        Rule("https://piston-meta.mojang.com/", "https://bmclapi2.bangbang93.com/"),
        Rule("https://piston-data.mojang.com/", "https://bmclapi2.bangbang93.com/"),
        Rule("https://launcher.s3.amazonaws.com/", "https://bmclapi2.bangbang93.com/"),
        Rule("https://libraries.minecraft.net/", "https://bmclapi2.bangbang93.com/maven/"),
        Rule("https://launcher.mojang.com/", "https://bmclapi2.bangbang93.com/"),
    ).sortedByDescending { it.prefix.length }

    /** 若该 URL 命中已知官方域名，返回镜像 URL；否则返回 null。 */
    fun toMirrorUrl(url: String): String? {
        for (rule in rules) {
            if (url.startsWith(rule.prefix)) {
                return rule.mirrorBase + url.removePrefix(rule.prefix)
            }
        }
        return null
    }

    /**
     * 按策略返回尝试顺序：
     * - [DownloadMirrorSource.OFFICIAL]：仅官方
     * - [DownloadMirrorSource.MIRROR]：仅镜像
     * - [DownloadMirrorSource.AUTO]：镜像 → 官方
     */
    fun candidates(url: String, source: DownloadMirrorSource): List<String> {
        val mirror = toMirrorUrl(url)
        return when (source) {
            DownloadMirrorSource.OFFICIAL -> listOf(url)
            DownloadMirrorSource.MIRROR -> listOfNotNull(mirror ?: url)
            DownloadMirrorSource.AUTO -> if (mirror != null) listOf(mirror, url) else listOf(url)
        }
    }

    /**
     * Adoptium JDK 归档的镜像候选。
     *
     * 官方归档在 github.com/adoptium/.../releases/download/<tag>/<file>，
     * 国内镜像（清华/华为）提供同构路径。由于归档文件名与镜像结构不完全一致，
     * 这里按文件名与 tag 直接拼镜像路径，失败时回退官方。
     */
    fun adoptiumCandidates(
        officialUrl: String,
        source: DownloadMirrorSource,
    ): List<String> {
        val mirror = toAdoptiumMirrorUrl(officialUrl)
        return when (source) {
            DownloadMirrorSource.OFFICIAL -> listOf(officialUrl)
            DownloadMirrorSource.MIRROR -> listOfNotNull(mirror ?: officialUrl)
            DownloadMirrorSource.AUTO -> if (mirror != null) listOf(mirror, officialUrl) else listOf(officialUrl)
        }
    }

    /**
     * 把 Adoptium GitHub 归档 URL 转换为国内镜像 URL。
     *
     * 例：https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9%2B9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.9_9.zip
     *  → https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jdk/x64/windows/hotspot/OpenJDK17U-jdk_x64_windows_hotspot_17.0.9_9.zip
     */
    fun toAdoptiumMirrorUrl(url: String): String? {
        // 形如 https://github.com/adoptium/<repo>/releases/download/<tag>/<fileName>
        val match = Regex(
            "^https://github\\.com/adoptium/(?:temurin|jdk)(\\d+)[^/]*/releases/download/[^/]+/([^/?#]+)$",
        ).find(url) ?: return null
        val major = match.groupValues[1]
        val fileName = match.groupValues[2]

        // 从文件名解析镜像子路径：OpenJDK17U-jdk_x64_windows_hotspot_17.0.9_9.zip
        // 格式: OpenJDK<major>U-<type>_<arch>_<os>_<jvm>_<version>.<ext>
        val fileMatch = Regex(
            "^OpenJDK\\d+U-(jdk|jre)_([^_]+)_(windows|mac|linux)_([^_]+)_(.+)$",
        ).find(fileName) ?: return null
        val imageType = fileMatch.groupValues[1]
        val arch = fileMatch.groupValues[2]
        val os = fileMatch.groupValues[3]

        // 清华镜像路径结构：<major>/<imageType>/<arch>/<os>/<jvm>/<fileName>
        val osPath = when (os) {
            "windows" -> "windows"
            "mac" -> "mac"
            else -> "linux"
        }
        val archPath = when (arch) {
            "x64" -> "x64"
            "aarch64" -> "aarch64"
            "x86" -> "x86"
            else -> arch
        }
        return "https://mirrors.tuna.tsinghua.edu.cn/Adoptium/$major/$imageType/$archPath/$osPath/hotspot/$fileName"
    }
}
