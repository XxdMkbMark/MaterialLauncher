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
package cc.lanternmc.materiallauncher.core.util

import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipFile
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream

/**
 * 归档解压工具：zip / tar.gz，全部带路径穿越防护。
 */
object ArchiveExtractor {

    /**
     * 从 native jar 中提取原生库到目标目录（跳过 META-INF 与指定 exclude）。
     */
    fun extractNativeJar(jarPath: String, destination: String, excludes: List<String> = listOf("META-INF/")) {
        ZipFile(jarPath).use { zip ->
            for (entry in zip.entries()) {
                val name = entry.name.trimStart('/')
                if (name.isEmpty() || entry.isDirectory) continue
                if (excludes.any { name.startsWith(it) }) continue
                val target = safeJoin(destination, name)
                    ?: throw IllegalArgumentException("非法的 native 归档路径: ${entry.name}")
                File(target).parentFile?.mkdirs()
                zip.getInputStream(entry).use { input ->
                    File(target).outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }

    /**
     * 解压 zip / tar.gz 到 destDir。
     * 若归档含单个顶层目录，则剥掉该顶层目录，使 destDir 本身就是解压根目录。
     * 这样无论归档内目录名是什么（如 JDK8 的 jdk8u392-b08 与目标名不同），destDir 内都是 JDK 根。
     */
    fun extractArchive(archivePath: String, destDir: String) {
        if (archivePath.endsWith(".zip", ignoreCase = true)) {
            extractZip(archivePath, destDir)
        } else if (archivePath.endsWith(".tar.gz", ignoreCase = true) || archivePath.endsWith(".tgz", ignoreCase = true)) {
            extractTarGz(archivePath, destDir)
        } else {
            throw IllegalArgumentException("不支持的归档格式: $archivePath")
        }
    }

    private fun extractZip(archivePath: String, destDir: String) {
        val topDir = firstZipTopDir(archivePath)
        ZipFile(archivePath).use { zip ->
            for (entry in zip.entries()) {
                var name = entry.name.replace('\\', '/')
                if (topDir != null) {
                    if (name == topDir) continue
                    if (name.startsWith("$topDir/")) name = name.removePrefix("$topDir/")
                }
                if (name.isEmpty()) continue
                val target = safeJoin(destDir, name) ?: continue
                if (entry.isDirectory) {
                    File(target).mkdirs()
                    continue
                }
                File(target).parentFile?.mkdirs()
                zip.getInputStream(entry).use { input ->
                    File(target).outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }

    private fun extractTarGz(archivePath: String, destDir: String) {
        val topDir = firstTarTopDir(archivePath)
        FileInputStream(archivePath).use { fileInput ->
            GzipCompressorInputStream(fileInput).use { gzip ->
                TarArchiveInputStream(gzip).use { tar ->
                    while (true) {
                        val entry = tar.nextEntry ?: break
                        var name = entry.name.replace('\\', '/')
                        if (topDir != null) {
                            if (name == topDir) continue
                            if (name.startsWith("$topDir/")) name = name.removePrefix("$topDir/")
                        }
                        if (name.isEmpty()) continue
                        val target = safeJoin(destDir, name) ?: continue
                        if (entry.isDirectory) {
                            File(target).mkdirs()
                            continue
                        }
                        File(target).parentFile?.mkdirs()
                        File(target).outputStream().use { output -> tar.copyTo(output) }
                    }
                }
            }
        }
    }

    private fun firstZipTopDir(archivePath: String): String? {
        ZipFile(archivePath).use { zip ->
            for (entry in zip.entries()) {
                val idx = entry.name.indexOf('/')
                if (idx > 0) return entry.name.substring(0, idx)
            }
        }
        return null
    }

    private fun firstTarTopDir(archivePath: String): String? {
        FileInputStream(archivePath).use { fileInput ->
            GzipCompressorInputStream(fileInput).use { gzip ->
                TarArchiveInputStream(gzip).use { tar ->
                    val first = tar.nextEntry ?: return null
                    val idx = first.name.indexOf('/')
                    return if (idx > 0) first.name.substring(0, idx) else null
                }
            }
        }
    }

    private fun safeJoin(base: String, name: String): String? {
        val basePath = File(base).absoluteFile.toPath().normalize()
        val targetPath = File(base, name.replace('/', File.separatorChar)).toPath().normalize()
        return if (targetPath.startsWith(basePath)) targetPath.toString() else null
    }
}
