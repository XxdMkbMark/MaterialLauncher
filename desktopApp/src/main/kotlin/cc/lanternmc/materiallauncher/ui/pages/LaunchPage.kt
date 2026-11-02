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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import cc.lanternmc.materiallauncher.api.Account
import cc.lanternmc.materiallauncher.api.DownloadConfig
import cc.lanternmc.materiallauncher.api.JavaInstallation
import cc.lanternmc.materiallauncher.api.LaunchRequest
import cc.lanternmc.materiallauncher.api.LauncherApi
import cc.lanternmc.materiallauncher.api.LauncherEvent
import cc.lanternmc.materiallauncher.generated.resources.Res
import cc.lanternmc.materiallauncher.generated.resources.account_offline
import cc.lanternmc.materiallauncher.generated.resources.account_online
import cc.lanternmc.materiallauncher.generated.resources.account_value
import cc.lanternmc.materiallauncher.generated.resources.app_name
import cc.lanternmc.materiallauncher.generated.resources.launch_game_exited
import cc.lanternmc.materiallauncher.generated.resources.launch_game_ready
import cc.lanternmc.materiallauncher.generated.resources.launch_index_busy
import cc.lanternmc.materiallauncher.generated.resources.launch_indexing
import cc.lanternmc.materiallauncher.generated.resources.launch_java_index_done
import cc.lanternmc.materiallauncher.generated.resources.launch_java_index_failed
import cc.lanternmc.materiallauncher.generated.resources.launch_need_selection
import cc.lanternmc.materiallauncher.generated.resources.launch_starting
import cc.lanternmc.materiallauncher.generated.resources.launch_starting_bg
import cc.lanternmc.materiallauncher.generated.resources.not_selected
import cc.lanternmc.materiallauncher.generated.resources.not_set
import cc.lanternmc.materiallauncher.generated.resources.sidebar_account
import cc.lanternmc.materiallauncher.generated.resources.sidebar_java
import cc.lanternmc.materiallauncher.generated.resources.sidebar_launch_mc
import cc.lanternmc.materiallauncher.generated.resources.sidebar_mc_path
import cc.lanternmc.materiallauncher.generated.resources.sidebar_mc_version
import cc.lanternmc.materiallauncher.generated.resources.sidebar_memory
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun LaunchPage(
    api: LauncherApi,
    events: Flow<LauncherEvent>,
    launchSignal: Int,
    dialog: SettingsDialog?,
    selectedAccount: Account?,
    onOpenAccountDialog: () -> Unit,
    onOpenSettingsDialog: (SettingsDialog?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var config by remember { mutableStateOf<DownloadConfig?>(null) }
    val javas = remember { mutableStateListOf<JavaInstallation>() }
    val mcVersions = remember { mutableStateListOf<String>() }
    var selectedJava by remember { mutableStateOf("") }
    var selectedMcVersion by remember { mutableStateOf("") }
    var memValue by remember { mutableIntStateOf(512) }
    var status by remember { mutableStateOf("") }
    val needSelection = stringResource(Res.string.launch_need_selection)
    val starting = stringResource(Res.string.launch_starting)
    val startingBg = stringResource(Res.string.launch_starting_bg)
    val gameReady = stringResource(Res.string.launch_game_ready)
    val gameExited = stringResource(Res.string.launch_game_exited)
    val indexing = stringResource(Res.string.launch_indexing)
    val indexBusy = stringResource(Res.string.launch_index_busy)
    val notSet = stringResource(Res.string.not_set)
    val notSelected = stringResource(Res.string.not_selected)
    val onlineLabel = stringResource(Res.string.account_online)
    val offlineLabel = stringResource(Res.string.account_offline)

    fun refreshMcVersions() {
        scope.launch {
            val path = config?.minecraft?.path.orEmpty()
            if (path.isBlank()) return@launch
            val versions = api.getInstalledMinecraftVersions(path)
            mcVersions.clear()
            mcVersions.addAll(versions)
            selectedMcVersion = versions.firstOrNull() ?: ""
        }
    }

    fun refreshJavas() {
        scope.launch {
            val found = api.findJavaPaths()
            javas.clear()
            javas.addAll(found)
            if (found.isNotEmpty() && selectedJava.isBlank()) selectedJava = found.first().path
        }
    }

    fun updateConfig(partial: DownloadConfig) {
        config = partial
        scope.launch { api.saveDownloadConfig(partial) }
    }

    val doLaunch: () -> Unit = {
        if (selectedJava.isBlank() || config?.minecraft?.path.isNullOrBlank() || selectedMcVersion.isBlank()) {
            status = needSelection
        } else {
            status = starting
            // 异步启动：完整流程（下载/校验/拉起）在启动器自己的协程中执行，
            // 不会因 UI 组合离开而被取消。
            api.launchMinecraftAsync(
                LaunchRequest(
                    javaPath = selectedJava,
                    gameDir = config?.minecraft?.path.orEmpty(),
                    versionId = selectedMcVersion,
                    username = selectedAccount?.username
                        ?: config?.username.orEmpty().ifBlank { "TestUser" },
                    accessToken = selectedAccount?.accessToken ?: "0",
                    uuid = selectedAccount?.uuid
                        ?: "00000000-0000-0000-0000-000000000000",
                    userType = selectedAccount?.userType ?: "legacy",
                    maxMemory = "${memValue}M",
                    isolateVersion = true,
                ),
            )
            status = startingBg
        }
    }

    // 初始化：加载配置、已安装版本、Java 列表
    LaunchedEffect(Unit) {
        try {
            val cfg = api.getDownloadConfig()
            config = cfg
            if (cfg.minecraft.path.isNotBlank()) {
                val versions = api.getInstalledMinecraftVersions(cfg.minecraft.path)
                mcVersions.clear()
                mcVersions.addAll(versions)
                if (versions.isNotEmpty()) selectedMcVersion = versions.first()
            }
        } catch (e: Exception) {
            api.logError("加载配置失败: $e")
        }
        try {
            val found = api.findJavaPaths()
            javas.clear()
            javas.addAll(found)
            if (found.isNotEmpty()) selectedJava = found.first().path
        } catch (e: Exception) {
            api.logError("查找 Java 失败: $e")
        }
    }

    // 游戏状态事件
    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is LauncherEvent.GameReady -> status = gameReady
                is LauncherEvent.GameExited -> status = gameExited
                is LauncherEvent.JavaIndexFound -> {
                    if (javas.none { it.path == event.installation.path }) {
                        javas.add(event.installation)
                    }
                }
                is LauncherEvent.JavaIndexCompleted -> {
                    refreshJavas()
                    status = getString(Res.string.launch_java_index_done, event.installations.size.toString())
                    onOpenSettingsDialog(null)
                }
                is LauncherEvent.JavaIndexError -> status = getString(Res.string.launch_java_index_failed, event.message)
                else -> Unit
            }
        }
    }

    // 侧边栏点击"启动"触发
    LaunchedEffect(launchSignal) {
        if (launchSignal > 0) doLaunch()
    }

    val memLabel = formatMem(memValue)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Button(
                onClick = doLaunch,
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(100),
            ) {
                Text(stringResource(Res.string.sidebar_launch_mc), fontSize = 16.sp)
            }
            Text(
                text = status,
                // 英文状态句比中文长，占满按钮右侧剩余宽度并允许换行，避免挤出固定窗口。
                modifier = Modifier.weight(1f),
                color = Color(0xFF44C835),
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SettingCard(stringResource(Res.string.sidebar_mc_path), config?.minecraft?.path ?: notSet, Modifier.weight(1f)) { onOpenSettingsDialog(SettingsDialog.MC_PATH) }
                SettingCard(stringResource(Res.string.sidebar_mc_version), selectedMcVersion.ifBlank { notSelected }, Modifier.weight(1f)) { onOpenSettingsDialog(SettingsDialog.MC_VERSION) }
            }
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SettingCard(stringResource(Res.string.sidebar_java), selectedJava.ifBlank { notSelected }, Modifier.weight(1f)) { onOpenSettingsDialog(SettingsDialog.JAVA) }
                SettingCard(stringResource(Res.string.sidebar_memory), memLabel, Modifier.weight(1f)) { onOpenSettingsDialog(SettingsDialog.MEM) }
            }
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SettingCard(
                    label = stringResource(Res.string.sidebar_account),
                    value = selectedAccount?.let { acc ->
                        stringResource(
                            Res.string.account_value,
                            acc.username,
                            if (acc.type == "online") onlineLabel else offlineLabel,
                        )
                    } ?: config?.username?.ifBlank { "TestUser" } ?: "TestUser",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenAccountDialog,
                )
            }
        }
    }

    SettingsDialogs(
        api = api,
        dialog = dialog,
        onDismiss = { onOpenSettingsDialog(null) },
        config = config,
        mcVersions = mcVersions,
        selectedMcVersion = selectedMcVersion,
        onSelectedMcVersionChange = { selectedMcVersion = it },
        onRefreshMcVersions = { refreshMcVersions() },
        javas = javas,
        selectedJava = selectedJava,
        onSelectedJavaChange = { selectedJava = it },
        onRescanJava = {
            status = indexing
            val started = api.refreshJavaIndex()
            if (!started) status = indexBusy
        },
        memValue = memValue,
        onMemValueChange = { memValue = it },
        onApplyMcPath = { source, path ->
            scope.launch {
                val resolved = when (source) {
                    "default" -> api.getDefaultMinecraftDir()
                    "launcher" -> api.getLauncherMinecraftDir()
                    else -> path
                }
                val cfg = config ?: api.getDownloadConfig()
                val updated = cfg.copy(
                    minecraft = cfg.minecraft.copy(path = resolved, source = source),
                )
                updateConfig(updated)
                refreshMcVersions()
                onOpenSettingsDialog(null)
            }
        },
        username = config?.username.orEmpty().ifBlank { "TestUser" },
        onApplyUsername = { newUsername ->
            scope.launch {
                val cfg = config ?: api.getDownloadConfig()
                val updated = cfg.copy(username = newUsername.ifBlank { "TestUser" })
                updateConfig(updated)
                onOpenSettingsDialog(null)
            }
        },
    )
}

@Composable
private fun SettingCard(label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
