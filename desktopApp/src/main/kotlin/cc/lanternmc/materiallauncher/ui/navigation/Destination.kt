package cc.lanternmc.materiallauncher.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import cc.lanternmc.materiallauncher.generated.resources.Res
import cc.lanternmc.materiallauncher.generated.resources.app_registration_24dp
import cc.lanternmc.materiallauncher.generated.resources.apparel_24dp
import cc.lanternmc.materiallauncher.generated.resources.coffee_24dp
import cc.lanternmc.materiallauncher.generated.resources.download_24dp
import cc.lanternmc.materiallauncher.generated.resources.home_24dp
import cc.lanternmc.materiallauncher.generated.resources.person_24dp
import cc.lanternmc.materiallauncher.generated.resources.play_arrow_24dp
import cc.lanternmc.materiallauncher.generated.resources.settings_24dp
import org.jetbrains.compose.resources.DrawableResource

enum class Destination (  // 在此处定义所有需要跳转的页面
    val route: String,
    val label: String,
    val icon: DrawableResource,
    val showInNavigationRail: Boolean = false    // 是否在侧边栏中显示
) {
    HOME("home", "主页", Res.drawable.home_24dp),
    DOWNLOAD("download", "下载", Res.drawable.download_24dp, showInNavigationRail = true),
    USERS("users", "档案", Res.drawable.person_24dp, showInNavigationRail = true),
    VERSIONS("versions","版本", Res.drawable.app_registration_24dp, showInNavigationRail = true),
    SETTINGS("settings", "设置", Res.drawable.settings_24dp, showInNavigationRail = true),
    JAVA_SETTINGS("java_settings", "Java 设置", Res.drawable.coffee_24dp),
    PERSONALIZATION_SETTINGS("personalization_settings", "个性化设置", Res.drawable.apparel_24dp),
    START_SETTINGS("start_settings", "启动设置", Res.drawable.play_arrow_24dp),
    DOWNLOAD_SETTINGS("download_settings", "下载设置", Res.drawable.download_24dp),
}