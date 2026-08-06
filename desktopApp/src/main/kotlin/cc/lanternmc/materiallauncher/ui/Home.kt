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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import cc.lanternmc.materiallauncher.ui.components.UnderConstructionDialog
import cc.lanternmc.materiallauncher.ui.pages.SampleDownloadPage
import cc.lanternmc.materiallauncher.ui.pages.SampleSettings
import cc.lanternmc.materiallauncher.ui.pages.SampleUsersManagement
import cc.lanternmc.materiallauncher.ui.pages.SampleVersionsManagement
import cc.lanternmc.materiallauncher.ui.theme.backgroundLight
import cc.lanternmc.materiallauncher.ui.theme.lightScheme
import kotlinx.coroutines.coroutineScope

@Composable
fun SampleHome() {
    var showDialog by remember { mutableStateOf(false) }
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.size(400.dp, 200.dp).background(lightScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("UNDER CONSTRUCTION", Modifier.padding(16.dp), fontSize = 20.sp)
                    Button(onClick = { showDialog = true }) {
                        Text("这是什么?")
                    }
                }
            }
        }
    }
    if (showDialog) {
        UnderConstructionDialog(
            onConfirm = { showDialog = false },
        )
    }
}

enum class Destination (val route: String, val label: String, val icon: ImageVector,val showInNavigationRail: Boolean = true) {
    HOME("home", "主页", Icons.Rounded.Home, false),
    DOWNLOAD("download", "下载", Icons.Rounded.Download),
    VERSIONS("versions","版本", Icons.Rounded.Checklist),
    SETTINGS("settings", "设置", Icons.Rounded.Settings),
    USERS("users", "用户档案", Icons.Rounded.ManageAccounts),
}

@Composable
fun AppNavHost (navController: NavHostController, startDestination: Destination, modifier: Modifier = Modifier) {
    NavHost(navController, startDestination.route){
        Destination.entries.forEach { destination ->
            composable(destination.route) {
                when (destination) {
                    Destination.HOME -> SampleHome()
                    Destination.DOWNLOAD -> SampleDownloadPage()
                    Destination.VERSIONS -> SampleVersionsManagement()
                    Destination.SETTINGS -> SampleSettings()
                    Destination.USERS -> SampleUsersManagement()
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

    val navigationDestinations = Destination.entries.filter {
        it.showInNavigationRail
    }

    // 抽屉状态管理
    // val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    // val coroutineScope = rememberCoroutineScope()

    MaterialTheme(lightScheme) {
        Scaffold(modifier = modifier) { contentPadding ->
            Row(modifier = Modifier.padding(contentPadding)) {
                // 左侧导航栏
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(72.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            NavigationRailItem(
                                selected = false,
                                onClick = {
                                    // coroutineScope.launch { drawerState.open() }
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Menu,
                                        contentDescription = "展开",
                                    )
                                },
                            )

                            FloatingActionButton(
                                onClick = {
                                    navController.navigate(Destination.HOME.route) {
                                        launchSingleTop = true
                                    }
                                },
                                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp, focusedElevation = 0.dp, hoveredElevation = 0.dp),
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Home,
                                    contentDescription = "主页",
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f).padding(20.dp),  // 占据剩余空间
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            /*
                            navigationDestinations.forEach { destination ->
                                val currentRoute = null
                                NavigationRailItem(
                                    selected = currentRoute == destination.route,
                                    onClick = {
                                        navController.navigate(destination.route) {
                                            launchSingleTop = true

                                            popUpTo(
                                                navController.graph.startDestinationId
                                            ) {
                                                saveState = true
                                            }

                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = destination.icon,
                                            contentDescription = destination.label,
                                        )
                                    },
                                    label = {
                                        Text(destination.label)
                                    },
                                )
                            }

                             */
                            Destination.entries.forEachIndexed { index, destination ->
                                if (!destination.showInNavigationRail) return@forEachIndexed
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