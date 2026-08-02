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
 * Minecraft / Adoptium 远程 JSON 的内部模型（后端专用）。
 */

@Serializable
data class VersionJson(
    val mainClass: String = "",
    val assetIndex: AssetIndexInfo = AssetIndexInfo(id = "", url = ""),
    val downloads: ClientDownloads? = null,
    val libraries: List<Library> = emptyList(),
    val javaVersion: JavaVersionInfo? = null,
)

@Serializable
data class JavaVersionInfo(
    val majorVersion: Int = 8,
    val component: String = "",
)

@Serializable
data class ClientDownloads(
    val client: ClientArtifact? = null,
    val server: ClientArtifact? = null,
)

@Serializable
data class ClientArtifact(
    val sha1: String = "",
    val size: Long = 0,
    val url: String = "",
)

@Serializable
data class AssetIndexInfo(
    val id: String = "",
    val url: String = "",
)

@Serializable
data class Library(
    val name: String = "",
    val downloads: LibraryDownloads? = null,
    val natives: Map<String, String> = emptyMap(),
    val extract: LibraryExtract? = null,
    val rules: List<LibraryRule> = emptyList(),
)

@Serializable
data class LibraryDownloads(
    val artifact: Artifact? = null,
    val classifiers: Map<String, Artifact> = emptyMap(),
)

@Serializable
data class LibraryExtract(
    val exclude: List<String> = emptyList(),
)

@Serializable
data class LibraryRule(
    val action: String = "",
    val os: LibraryRuleOs? = null,
    val features: Map<String, Boolean> = emptyMap(),
)

@Serializable
data class LibraryRuleOs(
    val name: String? = null,
    val arch: String? = null,
    val version: String? = null,
)

@Serializable
data class Artifact(
    val path: String = "",
    val url: String = "",
    val sha1: String = "",
    val size: Long = 0,
)

@Serializable
data class AssetIndex(
    val objects: Map<String, AssetObject> = emptyMap(),
    val virtual: Boolean = false,
)

@Serializable
data class AssetObject(
    val hash: String = "",
    val size: Long = 0,
)

@Serializable
data class MinecraftManifest(
    val latest: Latest? = null,
    val versions: List<ManifestVersion> = emptyList(),
)

@Serializable
data class Latest(
    val release: String = "",
    val snapshot: String = "",
)

@Serializable
data class ManifestVersion(
    val id: String = "",
    val type: String = "",
    val url: String = "",
    val releaseTime: String = "",
)

@Serializable
data class JavaFeatureRelease(
    val version: JavaVersionData = JavaVersionData(openjdkVersion = "", semver = ""),
    val binary: JavaBinary? = null,
)

@Serializable
data class JavaVersionData(
    val openjdkVersion: String = "",
    val semver: String = "",
)

@Serializable
data class JavaBinary(
    val architecture: String = "",
    val imageType: String = "",
    val os: String = "",
    @SerialName("package")
    val archive: JavaPackage = JavaPackage(link = "", size = 0, name = ""),
)

@Serializable
data class JavaPackage(
    val link: String = "",
    val size: Long = 0,
    val name: String = "",
)
