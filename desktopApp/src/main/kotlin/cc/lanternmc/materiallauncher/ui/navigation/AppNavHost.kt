package cc.lanternmc.materiallauncher.ui.navigation

import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cc.lanternmc.materiallauncher.ui.navigation.Destination
import cc.lanternmc.materiallauncher.ui.SampleHome
import cc.lanternmc.materiallauncher.ui.pages.SampleDownloadPage
import cc.lanternmc.materiallauncher.ui.pages.SampleSettings
import cc.lanternmc.materiallauncher.ui.pages.SampleUsersManagement
import cc.lanternmc.materiallauncher.ui.pages.SampleVersionsManagement

@Composable
fun AppNavHost (navController: NavHostController, startDestination: Destination = Destination.HOME, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = startDestination.route, modifier = modifier) {
        Destination.entries.forEach { destination ->
            composable(destination.route) {
                when (destination) {    // 路由页面（目的地和页面函数的对应）
                    Destination.HOME -> SampleHome()
                    Destination.DOWNLOAD -> SampleDownloadPage()
                    Destination.USERS -> SampleUsersManagement()
                    Destination.VERSIONS -> SampleVersionsManagement()
                    Destination.SETTINGS -> SampleSettings()
                    Destination.JAVA_SETTINGS -> {}
                }
            }
        }
    }
}