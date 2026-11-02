package cc.lanternmc.materiallauncher.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import cc.lanternmc.materiallauncher.generated.resources.Res
import cc.lanternmc.materiallauncher.generated.resources.nav_download
import cc.lanternmc.materiallauncher.generated.resources.nav_download_settings
import cc.lanternmc.materiallauncher.generated.resources.nav_home
import cc.lanternmc.materiallauncher.generated.resources.nav_java_settings
import cc.lanternmc.materiallauncher.generated.resources.nav_personalization
import cc.lanternmc.materiallauncher.generated.resources.nav_settings
import cc.lanternmc.materiallauncher.generated.resources.nav_start_settings
import cc.lanternmc.materiallauncher.generated.resources.nav_users
import cc.lanternmc.materiallauncher.generated.resources.nav_versions
import org.jetbrains.compose.resources.StringResource

enum class Destination (  // 在此处定义所有需要跳转的页面
    val route: String,
    val label: StringResource,
    val icon: ImageVector,
    val showInNavigationRail: Boolean = false    // 是否在侧边栏中显示
) {
    HOME("home", Res.string.nav_home, Icons.Rounded.Home),
    DOWNLOAD("download", Res.string.nav_download, Icons.Rounded.Download, showInNavigationRail = true),
    USERS("users", Res.string.nav_users, Icons.Rounded.Person, showInNavigationRail = true),
    VERSIONS("versions", Res.string.nav_versions, Icons.Rounded.Checklist, showInNavigationRail = true),
    SETTINGS("settings", Res.string.nav_settings, Icons.Rounded.Settings, showInNavigationRail = true),
    JAVA_SETTINGS("java_settings", Res.string.nav_java_settings, Icons.Rounded.Settings),
    PERSONALIZATION_SETTINGS("personalization_settings", Res.string.nav_personalization, Icons.Rounded.Settings),
    START_SETTINGS("start_settings", Res.string.nav_start_settings, Icons.Rounded.Settings),
    DOWNLOAD_SETTINGS("download_settings", Res.string.nav_download_settings, Icons.Rounded.Settings),
}