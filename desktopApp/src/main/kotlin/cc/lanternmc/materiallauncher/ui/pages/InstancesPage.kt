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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import cc.lanternmc.materiallauncher.api.GameInstance
import cc.lanternmc.materiallauncher.api.LauncherApi

/**
 * 实例管理页：列出所有已下载的命名实例，支持启动 / 删除。
 *
 * 实例与版本文件夹合一：在下载页下载 MC 时输入名称即创建实例
 * （默认用版本号命名），这里统一管理。
 */
@Composable
fun InstancesPage(
    api: LauncherApi,
    onLaunchInstance: (GameInstance) -> Unit,
    onMessage: (String) -> Unit,
) {
    var instances by remember { mutableStateOf<List<GameInstance>>(emptyList()) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            instances = runCatching { api.listInstances() }.getOrDefault(emptyList())
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("游戏实例", style = MaterialTheme.typography.headlineMedium)
        }

        Text(
            "实例与版本文件夹合一：在「下载」页下载 Minecraft 时输入名称（默认版本号）即创建实例，每个实例拥有独立存档与设置。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )

        if (instances.isEmpty()) {
            Text(
                "还没有实例。去「下载」页选择一个版本下载，输入名称即可创建。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(instances, key = { it.id }) { instance ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(instance.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "版本 ${instance.versionId}  ·  内存 ${instance.maxMemory}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                instance.gameDir,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { onLaunchInstance(instance) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Text("启动")
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    val ok = api.deleteInstance(instance.id)
                                    if (ok) {
                                        refresh()
                                        onMessage("已删除实例 ${instance.name}")
                                    } else {
                                        onMessage("删除实例失败")
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除实例")
                            }
                        }
                    }
                }
            }
        }
    }
}
