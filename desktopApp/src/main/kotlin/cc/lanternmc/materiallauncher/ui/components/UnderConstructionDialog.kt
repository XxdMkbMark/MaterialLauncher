package cc.lanternmc.materiallauncher.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.lanternmc.materiallauncher.ui.theme.lightScheme

@Composable
fun UnderConstructionDialog(onConfirm: () -> Unit) {
    MaterialTheme(lightScheme) {
        AlertDialog(
            icon = { Icon(Icons.Rounded.Construction, "Under Construction", Modifier.size(48.dp)) },
            onDismissRequest = {},
            title = {
                Text("这是什么?")
            },
            text = {
                Text("当你看到这个提示时, 意味着此页面正在施工且暂无可正常使用的功能")
            },
            confirmButton = {
                TextButton(onClick = { onConfirm() }) {
                    Text("OK")
                }
            },
        )
    }
}