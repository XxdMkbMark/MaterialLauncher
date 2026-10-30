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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import cc.lanternmc.materiallauncher.ui.navigation.AppNavHost
import cc.lanternmc.materiallauncher.ui.navigation.Destination
import cc.lanternmc.materiallauncher.ui.navigation.navigateTo
import cc.lanternmc.materiallauncher.ui.theme.lightScheme

/*
@Composable
fun SampleSettings(navController: NavHostController) {
    MaterialTheme(lightScheme) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(15.dp,0.dp,30.dp,0.dp)) { // 第一个设置项
                Button(
                    modifier = Modifier.height(70.dp).fillMaxSize(),
                    onClick = {
                        navController.navigateTo(destination = Destination.JAVA_SETTINGS)
                    },
                    shape = RoundedCornerShape(18.dp,18.dp,5.dp,5.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFFFFF),
                        contentColor = Color(0xFF44473D)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp, focusedElevation = 0.dp, hoveredElevation = 0.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = Color(0xFFEAB7CB),   // 圆形填充色
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.LocalCafe,
                                "Settings",
                                modifier = Modifier.padding(10.dp).size(44.dp),
                                tint = Color(0xFF472536)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Java 设置",
                                fontWeight = FontWeight.SemiBold,
                                style = LocalTextStyle.current.copy(lineHeight = 16.sp),
                                fontSize = 15.sp
                            )
                            Text("Java 版本、内存分配、JVM 参数",
                                fontWeight = FontWeight.Normal,
                                style = LocalTextStyle.current.copy(lineHeight = 16.sp),

                            )
                        }
                    }
                }
            }
        }
    }
}

 */
@Composable
fun SampleSettings(navController: NavHostController) {
    MaterialTheme(lightScheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 15.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.5.dp)   // 两个按钮之间的间距
        ) {
            SampleSettingItem(
                title = "Java 设置",
                subtitle = "Java 版本、内存分配、JVM 参数",
                onClick = { navController.navigateTo(Destination.JAVA_SETTINGS) },
                shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 4.dp)
            )
            SampleSettingItem(
                title = "启动设置",
                subtitle = "版本隔离、窗口大小、启动器可见性",
                onClick = { navController.navigateTo(Destination.JAVA_SETTINGS) },
                shape = RoundedCornerShape(4.dp, 4.dp, 18.dp, 18.dp)
            )
        }
    }
}

@Composable
private fun SampleSettingItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    shape: Shape
) {
    Button(
        modifier = Modifier
            .fillMaxWidth()        // ← 先宽度铺满
            .height(70.dp),        // ← 再固定高度
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
                    .background(Color(0xFFEAB7CB), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocalCafe,
                    contentDescription = "Settings",
                    modifier = Modifier.size(24.dp),      // ← 别再用 padding(10).size(44)
                    tint = Color(0xFF472536)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    style = LocalTextStyle.current.copy(lineHeight = 16.sp)
                )
                Text(
                    subtitle,
                    fontWeight = FontWeight.Normal,
                    style = LocalTextStyle.current.copy(lineHeight = 16.sp)
                )
            }
        }
    }
}