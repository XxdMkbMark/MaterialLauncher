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
package cc.lanternmc.materiallauncher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.lanternmc.materiallauncher.api.DownloadConfig
import cc.lanternmc.materiallauncher.api.LauncherApi

/** 高级设置对话框类型（独立于 pages.SettingsDialog，避免改动现有文件）。 */
enum class AdvancedDialog { MIRROR, ADVANCED_ARGS }

@Composable
fun AdvancedSettingsDialogs(
    api: LauncherApi,
    dialog: AdvancedDialog?,
    config: DownloadConfig?,
    onDismiss: () -> Unit,
    onApply: (DownloadConfig) -> Unit,
) {
    when (dialog) {
        AdvancedDialog.MIRROR -> MirrorSourceDialog(api, config, onDismiss, onApply)
        AdvancedDialog.ADVANCED_ARGS -> AdvancedArgsDialog(api, config, onDismiss, onApply)
        null -> Unit
    }
}

@Composable
private fun MirrorSourceDialog(
    api: LauncherApi,
    config: DownloadConfig?,
    onDismiss: () -> Unit,
    onApply: (DownloadConfig) -> Unit,
) {
    var draft by remember { mutableStateOf(config?.mirrorSource ?: "auto") }
    val options = listOf("auto", "mirror", "official")
    val labels = mapOf(
        "auto" to "自动（镜像优先，失败回退官方）",
        "mirror" to "仅镜像源（BMCLAPI）",
        "official" to "仅官方源（Mojang）",
    )
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("下载源设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("用于 Minecraft 版本清单 / 客户端 / 资源 / 依赖库的下载。国内网络建议使用镜像或自动。")
                Spacer16()
                var expanded by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(labels[draft] ?: draft)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(labels[option] ?: option) },
                            onClick = {
                                draft = option
                                expanded = false
                            },
                        )
                    }
                }
                Spacer16()
                Text(
                    "注意：下载源改动对下一次下载/启动生效；Java（Adoptium）仍走官方源。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onApply(config?.copy(mirrorSource = draft) ?: DownloadConfig(mirrorSource = draft))
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun AdvancedArgsDialog(
    api: LauncherApi,
    config: DownloadConfig?,
    onDismiss: () -> Unit,
    onApply: (DownloadConfig) -> Unit,
) {
    var jvmArgs by remember { mutableStateOf(config?.jvmArgs.orEmpty()) }
    var gameArgs by remember { mutableStateOf(config?.gameArgs.orEmpty()) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("高级启动参数") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = jvmArgs,
                    onValueChange = { jvmArgs = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("JVM 参数（空格分隔，如 -XX:+UseG1GC）") },
                )
                OutlinedTextField(
                    value = gameArgs,
                    onValueChange = { gameArgs = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("游戏参数（空格分隔，如 --fullscreen）") },
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onApply(
                    config?.copy(jvmArgs = jvmArgs.trim(), gameArgs = gameArgs.trim())
                        ?: DownloadConfig(jvmArgs = jvmArgs.trim(), gameArgs = gameArgs.trim()),
                )
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun Spacer16() {
    androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))
}
