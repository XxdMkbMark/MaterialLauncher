package cc.lanternmc.materiallauncher.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JavaSettingsPage(navController: NavHostController) {
    val javaList = listOf("17.0", "21.0", "25.0", "1.8.0")

    var sliderPosition by remember { mutableStateOf(0f..16384f) }    // 内存滑块
    var Xms = 0
    var Xmx = 0

    var expanded by remember { mutableStateOf(false) }    // 状态
    var selectedOption by remember { mutableStateOf(javaList[0]) }
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Row {

            }
        }
        Box(modifier = Modifier.fillMaxSize().padding(18.dp,0.dp,0.dp,0.dp)) {
            Column {
                Text(text = "全局Java版本", modifier = Modifier.padding(4.dp,10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        // 文本框
                        TextField(
                            value = selectedOption,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor()     // 将文本框与菜单锚定
                        )

                        // 下拉菜单
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            javaList.forEach { java ->
                                DropdownMenuItem(
                                    text = { Text(java) },
                                    onClick = {
                                        selectedOption = java
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    Button(
                        onClick = {},
                        modifier = Modifier.padding(12.dp,0.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End)
                        ) {
                            Icon(imageVector = Icons.Rounded.Refresh, contentDescription = "")
                            Text("重新扫描")
                        }
                    }
                }
                Text(text = "全局JVM内存分配", modifier = Modifier.padding(4.dp,26.dp,0.dp,4.dp))
                Column {
                    RangeSlider(
                        value = sliderPosition,
                        onValueChange = { range -> sliderPosition = range },
                        valueRange = 0f..16384f,
                        onValueChangeFinished = {
                            // launch some business logic update with the state you hold
                            // viewModel.updateSelectedSliderValue(sliderPosition)
                        },
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Xms = sliderPosition.start.toInt()
                        Xmx = sliderPosition.endInclusive.toInt()
                        if (Xms == 0) {
                            Text(text = "最小内存: 不指定      最大内存: ${Xmx}MB")
                        } else {
                            Text(text = "最小内存: ${Xms}MB      最大内存: ${Xmx}MB")
                        }

                    }
                }
            }
        }
    }
}