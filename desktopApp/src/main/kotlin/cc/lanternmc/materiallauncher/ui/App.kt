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
package cc.lanternmc.materiallauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.lanternmc.materiallauncher.api.Account
import cc.lanternmc.materiallauncher.api.DownloadConfig
import cc.lanternmc.materiallauncher.api.LauncherEvent
import cc.lanternmc.materiallauncher.core.LauncherBackend
import cc.lanternmc.materiallauncher.generated.resources.Res
import cc.lanternmc.materiallauncher.generated.resources.sidebar_account
import cc.lanternmc.materiallauncher.generated.resources.sidebar_advanced
import cc.lanternmc.materiallauncher.generated.resources.sidebar_data_dir
import cc.lanternmc.materiallauncher.generated.resources.sidebar_download
import cc.lanternmc.materiallauncher.generated.resources.sidebar_instances
import cc.lanternmc.materiallauncher.generated.resources.sidebar_java
import cc.lanternmc.materiallauncher.generated.resources.sidebar_java_download
import cc.lanternmc.materiallauncher.generated.resources.sidebar_launch
import cc.lanternmc.materiallauncher.generated.resources.sidebar_launch_mc
import cc.lanternmc.materiallauncher.generated.resources.sidebar_log_panel
import cc.lanternmc.materiallauncher.generated.resources.sidebar_logs
import cc.lanternmc.materiallauncher.generated.resources.sidebar_mc_download
import cc.lanternmc.materiallauncher.generated.resources.sidebar_mc_path
import cc.lanternmc.materiallauncher.generated.resources.sidebar_mc_version
import cc.lanternmc.materiallauncher.generated.resources.sidebar_memory
import cc.lanternmc.materiallauncher.generated.resources.sidebar_mirror
import cc.lanternmc.materiallauncher.generated.resources.sidebar_multi_instance
import cc.lanternmc.materiallauncher.generated.resources.sidebar_settings
import cc.lanternmc.materiallauncher.generated.resources.sidebar_username
import cc.lanternmc.materiallauncher.generated.resources.snack_instance_launching
import cc.lanternmc.materiallauncher.generated.resources.snack_java_index_done
import cc.lanternmc.materiallauncher.generated.resources.snack_java_index_failed
import cc.lanternmc.materiallauncher.generated.resources.snack_login_failed
import cc.lanternmc.materiallauncher.generated.resources.snack_login_ok
import cc.lanternmc.materiallauncher.generated.resources.snack_settings_saved
import cc.lanternmc.materiallauncher.ui.components.AdvancedDialog
import cc.lanternmc.materiallauncher.ui.components.AdvancedSettingsDialogs
import cc.lanternmc.materiallauncher.ui.pages.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

private enum class Page { LAUNCH, DOWNLOAD, INSTANCES, LOGS }

@Composable
fun App(backend: LauncherBackend) {
    MaterialTheme(lightColorScheme()) {
        var page by remember { mutableStateOf(Page.LAUNCH) }
        var launchSignal by remember { mutableIntStateOf(0) }
        var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
        var advancedDialog by remember { mutableStateOf<AdvancedDialog?>(null) }
        var progressTick by remember { mutableIntStateOf(0) }
        var selectedAccount by remember { mutableStateOf<Account?>(null) }
        var config by remember { mutableStateOf<DownloadConfig?>(null) }
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        fun showMessage(message: String) {
            scope.launch {
                snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            }
        }

        // 加载保存的账户选择与账户列表
        LaunchedEffect(Unit) {
            val cfg = runCatching { backend.getDownloadConfig() }.getOrNull()
            config = cfg
            val accounts = runCatching { backend.getAccounts() }.getOrDefault(emptyList())
            if (accounts.isNotEmpty()) {
                selectedAccount = accounts.find { it.id == cfg?.accountId }
                    ?: accounts.first()
            }
        }

        LaunchedEffect(backend) {
            backend.events.collect { event ->
                when (event) {
                    is LauncherEvent.GameReady -> backend.logInfo("游戏窗口弹出来了 (PID=${event.pid})")
                    is LauncherEvent.GameExited -> backend.logInfo("游戏关了 (PID=${event.pid})")
                    is LauncherEvent.DownloadProgressEvent -> Unit
                    is LauncherEvent.JavaIndexStarted -> backend.logInfo("开始索引 Java: ${event.roots.size} 个根目录, ${event.workers} 个线程")
                    is LauncherEvent.JavaIndexProgress -> {
                        // 全盘索引会产生海量进度事件，只抽样打日志
                        progressTick++
                        if (progressTick % 20 == 0) {
                            backend.logInfo("索引进度: 扫描 ${event.directoriesScanned} 个目录")
                        }
                    }
                    is LauncherEvent.JavaIndexFound -> backend.logInfo("发现 Java: ${event.installation.path}")
                    is LauncherEvent.JavaIndexCompleted -> {
                        backend.logInfo("Java 索引完成: 找到 ${event.installations.size} 个")
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = getString(Res.string.snack_java_index_done, event.installations.size.toString()),
                                duration = SnackbarDuration.Short,
                            )
                        }
                    }
                    is LauncherEvent.JavaIndexError -> {
                        backend.logError("Java 索引错误: ${event.message}")
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = getString(Res.string.snack_java_index_failed, event.message),
                                duration = SnackbarDuration.Short,
                            )
                        }
                    }
                    is LauncherEvent.AuthStatusChanged -> {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = event.status,
                                duration = SnackbarDuration.Short,
                            )
                        }
                    }
                    is LauncherEvent.AuthCompleted -> {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = getString(Res.string.snack_login_ok, event.account.username),
                                duration = SnackbarDuration.Short,
                            )
                        }
                    }
                    is LauncherEvent.AuthFailed -> {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = getString(Res.string.snack_login_failed, event.message),
                                duration = SnackbarDuration.Short,
                            )
                        }
                    }
                    is LauncherEvent.AccountsChanged -> {
                        if (event.accounts.isNotEmpty()) {
                            val keep = selectedAccount?.id
                            selectedAccount = event.accounts.find { it.id == keep }
                                ?: event.accounts.firstOrNull()
                        } else {
                            selectedAccount = null
                        }
                    }
                    else -> Unit
                }
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(start = 44.dp),
            ) {
                when (page) {
                    Page.LAUNCH -> LaunchPage(
                        api = backend,
                        events = backend.events,
                        launchSignal = launchSignal,
                        dialog = dialog,
                        selectedAccount = selectedAccount,
                        onOpenAccountDialog = { dialog = SettingsDialog.ACCOUNT },
                        onOpenSettingsDialog = { dialog = it },
                    )
                    Page.DOWNLOAD -> DownloadPage(
                        api = backend,
                        events = backend.events,
                        onBack = { page = Page.LAUNCH },
                    )
                    Page.INSTANCES -> InstancesPage(
                        api = backend,
                        onLaunchInstance = { instance ->
                            // 异步启动：在启动器自己的协程中执行，UI 组合销毁不影响
                            val acc = selectedAccount
                            backend.launchInstanceAsync(
                                instanceId = instance.id,
                                username = acc?.username ?: config?.username.orEmpty().ifBlank { "TestUser" },
                                accessToken = acc?.accessToken ?: "0",
                                uuid = acc?.uuid ?: "00000000-0000-0000-0000-000000000000",
                                userType = acc?.userType ?: "legacy",
                            )
                            scope.launch {
                                showMessage(getString(Res.string.snack_instance_launching, instance.name))
                            }
                        },
                        onMessage = ::showMessage,
                    )
                    Page.LOGS -> LogsPage(
                        api = backend,
                        events = backend.events,
                        onBack = { page = Page.LAUNCH },
                    )
                }
            }

            if (dialog == SettingsDialog.ACCOUNT) {
                AccountDialog(
                    api = backend,
                    events = backend.events,
                    selectedAccountId = selectedAccount?.id.orEmpty(),
                    onSelectAccount = { id ->
                        selectedAccount = null
                        scope.launch {
                            val accounts = backend.getAccounts()
                            selectedAccount = accounts.find { it.id == id }
                            val cfg = backend.getDownloadConfig()
                            backend.saveDownloadConfig(cfg.copy(accountId = id))
                        }
                    },
                    onDismiss = { dialog = null },
                )
            }

            AdvancedSettingsDialogs(
                api = backend,
                dialog = advancedDialog,
                config = config,
                onDismiss = { advancedDialog = null },
                onApply = { newConfig ->
                    scope.launch {
                        backend.saveDownloadConfig(newConfig)
                        config = newConfig
                        advancedDialog = null
                        showMessage(getString(Res.string.snack_settings_saved))
                    }
                },
            )

            LauncherSidebar(
                modifier = Modifier.align(Alignment.CenterStart),
                categories = listOf(
                    SidebarCategory(
                        label = stringResource(Res.string.sidebar_launch),
                        icon = Icons.Default.PlayArrow,
                        items = listOf(
                            SidebarItem(stringResource(Res.string.sidebar_launch_mc)) {
                                page = Page.LAUNCH
                                launchSignal++
                            },
                        ),
                        direct = true,
                    ),
                    SidebarCategory(
                        label = stringResource(Res.string.sidebar_download),
                        icon = Icons.Default.Download,
                        items = listOf(
                            SidebarItem(stringResource(Res.string.sidebar_mc_download)) { page = Page.DOWNLOAD },
                            SidebarItem(stringResource(Res.string.sidebar_java_download)) { page = Page.DOWNLOAD },
                        ),
                    ),
                    SidebarCategory(
                        label = stringResource(Res.string.sidebar_instances),
                        icon = Icons.Default.ViewList,
                        items = listOf(
                            SidebarItem(stringResource(Res.string.sidebar_multi_instance)) { page = Page.INSTANCES },
                        ),
                    ),
                    SidebarCategory(
                        label = stringResource(Res.string.sidebar_logs),
                        icon = Icons.Default.Article,
                        items = listOf(
                            SidebarItem(stringResource(Res.string.sidebar_log_panel)) { page = Page.LOGS },
                        ),
                    ),
                    SidebarCategory(
                        label = stringResource(Res.string.sidebar_settings),
                        icon = Icons.Default.Settings,
                        items = listOf(
                            SidebarItem(stringResource(Res.string.sidebar_account)) { dialog = SettingsDialog.ACCOUNT },
                            SidebarItem(stringResource(Res.string.sidebar_mc_path)) { dialog = SettingsDialog.MC_PATH },
                            SidebarItem(stringResource(Res.string.sidebar_mc_version)) { dialog = SettingsDialog.MC_VERSION },
                            SidebarItem(stringResource(Res.string.sidebar_username)) { dialog = SettingsDialog.USERNAME },
                            SidebarItem(stringResource(Res.string.sidebar_java)) { dialog = SettingsDialog.JAVA },
                            SidebarItem(stringResource(Res.string.sidebar_memory)) { dialog = SettingsDialog.MEM },
                            SidebarItem(stringResource(Res.string.sidebar_mirror)) { advancedDialog = AdvancedDialog.MIRROR },
                            SidebarItem(stringResource(Res.string.sidebar_advanced)) { advancedDialog = AdvancedDialog.ADVANCED_ARGS },
                            SidebarItem(stringResource(Res.string.sidebar_data_dir)) { advancedDialog = AdvancedDialog.DATA_DIR },
                        ),
                    ),
                ),
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
            )
        }
    }
}
