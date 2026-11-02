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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import cc.lanternmc.materiallauncher.ui.components.SettingsSubPage
import cc.lanternmc.materiallauncher.ui.navigation.Destination
import cc.lanternmc.materiallauncher.ui.navigation.navigateTo
import cc.lanternmc.materiallauncher.generated.resources.Res
import cc.lanternmc.materiallauncher.generated.resources.java_allocated_memory
import cc.lanternmc.materiallauncher.generated.resources.java_global_memory
import cc.lanternmc.materiallauncher.generated.resources.java_global_version
import cc.lanternmc.materiallauncher.generated.resources.java_memory_min_max
import cc.lanternmc.materiallauncher.generated.resources.java_memory_unset_max
import cc.lanternmc.materiallauncher.generated.resources.java_rescan
import cc.lanternmc.materiallauncher.generated.resources.java_set_min_memory
import cc.lanternmc.materiallauncher.generated.resources.settings_java_title
import org.jetbrains.compose.resources.stringResource
import cc.lanternmc.materiallauncher.viewmodel.JavaScannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JavaSettingsPage(navController: NavHostController, viewModel: JavaScannerViewModel) {
    //val javaList = listOf("17.0", "21.0", "25.0", "1.8.0")    // 临时
    val javaList by viewModel.javaList.collectAsState()

    var rangeSliderPosition by remember { mutableStateOf(0f..4096f) }    // 范围内存滑块
    var sliderPosition by remember { mutableFloatStateOf(4096f) }   // 内存滑块
    var Xms: Int
    var Xmx: Int

    var isJavaSelectionExpanded by remember { mutableStateOf(false) }    // 下拉框
    var selectedJava by remember { mutableStateOf(javaList[0]) }

    var doDynamicMemChecked by remember { mutableStateOf(false) }    // 复选框

    SettingsSubPage(
        title = stringResource(Res.string.settings_java_title),
        onBack = { navController.navigateTo(destination = Destination.SETTINGS) },
        // 这一页原来的返回图标没有写死 26.dp，保持默认尺寸。
        backIconSize = null,
    ) { contentPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(contentPadding).padding(start = 18.dp)) {
                        Column {
                            Text(text = stringResource(Res.string.java_global_version), modifier = Modifier.padding(4.dp,10.dp))
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
                                        value = "${selectedJava.javaType} ${selectedJava.version}: ${selectedJava.path}",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isJavaSelectionExpanded) },
                                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    )

                                    // 下拉菜单
                                    ExposedDropdownMenu(
                                        expanded = isJavaSelectionExpanded,
                                        onDismissRequest = { isJavaSelectionExpanded = false }
                                    ) {
                                        javaList.forEach { java ->
                                            DropdownMenuItem(
                                                text = { Text("${java.javaType} ${java.version}: ${java.path}") },
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
                                        Text(stringResource(Res.string.java_rescan))
                                    }
                                }
                            }
                            Text(text = stringResource(Res.string.java_global_memory), modifier = Modifier.padding(start = 4.dp, top = 26.dp, bottom = 4.dp))
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
                                        // 英文 “Allocated memory / Set minimum memory” 比中文长，铺满宽度后让数字说明让出位置。
                                        modifier = Modifier.fillMaxWidth().padding(0.dp, 0.dp, 0.dp)
                                    ) {
                                        Xmx = sliderPosition.toInt()
                                        Xms = Xmx
                                        Text(
                                            text = stringResource(Res.string.java_allocated_memory, Xmx.toString()),
                                            fontSize = 14.sp,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy((-5).dp, Alignment.End),
                                            modifier = Modifier.padding(end = 10.dp)
                                        ) {
                                            Checkbox(
                                                checked = doDynamicMemChecked,
                                                onCheckedChange = { doDynamicMemChecked = it },
                                                modifier = Modifier.scale(0.9F)
                                            )
                                            Text(
                                                text = stringResource(Res.string.java_set_min_memory),
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
                                        // 英文最小/最大内存说明更长，不限宽会把右侧复选框挤出窗口。
                                        modifier = Modifier.fillMaxWidth().padding(0.dp, 0.dp, 0.dp)
                                    ) {
                                        Xms = rangeSliderPosition.start.toInt()
                                        Xmx = rangeSliderPosition.endInclusive.toInt()
                                        val unsetMemory = stringResource(Res.string.java_memory_unset_max, Xmx.toString())
                                        val rangedMemory = stringResource(Res.string.java_memory_min_max, Xms.toString(), Xmx.toString())
                                        Text(
                                            text =
                                                if (Xms == 0) {     // 如果最小内存设为0则显示不指定
                                                    unsetMemory
                                                } else {
                                                    rangedMemory
                                                },
                                            fontSize = 14.sp,
                                            modifier = Modifier.weight(1f),
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
                                                text = stringResource(Res.string.java_set_min_memory),
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