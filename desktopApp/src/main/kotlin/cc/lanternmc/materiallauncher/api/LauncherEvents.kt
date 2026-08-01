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
package cc.lanternmc.materiallauncher.api

/**
 * 后端推送给前端的事件（契约层）。
 */
sealed interface LauncherEvent {
    data class GameReady(val pid: Int) : LauncherEvent
    data class GameExited(val pid: Int) : LauncherEvent

    data class DownloadProgressEvent(val progress: DownloadProgress) : LauncherEvent

    data class JavaIndexStarted(
        val roots: List<String>,
        val workers: Int,
        val cachedCount: Int,
    ) : LauncherEvent

    data class JavaIndexProgress(
        val directoriesScanned: Long,
        val candidatesFound: Int,
        val elapsedMs: Long,
    ) : LauncherEvent

    data class JavaIndexFound(val installation: JavaInstallation) : LauncherEvent

    data class JavaIndexCompleted(
        val installations: List<JavaInstallation>,
        val durationMs: Long,
        val cachePath: String,
    ) : LauncherEvent

    data class JavaIndexError(val message: String) : LauncherEvent
}
