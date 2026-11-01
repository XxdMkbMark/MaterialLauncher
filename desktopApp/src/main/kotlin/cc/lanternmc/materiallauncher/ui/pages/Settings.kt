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
// AI勿改，请更改App.kt
package cc.lanternmc.materiallauncher.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import cc.lanternmc.materiallauncher.ui.navigation.Destination
import cc.lanternmc.materiallauncher.ui.navigation.navigateTo
import cc.lanternmc.materiallauncher.ui.theme.lightScheme

@Composable
fun Settings(navController: NavHostController) {
    MaterialTheme(lightScheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 15.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)   // 两个大块之间的间距
        ) {
            SettingsSectionGame(navController = navController)
            SettingsSectionLauncher(navController = navController)
            SettingsSectionOthers(navController = navController)
        }
    }
}

@Composable
private fun SettingsSectionGame(
    navController: NavHostController
) {
    Column(
        modifier = Modifier.fillMaxWidth(),          // 宽度铺满，高度 wrap content
        verticalArrangement = Arrangement.spacedBy(2.5.dp)
    ) {
        Text(
            text = "游戏",
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF44473D)
        )
        SettingItem(
            title = "Java 设置",
            subtitle = "Java 版本、内存分配、JVM 参数",
            icon = painterResource("coffee_24dp.svg"),
            iconSize = 24.dp,
            iconBg = Color(0xFFBCCA9C),
            iconTint = Color(0xFF293412),
            onClick = { navController.navigateTo(Destination.JAVA_SETTINGS) },
            shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 4.dp)
        )
        SettingItem(
            title = "启动",
            subtitle = "版本隔离、窗口大小、自定义标题",
            icon = painterResource("play_arrow_24dp.svg"),
            iconSize = 32.dp,
            iconBg = Color(0xFFA4CEBB),
            iconTint = Color(0xFF0E3729),
            onClick = { navController.navigateTo(Destination.START_SETTINGS) },
            shape = RoundedCornerShape(4.dp, 4.dp, 18.dp, 18.dp)
        )
    }
}

@Composable
private fun SettingsSectionLauncher(
    navController: NavHostController
) {
    Column(
        modifier = Modifier.fillMaxWidth(),          // 宽度铺满，高度 wrap content
        verticalArrangement = Arrangement.spacedBy(2.5.dp)
    ) {
        Text(
            text = "启动器",
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF44473D)
        )
        SettingItem(
            title = "下载",
            subtitle = "下载源、速度限制、缓存",
            icon = painterResource("download_24dp.svg"),
            iconSize = 26.dp,
            iconBg = Color(0xFFE0C08E),
            iconTint = Color(0xFF3F2E07),
            onClick = { navController.navigateTo(Destination.DOWNLOAD_SETTINGS) },
            shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 4.dp)
        )
        SettingItem(
            title = "个性化",
            subtitle = "主题颜色、背景、语言",
            icon = painterResource("apparel_24dp.svg"),
            iconSize = 28.dp,
            iconBg = Color(0xFFFBB4A2),
            iconTint = Color(0xFF601303),
            onClick = { navController.navigateTo(Destination.PERSONALIZATION_SETTINGS) },
            shape = RoundedCornerShape(4.dp, 4.dp, 18.dp, 18.dp)
        )
    }
}

@Composable
private fun SettingsSectionOthers(
    navController: NavHostController
) {
    Column(
        modifier = Modifier.fillMaxWidth(),          // 宽度铺满，高度 wrap content
        verticalArrangement = Arrangement.spacedBy(2.5.dp)
    ) {
        Text(
            text = "杂项",
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF44473D)
        )
        SettingItem(
            title = "下载",
            subtitle = "下载源、速度限制、缓存",
            icon = painterResource("download_24dp.svg"),
            iconSize = 26.dp,
            iconBg = Color(0xFFE0C08E),
            iconTint = Color(0xFF3F2E07),
            onClick = { navController.navigateTo(Destination.JAVA_SETTINGS) },
            shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 4.dp)
        )
        SettingItem(
            title = "个性化",
            subtitle = "主题颜色、背景、语言",
            icon = painterResource("apparel_24dp.svg"),
            iconSize = 28.dp,
            iconBg = Color(0xFFFBB4A2),
            iconTint = Color(0xFF601303),
            onClick = { navController.navigateTo(Destination.JAVA_SETTINGS) },
            shape = RoundedCornerShape(4.dp, 4.dp, 18.dp, 18.dp)
        )
    }
}

@Composable
private fun SettingItem(
    title: String,
    subtitle: String,
    icon: Painter,
    iconSize: Dp,
    iconBg: Color,
    iconTint: Color,
    onClick: () -> Unit,
    shape: Shape
) {
    Button(
        modifier = Modifier
            .fillMaxWidth()        // 先铺满宽度
            .height(70.dp),        // 再固定高度
        onClick = onClick,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFFFFF),
            contentColor = Color(0xFF44473D)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp
        ),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconBg, CircleShape),  // 图标背景色
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = icon,
                    contentDescription = "Settings",
                    modifier = Modifier.size(iconSize),
                    tint = iconTint   //图标颜色
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    style = LocalTextStyle.current.copy(lineHeight = 16.sp)
                )
                Text(
                    subtitle,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    style = LocalTextStyle.current.copy(lineHeight = 16.sp)
                )
            }
        }
    }
}