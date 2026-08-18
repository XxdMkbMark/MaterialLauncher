package cc.lanternmc.materiallauncher.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destination (  // 在此处定义所有需要跳转的页面
    val route: String,
    val label: String,
    val icon: ImageVector,
    val showInNavigationRail: Boolean = false    // 是否在侧边栏中显示
) {
    HOME("home", "主页", Icons.Rounded.Home),
    DOWNLOAD("download", "下载", Icons.Rounded.Download, showInNavigationRail = true),
    USERS("users", "档案", Icons.Rounded.Person, showInNavigationRail = true),
    VERSIONS("versions","版本", Icons.Rounded.Checklist, showInNavigationRail = true),
    SETTINGS("settings", "设置", Icons.Rounded.Settings, showInNavigationRail = true),
    JAVA_SETTINGS("java_settings", "Java设置", Icons.Rounded.Settings),
}