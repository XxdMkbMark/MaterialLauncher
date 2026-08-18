package cc.lanternmc.materiallauncher.ui.navigation

import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import cc.lanternmc.materiallauncher.api.JavaInstallation
import cc.lanternmc.materiallauncher.ui.HomePage
import cc.lanternmc.materiallauncher.ui.navigation.Destination
import cc.lanternmc.materiallauncher.ui.pages.JavaSettingsPage
import cc.lanternmc.materiallauncher.ui.pages.SampleDownloadPage
import cc.lanternmc.materiallauncher.ui.pages.SampleSettings
import cc.lanternmc.materiallauncher.ui.pages.SampleUsersManagement
import cc.lanternmc.materiallauncher.ui.pages.SampleVersionsManagement
import cc.lanternmc.materiallauncher.viewmodel.JavaScannerViewModel

@Composable
fun AppNavHost (     // 在Destination中添加路由项目后要在此对应到页面函数
    navController: NavHostController,
    startDestination: Destination = Destination.HOME,
    viewModel: JavaScannerViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(navController = navController, startDestination = startDestination.route, modifier = modifier) {
        Destination.entries.forEach { destination ->
            composable(destination.route) {
                when (destination) {    // 路由页面（目的地和页面函数对应）
                    Destination.HOME -> HomePage(navController)
                    Destination.DOWNLOAD -> SampleDownloadPage(navController)
                    Destination.USERS -> SampleUsersManagement(navController)
                    Destination.VERSIONS -> SampleVersionsManagement(navController)
                    Destination.SETTINGS -> SampleSettings(navController)
                    Destination.JAVA_SETTINGS -> JavaSettingsPage(navController, viewModel = viewModel)
                }
            }
        }
    }
}