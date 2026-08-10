package cc.lanternmc.materiallauncher.ui.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController

@Composable
fun JavaSettingsPage(navController: NavHostController) {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("Java Settings")
        }
    }
}