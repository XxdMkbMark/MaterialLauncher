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
import java.util.regex.Pattern
import cc.lanternmc.materiallauncher.core.model.Artifact
import cc.lanternmc.materiallauncher.core.model.Library
import cc.lanternmc.materiallauncher.core.model.LibraryRule
import cc.lanternmc.materiallauncher.core.util.ArchiveExtractor
import cc.lanternmc.materiallauncher.core.util.HttpUtil
import cc.lanternmc.materiallauncher.core.util.Logger
import cc.lanternmc.materiallauncher.core.util.Os
import cc.lanternmc.materiallauncher.core.util.SafePath
import cc.lanternmc.materiallauncher.core.util.Sha1
import cc.lanternmc.materiallauncher.core.util.currentOs
import cc.lanternmc.materiallauncher.core.util.is32Bit
import cc.lanternmc.materiallauncher.core.util.isArm64

/**
 * 下载 Minecraft 依赖库，处理 rules / natives，返回 classpath。
 */
class LibraryDownloader {

    /**
     * @return 应加入 classpath 的 jar 绝对路径列表
     */
    suspend fun downloadLibraries(
        gameDir: String,
        nativesDir: String,
        libraries: List<Library>,
        source: DownloadMirrorSource = DownloadMirrorSource.AUTO,
    ): List<String> {
        val libraryDir = File(gameDir, "libraries")
        File(nativesDir).mkdirs()
        val classpath = mutableListOf<String>()

        for (library in libraries) {
            if (!libraryAllowed(library) || library.downloads == null) continue

            val nativeArtifact = nativeArtifactForCurrentPlatform(library)
            if (nativeArtifact.second) {
                val artifact = nativeArtifact.first ?: continue
                val jarPath = File(libraryDir, artifact.path).absolutePath
                ensureArtifact(jarPath, artifact, source)
                val excludes = mutableListOf("META-INF/")
                library.extract?.exclude?.let { excludes.addAll(it) }
                ArchiveExtractor.extractNativeJar(jarPath, nativesDir, excludes)
                continue
            }

            val artifact = library.downloads.artifact ?: continue
            // 路径穿越防护：来自版本 JSON 的相对路径必须合法
            if (!SafePath.isSafeRelativePath(artifact.path)) {
                Logger.warn("拒绝非法依赖路径: ${artifact.path}")
                continue
            }
            val targetPath = File(libraryDir, artifact.path).absolutePath
            ensureArtifact(targetPath, artifact, source)
            classpath.add(targetPath)
        }
        return classpath
    }

    private suspend fun ensureArtifact(
        targetPath: String,
        artifact: Artifact,
        source: DownloadMirrorSource,
    ) {
        if (Sha1.isFileValid(targetPath, artifact.sha1, artifact.size)) return
        if (artifact.url.isBlank()) throw IllegalStateException("artifact URL 为空: ${artifact.path}")
        var lastError: Exception? = null
        // 失败自动重下 + 按镜像策略回退
        for (attempt in 1..MAX_LIBRARY_ATTEMPTS) {
            for (candidate in MirrorUrlRewriter.candidates(artifact.url, source)) {
                try {
                    File(targetPath).delete()
                    HttpUtil.downloadFile(candidate, targetPath) { _, _ -> }
                    if (Sha1.isFileValid(targetPath, artifact.sha1, artifact.size)) return
                    File(targetPath).delete()
                } catch (e: Exception) {
                    lastError = e
                }
            }
            Logger.warn("依赖下载失败（第 $attempt 次，将重试）: ${artifact.path}")
        }
        File(targetPath).delete()
        throw IllegalStateException("下载依赖失败 ${artifact.path}: ${lastError?.message}")
    }

    companion object {
        /** 单个依赖库的最大下载尝试次数（含重试）。 */
        internal const val MAX_LIBRARY_ATTEMPTS = 3
    }

    private fun nativeArtifactForCurrentPlatform(library: Library): Pair<Artifact?, Boolean> {
        val downloads = library.downloads ?: return Pair(null, false)
        if (library.natives.isNotEmpty()) {
            val classifier = library.natives[minecraftOsName()] ?: return Pair(null, true)

            val resolved = classifier.replace("\${arch}", minecraftArchBits())
            val artifact = downloads.classifiers[resolved] ?: return Pair(null, true)
            return Pair(artifact, true)
        }
        val artifact = downloads.artifact
        if (artifact == null || !artifact.path.lowercase().contains("-natives-")) return Pair(null, false)
        if (!nativeArtifactMatchesCurrentPlatform(artifact.path)) return Pair(null, true)
        return Pair(artifact, true)
    }

    private fun nativeArtifactMatchesCurrentPlatform(path: String): Boolean {
        val name = path.substringAfterLast('/').lowercase()
        val platform = "natives-${minecraftOsName()}"
        val matches = when {
            name.contains(platform) -> true
            currentOs == Os.MAC && name.contains("natives-macos") -> true
            else -> false
        }
        if (!matches) return false
        return when {
            name.contains("arm64") || name.contains("aarch64") -> isArm64
            name.contains("-x86.") || name.contains("-32.") -> is32Bit
            else -> !isArm64 && !is32Bit
        }
    }

    private fun libraryAllowed(library: Library): Boolean {
        if (library.rules.isEmpty()) return true
        var allowed = false
        for (rule in library.rules) {
            if (!libraryRuleMatches(rule)) continue
            allowed = rule.action == "allow"
        }
        return allowed
    }

    private fun libraryRuleMatches(rule: LibraryRule): Boolean {
        if (rule.features.isNotEmpty()) return false
        val os = rule.os ?: return true
        if (os.name != null && os.name != minecraftOsName()) return false
        if (os.version != null) {
            val osVersion = System.getProperty("os.version") ?: return false
            if (!runCatching { Pattern.matches(os.version, osVersion) }.getOrDefault(false)) return false
        }
        if (os.arch != null) {
            val matched = runCatching { Pattern.matches(os.arch, minecraftArchName()) }.getOrDefault(false)
            return matched
        }
        return true
    }

    private fun minecraftOsName(): String = when (currentOs) {
        Os.WINDOWS -> "windows"
        Os.MAC -> "osx"
        Os.LINUX -> "linux"
        else -> "linux"
    }

    private fun minecraftArchBits(): String = if (is32Bit) "32" else "64"

    private fun minecraftArchName(): String = when {
        is32Bit -> "x86"
        isArm64 -> "aarch64"
        else -> "x86_64"
    }
}
