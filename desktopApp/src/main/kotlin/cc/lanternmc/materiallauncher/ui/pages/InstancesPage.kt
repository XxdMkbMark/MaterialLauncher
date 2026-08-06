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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
 * 多实例管理页：列出所有实例，支持新建 / 删除 / 启动。
 * 每个实例拥有独立游戏目录与锁定的 MC 版本。
 */
@Composable
fun InstancesPage(
    api: LauncherApi,
    onLaunchInstance: (GameInstance) -> Unit,
    onMessage: (String) -> Unit,
) {
    var instances by remember { mutableStateOf<List<GameInstance>>(emptyList()) }
    var showCreate by remember { mutableStateOf(false) }
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
            Button(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("新建实例")
            }
        }

        if (instances.isEmpty()) {
            Text(
                "还没有实例。点击「新建实例」创建一个独立游戏环境（每个实例有独立存档与设置）。",
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

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        var version by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("新建实例") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("实例名称（如：生存服）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = version,
                        onValueChange = { version = it },
                        label = { Text("MC 版本（如：1.20.1）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isBlank() || version.isBlank()) {
                        onMessage("名称和版本不能为空")
                        return@Button
                    }
                    scope.launch {
                        runCatching { api.createInstance(name.trim(), version.trim()) }
                            .onSuccess {
                                showCreate = false
                                refresh()
                                onMessage("已创建实例 ${it.name}")
                            }
                            .onFailure { onMessage("创建实例失败: ${it.message}") }
                    }
                }) { Text("创建") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("取消") } },
        )
    }
}
