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
package cc.lanternmc.materiallauncher.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import cc.lanternmc.materiallauncher.ui.navigation.Destination
import cc.lanternmc.materiallauncher.ui.navigation.navigateTo
import cc.lanternmc.materiallauncher.ui.theme.lightScheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JavaSettingsPage(navController: NavHostController) {
    val javaList = listOf("17.0", "21.0", "25.0", "1.8.0")    // 临时

    var rangeSliderPosition by remember { mutableStateOf(0f..4096f) }    // 范围内存滑块
    var sliderPosition by remember { mutableFloatStateOf(4096f) }   // 内存滑块
    var Xms = 0
    var Xmx = 0

    var isJavaSelectionExpanded by remember { mutableStateOf(false) }    // 下拉框
    var selectedJava by remember { mutableStateOf(javaList[0]) }

    var doDynamicMemChecked by remember { mutableStateOf(false) }    // 复选框

    MaterialTheme(lightScheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row {
                Scaffold(    // 菜单栏
                    topBar = {
                        TopAppBar(
                            title = {
                                Text("Java 设置")
                            },
                            navigationIcon = {
                                IconButton(onClick = { navController.navigateTo(destination = Destination.SETTINGS) }) {    // 返回按钮
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = ""
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { /* do something */ }) {    // 重置按钮
                                    Icon(
                                        imageVector = Icons.Rounded.History,
                                        contentDescription = ""
                                    )
                                }
                            }
                        )
                    },
                ) { contentPadding ->   // 大括号里是页面内容
                    Box(modifier = Modifier.fillMaxSize().padding(contentPadding).padding(start = 18.dp)) {
                        Column {
                            Text(text = "全局Java版本", modifier = Modifier.padding(4.dp,10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 20.dp)
                            ) {
                                ExposedDropdownMenuBox(
                                    expanded = isJavaSelectionExpanded,
                                    onExpandedChange = { isJavaSelectionExpanded = it }
                                ) {
                                    // 文本框
                                    TextField(
                                        value = selectedJava,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isJavaSelectionExpanded) },
                                        modifier = Modifier.menuAnchor()     // 将文本框与菜单锚定
                                    )

                                    // 下拉菜单
                                    ExposedDropdownMenu(
                                        expanded = isJavaSelectionExpanded,
                                        onDismissRequest = { isJavaSelectionExpanded = false }
                                    ) {
                                        javaList.forEach { java ->
                                            DropdownMenuItem(
                                                text = { Text(java) },
                                                onClick = {
                                                    selectedJava = java
                                                    isJavaSelectionExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                Button(
                                    onClick = {},
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End)
                                    ) {
                                        Icon(imageVector = Icons.Rounded.Refresh, contentDescription = "")
                                        Text("重新扫描")
                                    }
                                }
                            }
                            Text(text = "全局JVM内存分配", modifier = Modifier.padding(start = 4.dp, top = 26.dp, bottom = 4.dp))
                            Column(
                                modifier = Modifier.padding(start = 20.dp, end = 36.dp)
                            ) {
                                if (!doDynamicMemChecked) {      // 根据是否勾选复选框采用两种不同布局
                                    Slider(
                                        value = sliderPosition,
                                        onValueChange = { sliderPosition = it },
                                        valueRange = 256f..16384f
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.End),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(0.dp, 0.dp, 0.dp)
                                    ) {
                                        Xmx = sliderPosition.toInt()
                                        Xms = Xmx
                                        Text(text = "分配的内存: ${Xmx}MB", fontSize = 14.sp)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy((-5).dp, Alignment.End)
                                        ) {
                                            Checkbox(
                                                checked = doDynamicMemChecked,
                                                onCheckedChange = { doDynamicMemChecked = it },
                                                modifier = Modifier.scale(0.9F)
                                            )
                                            Text(
                                                text = "启用动态内存调整",
                                                fontSize = 14.sp,
                                            )
                                        }
                                    }
                                } else {
                                    RangeSlider(
                                        value = rangeSliderPosition,
                                        onValueChange = { range -> rangeSliderPosition = range },
                                        valueRange = 0f..16384f,
                                        onValueChangeFinished = {
                                            // launch some business logic update with the state you hold
                                            // viewModel.updateSelectedSliderValue(sliderPosition)
                                        },
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.End),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(0.dp, 0.dp, 0.dp)
                                    ) {
                                        Xms = rangeSliderPosition.start.toInt()
                                        Xmx = rangeSliderPosition.endInclusive.toInt()
                                        Text(
                                            text =
                                                if (Xms == 0) {     // 如果最小内存设为0则显示不指定
                                                    "最小内存: 不指定      最大内存: ${Xmx}MB"
                                                } else {
                                                    "最小内存: ${Xms}MB      最大内存: ${Xmx}MB"
                                                },
                                            fontSize = 14.sp,
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy((-5).dp, Alignment.End)
                                        ) {
                                            Checkbox(
                                                checked = doDynamicMemChecked,
                                                onCheckedChange = { doDynamicMemChecked = it },
                                                modifier = Modifier.scale(0.9F)
                                            )
                                            Text(
                                                text = "启用动态内存调整",
                                                fontSize = 14.sp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}