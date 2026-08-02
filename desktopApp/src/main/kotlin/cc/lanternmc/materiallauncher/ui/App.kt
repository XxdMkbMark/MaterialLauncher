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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import cc.lanternmc.materiallauncher.api.Account
import cc.lanternmc.materiallauncher.api.LauncherEvent
import cc.lanternmc.materiallauncher.core.LauncherBackend
import cc.lanternmc.materiallauncher.ui.pages.AccountDialog
import cc.lanternmc.materiallauncher.ui.pages.DownloadPage
import cc.lanternmc.materiallauncher.ui.pages.LaunchPage
import cc.lanternmc.materiallauncher.ui.pages.LauncherSidebar
import cc.lanternmc.materiallauncher.ui.pages.SettingsDialog
import cc.lanternmc.materiallauncher.ui.pages.SidebarCategory
import cc.lanternmc.materiallauncher.ui.pages.SidebarItem

private enum class Page { LAUNCH, DOWNLOAD }

@Composable
fun App(backend: LauncherBackend) {
    MaterialTheme(lightColorScheme()) {
        var page by remember { mutableStateOf(Page.LAUNCH) }
        var launchSignal by remember { mutableIntStateOf(0) }
        var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
        var progressTick by remember { mutableIntStateOf(0) }
        var selectedAccount by remember { mutableStateOf<Account?>(null) }
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        // 加载保存的账户选择与账户列表
        LaunchedEffect(Unit) {
            val cfg = runCatching { backend.getDownloadConfig() }.getOrNull()
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
                        label = "设置",
                        icon = Icons.Default.Settings,
                        items = listOf(
                            SidebarItem("账户") { dialog = SettingsDialog.ACCOUNT },
                            SidebarItem("MC 路径") { dialog = SettingsDialog.MC_PATH },
                            SidebarItem("MC 版本") { dialog = SettingsDialog.MC_VERSION },
                            SidebarItem("用户名") { dialog = SettingsDialog.USERNAME },
                            SidebarItem("Java") { dialog = SettingsDialog.JAVA },
                            SidebarItem("内存") { dialog = SettingsDialog.MEM },
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
