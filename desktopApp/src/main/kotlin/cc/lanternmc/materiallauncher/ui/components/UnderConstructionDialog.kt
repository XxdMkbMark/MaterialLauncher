package cc.lanternmc.materiallauncher.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.lanternmc.materiallauncher.generated.resources.Res
import cc.lanternmc.materiallauncher.generated.resources.cd_under_construction
import cc.lanternmc.materiallauncher.generated.resources.under_construction_body
import cc.lanternmc.materiallauncher.generated.resources.under_construction_ok
import cc.lanternmc.materiallauncher.generated.resources.under_construction_title
import cc.lanternmc.materiallauncher.ui.theme.lightScheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun UnderConstructionDialog(onConfirm: () -> Unit) {
    MaterialTheme(lightScheme) {
        AlertDialog(
            icon = { Icon(Icons.Rounded.Construction, stringResource(Res.string.cd_under_construction), Modifier.size(48.dp)) },
            onDismissRequest = {},
            title = {
                Text(stringResource(Res.string.under_construction_title))
            },
            text = {
                Text(stringResource(Res.string.under_construction_body))
            },
            confirmButton = {
                TextButton(onClick = { onConfirm() }) {
                    Text(stringResource(Res.string.under_construction_ok))
                }
            },
        )
    }
}