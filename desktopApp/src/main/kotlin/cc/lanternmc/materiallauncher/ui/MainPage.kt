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
package cc.lanternmc.materiallauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import cc.lanternmc.materiallauncher.api.LauncherEvent
import cc.lanternmc.materiallauncher.core.LauncherBackend
import cc.lanternmc.materiallauncher.ui.pages.DownloadPage
import cc.lanternmc.materiallauncher.ui.pages.LaunchPage
import cc.lanternmc.materiallauncher.ui.pages.LauncherSidebar
import cc.lanternmc.materiallauncher.ui.pages.SettingsDialog
import cc.lanternmc.materiallauncher.ui.pages.SidebarCategory
import cc.lanternmc.materiallauncher.ui.pages.SidebarItem
import cc.lanternmc.materiallauncher.ui.theme.LightGreenScheme


@Composable
fun MainPage(backend: LauncherBackend) {
    MaterialTheme(LightGreenScheme) {
        var launchSignal by remember { mutableIntStateOf(0) }
        var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
        var progressTick by remember { mutableIntStateOf(0) }
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }



    }
}
