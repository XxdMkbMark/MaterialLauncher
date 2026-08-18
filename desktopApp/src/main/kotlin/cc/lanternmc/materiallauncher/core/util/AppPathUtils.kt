/*
 * Copyright (C) 2026 Mark <github@xxdmkbmark> & Pidan <github@bretren>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cc.lanternmc.materiallauncher.core.util

import java.io.File
import java.nio.file.Paths

/**
 * 启动器自身路径解析工具。
 *
 * 三策略回退（命中即返回）:
 * 1. ProcessHandle.command() - 适用于 jpackage / Compose Desktop 原生打包
 * 2. protectionDomain.codeSource - 适用于独立 Jar 包
 * 3. user.dir - 适用于 IDE 直接运行（兜底）
 *
 * 注意排除 java.exe / javaw.exe 避免在 IDE / java -jar 模式下误判为 JDK 目录；
 * 对 macOS App Bundle 嵌套路径做了特殊处理。
 */
object AppPathUtils {

    /**
     * 解析启动器可执行文件所在目录（绝对路径）。
     * 三策略回退，都失败则返回当前工作目录。
     */
    fun getAppDir(): File {
        return runCatching {
            // 策略 1: 原生打包进程 (jpackage / Compose Desktop Native)
            ProcessHandle.current().info().command().orElse(null)?.let { cmd ->
                val exeFile = File(cmd).absoluteFile
                val exeName = exeFile.name.lowercase()

                // 排除 java/javaw 通用进程,避免在 IDE 和 java -jar 下误判为 JDK 目录
                val isGenericJavaLauncher = exeName in listOf("java", "java.exe", "javaw.exe", "javaw")
                if (exeFile.exists() && !isGenericJavaLauncher) {
                    var dir = exeFile.parentFile
                    // macOS App Bundle 嵌套: MyApp.app/Contents/MacOS/MyApp -> .app 同级
                    if (dir?.name == "MacOS" && dir.parentFile?.name == "Contents") {
                        dir = dir.parentFile?.parentFile?.parentFile ?: dir
                    }
                    return@runCatching dir!!
                }
            }

            // 策略 2: Jar 包或类文件所在位置 (java -jar 模式)
            val codeSourceLocation = AppPathUtils::class.java.protectionDomain?.codeSource?.location
            if (codeSourceLocation != null) {
                val file = Paths.get(codeSourceLocation.toURI()).toFile().absoluteFile
                return@runCatching if (file.isFile) file.parentFile else file
            }

            null
        }.getOrNull()
            // 策略 3: IDE 本地开发兜底 (当前工作目录)
            ?: File(System.getProperty("user.dir")).absoluteFile
    }
}
