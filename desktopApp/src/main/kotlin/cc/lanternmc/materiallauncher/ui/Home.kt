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
package cc.lanternmc.materiallauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import cc.lanternmc.materiallauncher.ui.pages.SampleDownloadPage
import cc.lanternmc.materiallauncher.ui.pages.SampleSettings
import cc.lanternmc.materiallauncher.ui.pages.SampleUsersManagement
import cc.lanternmc.materiallauncher.ui.theme.lightScheme

@Composable
fun SampleHome() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("主页")
        }
    }
}

enum class Destination (val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "主页", Icons.Rounded.Home),
    DOWNLOAD("download", "下载", Icons.Rounded.Download),
    USERS("users","用户档案", Icons.Rounded.VerifiedUser),
    SETTINGS("settings", "设置", Icons.Rounded.Settings)
}

@Composable
fun AppNavHost (navController: NavHostController, startDestination: Destination, modifier: Modifier = Modifier) {
    NavHost(navController, startDestination.route){
        Destination.entries.forEach { destination ->
            composable(destination.route) {
                when (destination) {
                    Destination.HOME -> SampleHome()
                    Destination.DOWNLOAD -> SampleDownloadPage()
                    Destination.USERS -> SampleUsersManagement()
                    Destination.SETTINGS -> SampleSettings()
                }
            }
        }
    }
}


@Preview
@Composable
fun Home(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val startDestination = Destination.HOME
    var selectedDestination by rememberSaveable { mutableIntStateOf(startDestination.ordinal) }

    MaterialTheme(lightScheme) {
        Scaffold(modifier = modifier) { contentPadding ->
            Row(modifier = Modifier.padding(contentPadding)) {
                // 左侧导航栏
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(72.dp)  // 典型宽度，可根据需求调整
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Destination.entries.forEachIndexed { index, destination ->
                            NavigationRailItem(
                                selected = selectedDestination == index,
                                onClick = {
                                    navController.navigate(route = destination.route)
                                    selectedDestination = index
                                },
                                icon = {
                                    Icon(destination.icon, contentDescription = "")
                                },
                                label = { Text(destination.label) }
                            )
                        }
                    }
                }

                // 右侧主页面内容
                AppNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}