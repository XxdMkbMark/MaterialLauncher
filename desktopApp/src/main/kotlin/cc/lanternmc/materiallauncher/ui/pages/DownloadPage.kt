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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import cc.lanternmc.materiallauncher.api.DownloadConfig
import cc.lanternmc.materiallauncher.api.DownloadProgress
import cc.lanternmc.materiallauncher.api.JavaReleaseInfo
import cc.lanternmc.materiallauncher.api.LauncherApi
import cc.lanternmc.materiallauncher.api.LauncherEvent
import cc.lanternmc.materiallauncher.api.MinecraftVersionEntry

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
                        delay(5000)
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
            TextButton(onClick = onBack) { Text("返回启动页") }
        }

        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VersionDownloadCard(
                title = "Minecraft 下载",
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
                        onClick = { if (!busy) api.startMinecraftDownload(version.id) },
                    )
                }
            }
            VersionDownloadCard(
                title = "Java 下载",
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
                LinearProgressIndicator(
                    progress = { if (prog?.total ?: 0 > 0) fraction else 0f },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
                Text(
                    text = when (prog?.status) {
                        "fetching" -> "获取中..."
                        "extracting" -> "解压中..."
                        else -> "${formatSize(prog?.downloaded ?: 0)} / ${formatSize(prog?.total ?: 0)}"
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
