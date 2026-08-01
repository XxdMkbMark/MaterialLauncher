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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SidebarItem(
    val label: String,
    val onClick: () -> Unit,
)

data class SidebarCategory(
    val label: String,
    val icon: ImageVector,
    val items: List<SidebarItem>,
    val direct: Boolean = false,
)

/**
 * 悬浮折叠侧边栏：鼠标悬停展开子菜单，移出后 200ms 收起。
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LauncherSidebar(
    categories: List<SidebarCategory>,
    modifier: Modifier = Modifier,
) {
    var activeLabel by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var closeJob by remember { mutableStateOf<Job?>(null) }

    fun keepOpen() {
        closeJob?.cancel()
        closeJob = null
    }

    fun scheduleClose() {
        closeJob?.cancel()
        closeJob = scope.launch {
            delay(200)
            activeLabel = null
        }
    }

    fun toggle(category: SidebarCategory) {
        if (category.direct) {
            category.items.firstOrNull()?.onClick?.invoke()
        } else if (activeLabel == category.label) {
            activeLabel = null
        } else {
            activeLabel = category.label
        }
    }

    Box(modifier) {
        // 侧边栏主体
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(44.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary)
                .pointerMoveFilter(
                    onEnter = {
                        keepOpen()
                        true
                    },
                    onExit = {
                        scheduleClose()
                        true
                    },
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            categories.forEach { category ->
                val active = activeLabel == category.label
                val iconColor by animateColorAsState(
                    targetValue = if (active) Color.White else Color.White.copy(alpha = 0.7f),
                )
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .padding(vertical = 6.dp)
                        .clickable { toggle(category) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = category.label,
                        tint = iconColor,
                    )
                }
            }
        }

        // 弹出的子菜单
        AnimatedVisibility(
            visible = activeLabel != null,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 44.dp)
                .width(200.dp)
                .fillMaxHeight(),
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start),
        ) {
            val category = categories.firstOrNull { it.label == activeLabel } ?: return@AnimatedVisibility
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .pointerMoveFilter(
                        onEnter = {
                            keepOpen()
                            true
                        },
                        onExit = {
                            scheduleClose()
                            true
                        },
                    )
                    .padding(vertical = 16.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = category.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    category.items.forEach { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    item.onClick()
                                    scheduleClose()
                                }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}
