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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.awt.datatransfer.StringSelection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import cc.lanternmc.materiallauncher.api.Account
import cc.lanternmc.materiallauncher.api.DeviceCodeInfo
import cc.lanternmc.materiallauncher.api.LauncherApi
import cc.lanternmc.materiallauncher.api.LauncherEvent
import cc.lanternmc.materiallauncher.generated.resources.Res
import cc.lanternmc.materiallauncher.generated.resources.account_microsoft
import cc.lanternmc.materiallauncher.generated.resources.account_offline_label
import cc.lanternmc.materiallauncher.generated.resources.accounts_empty
import cc.lanternmc.materiallauncher.generated.resources.accounts_title
import cc.lanternmc.materiallauncher.generated.resources.action_add
import cc.lanternmc.materiallauncher.generated.resources.action_close
import cc.lanternmc.materiallauncher.generated.resources.add_offline
import cc.lanternmc.materiallauncher.generated.resources.cannot_open_browser
import cc.lanternmc.materiallauncher.generated.resources.cd_current_account
import cc.lanternmc.materiallauncher.generated.resources.cd_delete
import cc.lanternmc.materiallauncher.generated.resources.copied_code
import cc.lanternmc.materiallauncher.generated.resources.copy_code
import cc.lanternmc.materiallauncher.generated.resources.device_code_hint
import cc.lanternmc.materiallauncher.generated.resources.microsoft_login
import cc.lanternmc.materiallauncher.generated.resources.nickname
import cc.lanternmc.materiallauncher.generated.resources.open_browser
import cc.lanternmc.materiallauncher.generated.resources.snack_login_ok
import cc.lanternmc.materiallauncher.generated.resources.waiting_auth
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * 账户管理对话框：多账户列表 / 添加离线账户 / Microsoft 设备码登录。
 */
@Composable
fun AccountDialog(
    api: LauncherApi,
    events: Flow<LauncherEvent>,
    selectedAccountId: String,
    onSelectAccount: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val accounts = remember { mutableStateListOf<Account>() }
    var addingOffline by remember { mutableStateOf(false) }
    var offlineName by remember { mutableStateOf("") }
    var loggingIn by remember { mutableStateOf(false) }
    var deviceCode by remember { mutableStateOf<DeviceCodeInfo?>(null) }
    var loginStatus by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf("") }
    val copiedCode = stringResource(Res.string.copied_code)
    val waitingAuth = stringResource(Res.string.waiting_auth)
    val microsoftLogin = stringResource(Res.string.microsoft_login)

    suspend fun refreshAccounts() {
        val list = runCatching { api.getAccounts() }.getOrDefault(emptyList())
        accounts.clear()
        accounts.addAll(list)
    }

    fun openBrowser(url: String) {
        val opened = runCatching {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url))
        }.isSuccess
        if (!opened) {
            scope.launch { loginError = getString(Res.string.cannot_open_browser, url) }
        }
    }

    LaunchedEffect(Unit) { refreshAccounts() }

    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is LauncherEvent.AccountsChanged -> {
                    accounts.clear()
                    accounts.addAll(event.accounts)
                }
                is LauncherEvent.AuthDeviceCodeReceived -> {
                    deviceCode = event.deviceCode
                    loginError = ""
                    loginStatus = ""
                }
                is LauncherEvent.AuthStatusChanged -> loginStatus = event.status
                is LauncherEvent.AuthCompleted -> {
                    loggingIn = false
                    deviceCode = null
                    loginStatus = getString(Res.string.snack_login_ok, event.account.username)
                    onSelectAccount(event.account.id)
                    scope.launch { refreshAccounts() }
                }
                is LauncherEvent.AuthFailed -> {
                    loggingIn = false
                    deviceCode = null
                    loginError = event.message
                }
                else -> Unit
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.width(480.dp).padding(24.dp)) {
                Text(stringResource(Res.string.accounts_title), style = MaterialTheme.typography.titleLarge)

                if (accounts.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.accounts_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(accounts) { account ->
                            AccountRow(
                                account = account,
                                selected = account.id == selectedAccountId,
                                onSelect = { onSelectAccount(account.id) },
                                onDelete = {
                                    scope.launch { api.removeAccount(account.id) }
                                    if (account.id == selectedAccountId) onSelectAccount("")
                                },
                            )
                        }
                    }
                }

                if (addingOffline) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = offlineName,
                            onValueChange = { offlineName = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text(stringResource(Res.string.nickname)) },
                        )
                        Button(onClick = {
                            scope.launch {
                                val acc = api.addOfflineAccount(offlineName)
                                onSelectAccount(acc.id)
                                offlineName = ""
                                addingOffline = false
                                refreshAccounts()
                            }
                        }) { Text(stringResource(Res.string.action_add)) }
                    }
                } else {
                    TextButton(onClick = { addingOffline = true }) { Text(stringResource(Res.string.add_offline)) }
                }

                if (deviceCode != null) {
                    val code = deviceCode!!
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(16.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.device_code_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = code.verificationUri,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            text = code.userCode,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 6.sp,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(onClick = { openBrowser(code.verificationUri) }) { Text(stringResource(Res.string.open_browser)) }
                            TextButton(onClick = {
                                scope.launch {
                                    @OptIn(ExperimentalComposeUiApi::class)
                                    clipboard.setClipEntry(ClipEntry(StringSelection(code.userCode)))
                                }
                                loginStatus = copiedCode
                            }) { Text(stringResource(Res.string.copy_code)) }
                        }
                    }
                }

                if (loginStatus.isNotBlank()) {
                    Text(
                        text = loginStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (loginError.isNotBlank()) {
                    Text(
                        text = loginError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                Button(
                    onClick = {
                        if (!loggingIn) {
                            loggingIn = true
                            loginError = ""
                            api.startMicrosoftLogin()
                        }
                    },
                    enabled = !loggingIn && deviceCode == null,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Text(if (loggingIn) waitingAuth else microsoftLogin)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_close)) }
                }
            }
        }
    }
}

@Composable
private fun AccountRow(
    account: Account,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = account.username,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            val microsoftAccount = stringResource(Res.string.account_microsoft, account.userType)
            val offlineAccount = stringResource(Res.string.account_offline_label)
            Text(
                text = if (account.type == "online") microsoftAccount else offlineAccount,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(Res.string.cd_current_account),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(Res.string.cd_delete),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}
