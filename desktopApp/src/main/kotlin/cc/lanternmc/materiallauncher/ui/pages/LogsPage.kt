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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.Flow
import cc.lanternmc.materiallauncher.api.LauncherEvent

private const val MAX_LOG_LINES = 2000

/**
 * 日志面板：实时显示后端日志（INFO/WARN/ERROR），自动滚动到底部。
 */
@Composable
fun LogsPage(
    events: Flow<LauncherEvent>,
    onBack: () -> Unit,
) {
    val lines = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()

    LaunchedEffect(events) {
        events.collect { event ->
            if (event is LauncherEvent.LogLine) {
                val colorTag = when (event.level) {
                    "WARN" -> "[WARN]"
                    "ERROR" -> "[ERROR]"
                    else -> "[INFO]"
                }
                lines.add("[${event.time}] $colorTag ${event.message}")
                // 防止无限增长
                if (lines.size > MAX_LOG_LINES) {
                    lines.removeRange(0, lines.size - MAX_LOG_LINES)
                }
            }
        }
    }

    // 新日志出现时自动滚到底部
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.scrollToItem(lines.size - 1)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("日志面板", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = { lines.clear() }) { Text("清空") }
            TextButton(onClick = onBack) { Text("返回") }
        }
        Box(Modifier.fillMaxSize().padding(top = 8.dp)) {
            if (lines.isEmpty()) {
                Text(
                    "暂无日志…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    items(lines.size) { index ->
                        val line = lines[index]
                        val color = when {
                            line.contains("[ERROR]") -> Color(0xFFE53935)
                            line.contains("[WARN]") -> Color(0xFFF9A825)
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        Text(
                            line,
                            fontSize = 11.sp,
                            color = color,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        )
                    }
                }
            }
        }
    }
}
