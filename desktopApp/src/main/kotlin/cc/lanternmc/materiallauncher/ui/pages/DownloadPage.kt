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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import cc.lanternmc.materiallauncher.api.DownloadConfig
import cc.lanternmc.materiallauncher.api.DownloadProgress
import cc.lanternmc.materiallauncher.api.JavaReleaseInfo
import cc.lanternmc.materiallauncher.api.LauncherApi
import cc.lanternmc.materiallauncher.api.LauncherEvent
import cc.lanternmc.materiallauncher.api.MinecraftVersionEntry
import cc.lanternmc.materiallauncher.generated.resources.Res
import cc.lanternmc.materiallauncher.generated.resources.action_cancel
import cc.lanternmc.materiallauncher.generated.resources.action_start_download
import cc.lanternmc.materiallauncher.generated.resources.back_to_launch
import cc.lanternmc.materiallauncher.generated.resources.download_name_exists
import cc.lanternmc.materiallauncher.generated.resources.download_name_hint
import cc.lanternmc.materiallauncher.generated.resources.download_version_title
import cc.lanternmc.materiallauncher.generated.resources.eta_hour_min
import cc.lanternmc.materiallauncher.generated.resources.eta_min_sec
import cc.lanternmc.materiallauncher.generated.resources.eta_seconds
import cc.lanternmc.materiallauncher.generated.resources.eta_unknown
import cc.lanternmc.materiallauncher.generated.resources.instance_name
import cc.lanternmc.materiallauncher.generated.resources.progress_extracting
import cc.lanternmc.materiallauncher.generated.resources.progress_fetching
import cc.lanternmc.materiallauncher.generated.resources.sidebar_java_download
import cc.lanternmc.materiallauncher.generated.resources.sidebar_mc_download
import org.jetbrains.compose.resources.stringResource

@Composable
fun DownloadPage(
    api: LauncherApi,
    events: Flow<LauncherEvent>,
    onBack: () -> Unit,
) {
    val mcVersions = remember { mutableStateListOf<MinecraftVersionEntry>() }
    val javaVersions = remember { mutableStateListOf<JavaReleaseInfo>() }
    var config by remember { mutableStateOf<DownloadConfig?>(null) }
    val downloads = remember { mutableStateMapOf<String, DownloadProgress>() }
    var loading by remember { mutableStateOf(true) }
    // MC 下载命名对话框状态：null = 关闭；否则记录待下载的版本
    var pendingMcDownload by remember { mutableStateOf<MinecraftVersionEntry?>(null) }

    LaunchedEffect(Unit) {
        try {
            val mc = api.getMinecraftVersions()
            mcVersions.clear()
            mcVersions.addAll(mc)
        } catch (e: Exception) {
            api.logError("获取 MC 版本失败: $e")
        }
        try {
            val java = api.getJavaVersions()
            javaVersions.clear()
            javaVersions.addAll(java)
        } catch (e: Exception) {
            api.logError("获取 Java 版本失败: $e")
        }
        try {
            config = api.getDownloadConfig()
        } catch (e: Exception) {
            api.logError("获取配置失败: $e")
        }
        loading = false
    }

    LaunchedEffect(events) {
        val scope = this
        events.collect { event ->
            if (event is LauncherEvent.DownloadProgressEvent) {
                val progress = event.progress
                downloads[progress.id] = progress
                if (progress.status == "done" || progress.status == "error") {
                    scope.launch {
                        delay(5000.milliseconds)
                        downloads.remove(progress.id)
                    }
                }
            }
        }
    }

    fun progressFor(item: String): DownloadProgress? =
        downloads.values.firstOrNull { it.item == item }

    fun isBusy(prog: DownloadProgress?): Boolean =
        prog != null && (prog.status == "fetching" || prog.status == "downloading" || prog.status == "extracting")

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LinearProgressIndicator()
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text(stringResource(Res.string.back_to_launch)) }
        }

        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VersionDownloadCard(
                title = stringResource(Res.string.sidebar_mc_download),
                modifier = Modifier.weight(1f),
            ) {
                items(mcVersions) { version ->
                    val prog = progressFor(version.id)
                    val busy = isBusy(prog)
                    val done = prog?.status == "done"
                    val err = prog?.status == "error"
                    DownloadRow(
                        title = version.id,
                        subtitle = version.releaseTime.take(10),
                        busy = busy,
                        prog = prog,
                        done = done,
                        err = err,
                        onClick = { if (!busy) pendingMcDownload = version },
                    )
                }
            }
            VersionDownloadCard(
                title = stringResource(Res.string.sidebar_java_download),
                modifier = Modifier.weight(1f),
            ) {
                items(javaVersions) { version ->
                    val prog = progressFor(version.id)
                    val busy = isBusy(prog)
                    val done = prog?.status == "done"
                    val err = prog?.status == "error"
                    DownloadRow(
                        title = version.id,
                        subtitle = formatSize(version.downloadSize),
                        busy = busy,
                        prog = prog,
                        done = done,
                        err = err,
                        onClick = { if (!busy) api.startJavaDownload(version.id) },
                    )
                }
            }
        }
    }

    // MC 下载命名对话框：确认实例名称后开始下载
    pendingMcDownload?.let { version ->
        var name by remember { mutableStateOf(version.id) }
        AlertDialog(
            onDismissRequest = { pendingMcDownload = null },
            title = { Text(stringResource(Res.string.download_version_title, version.id)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(Res.string.download_name_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(Res.string.instance_name)) },
                    )
                    Text(
                        stringResource(Res.string.download_name_exists),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    pendingMcDownload = null
                    api.startMinecraftDownload(version.id, name.trim())
                }) { Text(stringResource(Res.string.action_start_download)) }
            },
            dismissButton = { TextButton(onClick = { pendingMcDownload = null }) { Text(stringResource(Res.string.action_cancel)) } },
        )
    }
}

@Composable
private fun VersionDownloadCard(
    title: String,
    modifier: Modifier = Modifier,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun DownloadRow(
    title: String,
    subtitle: String,
    busy: Boolean,
    prog: DownloadProgress?,
    done: Boolean,
    err: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (busy) {
                val fraction = if (prog != null && prog.total > 0) {
                    (prog.downloaded.toFloat() / prog.total).coerceIn(0f, 1f)
                } else {
                    0f
                }
                val fetchingLabel = stringResource(Res.string.progress_fetching)
                val extractingLabel = stringResource(Res.string.progress_extracting)
                val downloaded = prog?.downloaded ?: 0
                val total = prog?.total ?: 0
                val speed = prog?.speedBytesPerSec ?: 0
                val etaSec = if (speed > 0) (total - downloaded).coerceAtLeast(0) / speed else -1L
                val etaLabel = formatEta(etaSec)
                LinearProgressIndicator(
                    progress = { if ((prog?.total ?: 0) > 0) fraction else 0f },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
                Text(
                    text = when (prog?.status) {
                        "fetching" -> fetchingLabel
                        "extracting" -> extractingLabel
                        else -> {
                            val base = "${formatSize(downloaded)} / ${formatSize(total)}"
                            if (speed > 0) {
                                val etaText = if (etaSec >= 0) "  ·  $etaLabel" else ""
                                "$base  ·  ${formatSize(speed)}/s$etaText"
                            } else {
                                base
                            }
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        when {
            busy -> Unit
            done -> Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF43A047))
            err -> Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            else -> Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
    val errorText = prog?.error
    if (err && !errorText.isNullOrBlank()) {
        Text(
            text = errorText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var i = 0
    while (value >= 1024 && i < units.size - 1) {
        value /= 1024
        i++
    }
    return String.format("%.1f %s", value, units[i])
}

/** 把剩余秒数格式化为 ETA 文本（如 剩余 1 分 30 秒）。 */
@Composable
fun formatEta(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0)
    val min = safe / 60
    val sec = safe % 60
    val hour = min / 60
    val minRemain = min % 60
    val unknown = stringResource(Res.string.eta_unknown)
    val secondsLabel = stringResource(Res.string.eta_seconds, safe.toString())
    val minutesLabel = stringResource(Res.string.eta_min_sec, min.toString(), sec.toString())
    val hoursLabel = stringResource(Res.string.eta_hour_min, hour.toString(), minRemain.toString())
    return when {
        seconds < 0 -> unknown
        seconds < 60 -> secondsLabel
        min < 60 -> minutesLabel
        else -> hoursLabel
    }
}
