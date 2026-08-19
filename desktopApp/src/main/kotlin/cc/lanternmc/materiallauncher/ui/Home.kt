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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import cc.lanternmc.materiallauncher.ui.components.UnderConstructionDialog
import cc.lanternmc.materiallauncher.ui.navigation.AppNavHost
import cc.lanternmc.materiallauncher.ui.navigation.Destination
import cc.lanternmc.materiallauncher.ui.navigation.navigateTo
import cc.lanternmc.materiallauncher.ui.theme.lightScheme
import cc.lanternmc.materiallauncher.viewmodel.JavaScannerViewModel

@Composable
fun HomePage(navController: NavHostController) {
    var showDialog by remember { mutableStateOf(false) }
    MaterialTheme(lightScheme) {
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

@Preview
@Composable
fun Home(modifier: Modifier = Modifier) {
    val viewModel = remember { JavaScannerViewModel() }
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
                                    navController.navigateTo(Destination.HOME) {
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

                            Destination.entries.forEachIndexed { index, destination ->
                                if (!destination.showInNavigationRail) return@forEachIndexed
                                NavigationRailItem(
                                    selected = selectedDestination == index,
                                    onClick = {
                                        navController.navigateTo(destination)
                                        selectedDestination = index
                                    },
                                    icon = {
                                        Icon(destination.icon, contentDescription = "")
                                    },
                                    label = { Text(text = destination.label, textAlign = TextAlign.Center) }
                                )
                            }


                        }

                    }
                }

                // 右侧主页面内容
                AppNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}