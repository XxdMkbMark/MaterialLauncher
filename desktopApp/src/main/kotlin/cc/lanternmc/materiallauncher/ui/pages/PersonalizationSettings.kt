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

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import cc.lanternmc.materiallauncher.generated.resources.Res
import cc.lanternmc.materiallauncher.generated.resources.page_personalization
import cc.lanternmc.materiallauncher.ui.components.SettingsSubPage
import cc.lanternmc.materiallauncher.ui.navigation.Destination
import cc.lanternmc.materiallauncher.ui.navigation.navigateTo
import org.jetbrains.compose.resources.stringResource

@Composable
fun PersonalizationSettingsPage(navController: NavHostController) {
    SettingsSubPage(
        title = stringResource(Res.string.page_personalization),
        onBack = { navController.navigateTo(destination = Destination.SETTINGS) },
    )
}
