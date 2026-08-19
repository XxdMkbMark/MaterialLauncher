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
import cc.lanternmc.materiallauncher.ui.components.AdvancedDialog
import cc.lanternmc.materiallauncher.ui.components.AdvancedSettingsDialogs
import cc.lanternmc.materiallauncher.ui.pages.*
import kotlinx.coroutines.launch

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
                                message = "Java 索引完成，找到 ${event.installations.size} 个",
                                duration = SnackbarDuration.Short,
                            )
                        }
                    }
                    is LauncherEvent.JavaIndexError -> {
                        backend.logError("Java 索引错误: ${event.message}")
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Java 索引失败: ${event.message}",
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
                                message = "登录成功: ${event.account.username}",
                                duration = SnackbarDuration.Short,
                            )
                        }
                    }
                    is LauncherEvent.AuthFailed -> {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "登录失败: ${event.message}",
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
                            showMessage("实例「${instance.name}」启动中...（日志见「日志」页）")
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
                        showMessage("设置已保存")
                    }
                },
            )

            LauncherSidebar(
                modifier = Modifier.align(Alignment.CenterStart),
                categories = listOf(
                    SidebarCategory(
                        label = "启动",
                        icon = Icons.Default.PlayArrow,
                        items = listOf(
                            SidebarItem("启动 Minecraft") {
                                page = Page.LAUNCH
                                launchSignal++
                            },
                        ),
                        direct = true,
                    ),
                    SidebarCategory(
                        label = "下载",
                        icon = Icons.Default.Download,
                        items = listOf(
                            SidebarItem("Minecraft 下载") { page = Page.DOWNLOAD },
                            SidebarItem("Java 下载") { page = Page.DOWNLOAD },
                        ),
                    ),
                    SidebarCategory(
                        label = "实例",
                        icon = Icons.Default.ViewList,
                        items = listOf(
                            SidebarItem("多实例管理") { page = Page.INSTANCES },
                        ),
                    ),
                    SidebarCategory(
                        label = "日志",
                        icon = Icons.Default.Article,
                        items = listOf(
                            SidebarItem("日志面板") { page = Page.LOGS },
                        ),
                    ),
                    SidebarCategory(
                        label = "设置",
                        icon = Icons.Default.Settings,
                        items = listOf(
                            SidebarItem("账户") { dialog = SettingsDialog.ACCOUNT },
                            SidebarItem("MC 路径") { dialog = SettingsDialog.MC_PATH },
                            SidebarItem("MC 版本") { dialog = SettingsDialog.MC_VERSION },
                            SidebarItem("用户名") { dialog = SettingsDialog.USERNAME },
                            SidebarItem("Java") { dialog = SettingsDialog.JAVA },
                            SidebarItem("内存") { dialog = SettingsDialog.MEM },
                            SidebarItem("下载源") { advancedDialog = AdvancedDialog.MIRROR },
                            SidebarItem("高级参数") { advancedDialog = AdvancedDialog.ADVANCED_ARGS },
                            SidebarItem("数据目录") { advancedDialog = AdvancedDialog.DATA_DIR },
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
