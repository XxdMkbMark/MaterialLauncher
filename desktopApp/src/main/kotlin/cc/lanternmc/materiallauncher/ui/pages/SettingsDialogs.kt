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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import cc.lanternmc.materiallauncher.api.DownloadConfig
import cc.lanternmc.materiallauncher.api.JavaInstallation
import cc.lanternmc.materiallauncher.api.LauncherApi
import cc.lanternmc.materiallauncher.ui.components.DropdownField

enum class SettingsDialog { MC_PATH, MC_VERSION, JAVA, MEM, USERNAME }

@Composable
fun SettingsDialogs(
    api: LauncherApi,
    dialog: SettingsDialog?,
    onDismiss: () -> Unit,
    config: DownloadConfig?,
    mcVersions: List<String>,
    selectedMcVersion: String,
    onSelectedMcVersionChange: (String) -> Unit,
    onRefreshMcVersions: () -> Unit,
    javas: List<JavaInstallation>,
    selectedJava: String,
    onSelectedJavaChange: (String) -> Unit,
    onRescanJava: () -> Unit,
    memValue: Int,
    onMemValueChange: (Int) -> Unit,
    onApplyMcPath: (source: String, path: String) -> Unit,
    username: String,
    onApplyUsername: (String) -> Unit,
) {
    val mcSourceDraft = remember { mutableStateOf("default") }
    val customPath = remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // 每次打开 MC_PATH 对话框时重置草稿
    LaunchedEffect(dialog) {
        if (dialog == SettingsDialog.MC_PATH) {
            mcSourceDraft.value = config?.minecraft?.source ?: "default"
            customPath.value = config?.minecraft?.path.orEmpty()
        }
    }

    if (dialog == SettingsDialog.MC_PATH) {
        Dialog(onDismissRequest = onDismiss) {
            SettingsCard(title = "MC 路径设置") {
                DropdownField(
                    label = "安装位置",
                    display = when (mcSourceDraft.value) {
                        "default" -> "默认文件夹"
                        "launcher" -> "启动器文件夹"
                        else -> "自定义"
                    },
                    options = listOf("default", "launcher", "custom"),
                    getLabel = { source ->
                        when (source) {
                            "default" -> "默认文件夹"
                            "launcher" -> "启动器文件夹"
                            else -> "自定义"
                        }
                    },
                    onSelect = { mcSourceDraft.value = it },
                )
                if (mcSourceDraft.value == "custom") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.OutlinedTextField(
                            value = customPath.value,
                            onValueChange = { customPath.value = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        Button(onClick = {
                            scope.launch {
                                val selected = api.openDirectoryDialog("选择 Minecraft 文件夹")
                                if (!selected.isNullOrBlank()) customPath.value = selected
                            }
                        }) { Text("…") }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(onClick = {
                        onApplyMcPath(mcSourceDraft.value, customPath.value)
                    }) { Text("应用") }
                }
            }
        }
    }

    if (dialog == SettingsDialog.MC_VERSION) {
        Dialog(onDismissRequest = onDismiss) {
            SettingsCard(title = "MC 版本选择") {
                DropdownField(
                    label = "版本",
                    display = selectedMcVersion,
                    options = mcVersions,
                    onSelect = onSelectedMcVersionChange,
                )
                TextButton(onClick = onRefreshMcVersions) { Text("刷新列表") }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
            }
        }
    }

    if (dialog == SettingsDialog.JAVA) {
        Dialog(onDismissRequest = onDismiss) {
            SettingsCard(title = "Java 设置") {
                JavaSelectField(
                    javas = javas,
                    selectedPath = selectedJava,
                    onSelect = onSelectedJavaChange,
                )
                TextButton(onClick = onRescanJava) { Text("重新查找") }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
            }
        }
    }

    if (dialog == SettingsDialog.USERNAME) {
        Dialog(onDismissRequest = onDismiss) {
            SettingsCard(title = "用户名设置") {
                var draft by remember { mutableStateOf(username) }
                androidx.compose.material3.OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("游戏内昵称") },
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(onClick = { onApplyUsername(draft.trim()) }) { Text("应用") }
                }
            }
        }
    }

    if (dialog == SettingsDialog.MEM) {
        Dialog(onDismissRequest = onDismiss) {
            SettingsCard(title = "内存分配") {
                Slider(
                    value = memValue.toFloat(),
                    onValueChange = { onMemValueChange(it.toInt()) },
                    valueRange = 128f..16384f,
                    steps = 126,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("128 MB", style = MaterialTheme.typography.labelSmall)
                    Text(formatMem(memValue), style = MaterialTheme.typography.labelMedium)
                    Text("16 GB", style = MaterialTheme.typography.labelSmall)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.width(420.dp).padding(24.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Column(Modifier.fillMaxWidth().padding(top = 12.dp)) { content() }
        }
    }
}

@Composable
private fun JavaSelectField(
    javas: List<JavaInstallation>,
    selectedPath: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "选择 Java",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = javas.firstOrNull { it.path == selectedPath }?.let { "${it.version} - ${it.path}" }
                        ?: if (selectedPath.isBlank()) "未选择" else selectedPath,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.widthIn(max = 380.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large,
            ) {
                javas.forEach { java ->
                    DropdownMenuItem(
                        text = { Text("${java.version} - ${java.path}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = {
                            onSelect(java.path)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

fun formatMem(memBytes: Int): String =
    if (memBytes <= 1024) "$memBytes MB" else "${(memBytes / 1024.0).let { String.format("%.1f", it) }} GB"
