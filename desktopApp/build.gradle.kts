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
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.serialization)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.material3)
    implementation(libs.compose.materialIconsExtended)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.kotlinx.serializationJson)
    implementation(libs.commons.compress)
    implementation(libs.compose.uiToolingPreview)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material3.desktop)

    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.junit)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
}

compose.desktop {
    application {
        mainClass = "cc.lanternmc.materiallauncher.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "cc.lanternmc.materiallauncher"
            packageVersion = "1.0.0"

            // jlink 精简运行时默认不会自动包含 java.net.http 模块，
            // 缺它会导致打包版打开下载页时 NoClassDefFoundError 崩溃。
            modules("java.net.http", "java.management", "java.sql")
        }
    }
}