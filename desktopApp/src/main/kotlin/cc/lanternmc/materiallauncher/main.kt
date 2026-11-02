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
package cc.lanternmc.materiallauncher

// TODO: In windows prod package, only show few java but not all.Waka
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cc.lanternmc.materiallauncher.generated.resources.Res
import cc.lanternmc.materiallauncher.generated.resources.app_name
import cc.lanternmc.materiallauncher.i18n.AppLocale
import cc.lanternmc.materiallauncher.ui.Home
import org.jetbrains.compose.resources.stringResource

fun main() {
    // 第一帧之前固定语言。没配置时跟随系统；stringResource 按 JVM 默认 Locale 选 values/ 或 values-en/。
    AppLocale.applyFromConfig()
    application {
        val title = stringResource(Res.string.app_name)
        Window(
            onCloseRequest = ::exitApplication,
            title = title,
            state = rememberWindowState(width = 1032.dp, height = 617.dp),
            resizable = false,
            icon = painterResource("icon.png"),
        ) {
            Home()
        }
    }
}
