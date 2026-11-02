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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
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
import cc.lanternmc.materiallauncher.generated.resources.Res
import cc.lanternmc.materiallauncher.generated.resources.action_cancel
import cc.lanternmc.materiallauncher.generated.resources.action_launch
import cc.lanternmc.materiallauncher.generated.resources.action_save
import cc.lanternmc.materiallauncher.generated.resources.cd_delete_instance
import cc.lanternmc.materiallauncher.generated.resources.cd_instance_settings
import cc.lanternmc.materiallauncher.generated.resources.field_extra_jvm
import cc.lanternmc.materiallauncher.generated.resources.field_game_version
import cc.lanternmc.materiallauncher.generated.resources.field_java_path
import cc.lanternmc.materiallauncher.generated.resources.field_max_memory
import cc.lanternmc.materiallauncher.generated.resources.field_name
import cc.lanternmc.materiallauncher.generated.resources.instance_delete_failed
import cc.lanternmc.materiallauncher.generated.resources.instance_deleted
import cc.lanternmc.materiallauncher.generated.resources.instance_game_dir
import cc.lanternmc.materiallauncher.generated.resources.instance_meta
import cc.lanternmc.materiallauncher.generated.resources.instance_save_failed
import cc.lanternmc.materiallauncher.generated.resources.instance_saved
import cc.lanternmc.materiallauncher.generated.resources.instance_settings_title
import cc.lanternmc.materiallauncher.generated.resources.instances_empty
import cc.lanternmc.materiallauncher.generated.resources.instances_intro
import cc.lanternmc.materiallauncher.generated.resources.instances_title
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

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
    var editing by remember { mutableStateOf<GameInstance?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            instances = runCatching { api.listInstances() }.getOrDefault(emptyList())
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.instances_title), style = MaterialTheme.typography.headlineMedium)
        }

        Text(
            stringResource(Res.string.instances_intro),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )

        if (instances.isEmpty()) {
            Text(
                stringResource(Res.string.instances_empty),
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
                                stringResource(Res.string.instance_meta, instance.versionId, instance.maxMemory),
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
                                Text(stringResource(Res.string.action_launch))
                            }
                            IconButton(onClick = { editing = instance }) {
                                Icon(Icons.Default.Settings, contentDescription = stringResource(Res.string.cd_instance_settings))
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    val ok = api.deleteInstance(instance.id)
                                    if (ok) {
                                        refresh()
                                        onMessage(getString(Res.string.instance_deleted, instance.name))
                                    } else {
                                        onMessage(getString(Res.string.instance_delete_failed))
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.cd_delete_instance))
                            }
                        }
                    }
                }
            }
        }
    }

    // 编辑实例设置对话框：每个实例独立管理版本 / Java / 内存 / JVM 参数
    editing?.let { instance ->
        var name by remember(instance.id) { mutableStateOf(instance.name) }
        var versionId by remember(instance.id) { mutableStateOf(instance.versionId) }
        var javaPath by remember(instance.id) { mutableStateOf(instance.javaPath) }
        var maxMemory by remember(instance.id) { mutableStateOf(instance.maxMemory) }
        var jvmArgs by remember(instance.id) { mutableStateOf(instance.jvmArgs) }

        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(stringResource(Res.string.instance_settings_title, instance.name)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(Res.string.field_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = versionId,
                        onValueChange = { versionId = it },
                        label = { Text(stringResource(Res.string.field_game_version)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = javaPath,
                        onValueChange = { javaPath = it },
                        label = { Text(stringResource(Res.string.field_java_path)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = maxMemory,
                        onValueChange = { maxMemory = it },
                        label = { Text(stringResource(Res.string.field_max_memory)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = jvmArgs,
                        onValueChange = { jvmArgs = it },
                        label = { Text(stringResource(Res.string.field_extra_jvm)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        stringResource(Res.string.instance_game_dir, instance.gameDir),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        runCatching {
                            api.saveInstance(
                                instance.copy(
                                    name = name.trim().ifBlank { instance.name },
                                    versionId = versionId.trim(),
                                    javaPath = javaPath.trim(),
                                    maxMemory = maxMemory.trim().ifBlank { "2048M" },
                                    jvmArgs = jvmArgs.trim(),
                                ),
                            )
                        }.onSuccess {
                            editing = null
                            refresh()
                            onMessage(getString(Res.string.instance_saved, instance.name))
                        }.onFailure { e ->
                            onMessage(getString(Res.string.instance_save_failed, e.message.orEmpty()))
                        }
                    }
                }) {
                    Text(stringResource(Res.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }
}
