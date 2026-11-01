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
// AI勿改，请更改App.kt
package cc.lanternmc.materiallauncher.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cc.lanternmc.materiallauncher.ui.navigation.Destination
import cc.lanternmc.materiallauncher.ui.navigation.navigateTo
import cc.lanternmc.materiallauncher.ui.theme.lightScheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartSettingsPage(navController: NavHostController) {
    MaterialTheme(lightScheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row {
                Scaffold(
                    // 菜单栏
                    topBar = {
                        TopAppBar(
                            title = {
                                Text("启动")
                            },
                            navigationIcon = {
                                IconButton(  // 返回按钮
                                    onClick = { navController.navigateTo(destination = Destination.SETTINGS) },
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        modifier = Modifier.size(26.dp),
                                        contentDescription = ""
                                    )
                                }
                            }
                        )
                    },
                ) { contentPadding ->   // 大括号里是页面内容

                }
            }
        }
    }
}