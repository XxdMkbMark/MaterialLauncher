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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cc.lanternmc.materiallauncher.ui.theme.lightScheme

/**
 * 设置子页的公共外壳：浅色主题、返回按钮、顶栏标题。
 * 返回图标默认 26.dp，和原来三个空设置页一致。Java 设置页原来没有固定尺寸，传入 [backIconSize] = null 保持原样。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSubPage(
    title: String,
    onBack: () -> Unit,
    backIconSize: Dp? = 26.dp,
    content: @Composable (PaddingValues) -> Unit = {},
) {
    MaterialTheme(lightScheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(title) },
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        modifier = if (backIconSize != null) Modifier.size(backIconSize) else Modifier,
                                        contentDescription = "",
                                    )
                                }
                            },
                        )
                    },
                ) { contentPadding ->
                    content(contentPadding)
                }
            }
        }
    }
}
