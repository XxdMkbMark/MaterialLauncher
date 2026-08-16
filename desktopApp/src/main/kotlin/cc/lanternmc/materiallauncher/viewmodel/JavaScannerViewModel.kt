package cc.lanternmc.materiallauncher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.lanternmc.materiallauncher.api.JavaInstallation
import cc.lanternmc.materiallauncher.core.LauncherBackend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class JavaScannerViewModel : ViewModel() {
    private val _javaList = MutableStateFlow<List<JavaInstallation>>(emptyList())
    val javaList: StateFlow<List<JavaInstallation>> = _javaList

    private val api = LauncherBackend()

    fun refreshJavaPaths() {
        viewModelScope.launch {
            val found = api.findJavaPaths()
            _javaList.value = found
        }
    }

    init {
        refreshJavaPaths() // 启动时自动扫描
    }
}