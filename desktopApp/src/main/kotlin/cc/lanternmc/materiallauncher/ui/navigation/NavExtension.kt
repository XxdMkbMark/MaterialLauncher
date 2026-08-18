package cc.lanternmc.materiallauncher.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder

fun NavController.navigateTo(
    destination: Destination,
    navOptions: NavOptions? = null,
    builder: NavOptionsBuilder.() -> Unit = {} // 允许传入原navigate函数的参数
) {
    this.navigate(destination.route, navOptions) // 更方便的跳转，只需在跳转处调用navigateTo即可
}