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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import cc.lanternmc.materiallauncher.generated.resources.Res
import cc.lanternmc.materiallauncher.generated.resources.action_cancel
import cc.lanternmc.materiallauncher.generated.resources.action_close
import cc.lanternmc.materiallauncher.generated.resources.action_save
import cc.lanternmc.materiallauncher.generated.resources.advanced_args_title
import cc.lanternmc.materiallauncher.generated.resources.data_dir_body
import cc.lanternmc.materiallauncher.generated.resources.data_dir_empty
import cc.lanternmc.materiallauncher.generated.resources.data_dir_failed
import cc.lanternmc.materiallauncher.generated.resources.data_dir_label
import cc.lanternmc.materiallauncher.generated.resources.data_dir_saved
import cc.lanternmc.materiallauncher.generated.resources.data_dir_title
import cc.lanternmc.materiallauncher.generated.resources.game_args_label
import cc.lanternmc.materiallauncher.generated.resources.jvm_args_label
import cc.lanternmc.materiallauncher.generated.resources.mirror_auto
import cc.lanternmc.materiallauncher.generated.resources.mirror_body
import cc.lanternmc.materiallauncher.generated.resources.mirror_note
import cc.lanternmc.materiallauncher.generated.resources.mirror_official
import cc.lanternmc.materiallauncher.generated.resources.mirror_only
import cc.lanternmc.materiallauncher.generated.resources.mirror_title
import org.jetbrains.compose.resources.stringResource

/** 高级设置对话框类型（独立于 pages.SettingsDialog，避免改动现有文件）。 */
enum class AdvancedDialog { MIRROR, ADVANCED_ARGS, DATA_DIR }

@Composable
fun AdvancedSettingsDialogs(
    api: LauncherApi,
    dialog: AdvancedDialog?,
    config: DownloadConfig?,
    onDismiss: () -> Unit,
    onApply: (DownloadConfig) -> Unit,
) {
    when (dialog) {
        AdvancedDialog.MIRROR -> MirrorSourceDialog(config, onDismiss, onApply)
        AdvancedDialog.ADVANCED_ARGS -> AdvancedArgsDialog(config, onDismiss, onApply)
        AdvancedDialog.DATA_DIR -> DataDirectoryDialog(api, onDismiss)
        null -> Unit
    }
}

/** 数据目录设置：迁移 config/auth/instances 到自定义目录（重启生效）。 */
@Composable
private fun DataDirectoryDialog(
    api: LauncherApi,
    onDismiss: () -> Unit,
) {
    var current by remember { mutableStateOf(api.getDataDirectory()) }
    var draft by remember { mutableStateOf(current) }
    var message by remember { mutableStateOf<String?>(null) }
    val emptyDir = stringResource(Res.string.data_dir_empty)
    val savedDir = stringResource(Res.string.data_dir_saved)
    val failedDir = stringResource(Res.string.data_dir_failed)
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.data_dir_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(Res.string.data_dir_body),
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it; message = null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(Res.string.data_dir_label)) },
                )
                message?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (draft.isBlank()) {
                    message = emptyDir
                    return@Button
                }
                if (api.setDataDirectory(draft)) {
                    message = savedDir
                } else {
                    message = failedDir
                }
            }) { Text(stringResource(Res.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_close)) } },
    )
}

@Composable
private fun MirrorSourceDialog(
    config: DownloadConfig?,
    onDismiss: () -> Unit,
    onApply: (DownloadConfig) -> Unit,
) {
    var draft by remember { mutableStateOf(config?.mirrorSource ?: "auto") }
    val options = listOf("auto", "mirror", "official")
    val labels = mapOf(
        "auto" to stringResource(Res.string.mirror_auto),
        "mirror" to stringResource(Res.string.mirror_only),
        "official" to stringResource(Res.string.mirror_official),
    )
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.mirror_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(Res.string.mirror_body))
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
                    stringResource(Res.string.mirror_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onApply(config?.copy(mirrorSource = draft) ?: DownloadConfig(mirrorSource = draft))
            }) { Text(stringResource(Res.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}

@Composable
private fun AdvancedArgsDialog(
    config: DownloadConfig?,
    onDismiss: () -> Unit,
    onApply: (DownloadConfig) -> Unit,
) {
    var jvmArgs by remember { mutableStateOf(config?.jvmArgs.orEmpty()) }
    var gameArgs by remember { mutableStateOf(config?.gameArgs.orEmpty()) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.advanced_args_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = jvmArgs,
                    onValueChange = { jvmArgs = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.jvm_args_label)) },
                )
                OutlinedTextField(
                    value = gameArgs,
                    onValueChange = { gameArgs = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.game_args_label)) },
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onApply(
                    config?.copy(jvmArgs = jvmArgs.trim(), gameArgs = gameArgs.trim())
                        ?: DownloadConfig(jvmArgs = jvmArgs.trim(), gameArgs = gameArgs.trim()),
                )
            }) { Text(stringResource(Res.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}

@Composable
private fun Spacer16() {
    androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))
}
