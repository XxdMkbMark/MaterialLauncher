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
package cc.lanternmc.materiallauncher.core.config

import java.io.File
import cc.lanternmc.materiallauncher.api.GlobalLaunchSettings
import cc.lanternmc.materiallauncher.core.util.Logger
import cc.lanternmc.materiallauncher.core.util.Toml

/**
 * 旧版 `config.toml`（[DownloadConfigStore] 持久化格式）→ 新版 `global.toml`
 * （[GlobalConfigStore] 持久化格式）的一次性迁移。
 *
 * 迁移策略（每次调用都会扫一遍旧文件，确保遗留配置被及时归档）：
 *   - 旧文件不存在：什么都不做
 *   - 旧文件存在但 global.toml 已存在：不重复迁移，直接把旧文件改名归档
 *     （保护用户已在 global.toml 中手动调整过的内容不被覆盖）
 *   - 旧文件存在且 global.toml 不存在：解析旧分区 [minecraft] / [java] /
 *     [account] / [download] / [launch]，字段映射为新的扁平 KV，写入 global.toml，
 *     再把旧文件改名归档
 *
 * 归档就是把旧文件改名为 `config.toml.migrated`（不删除，便于回滚）。
 * 迁移是幂等的：源文件改名后即视为已迁移，再次调用不会重复写入。
 */
object ConfigMigration {

    private const val LEGACY_NAME = "config.toml"
    private const val NEW_NAME = "global.toml"
    private const val BACKUP_SUFFIX = ".migrated"

    /** 数据目录级别一次性迁移。 */
    fun migrateIfNeeded(dataDir: String) {
        val dir = File(dataDir)
        if (!dir.isDirectory) return
        val oldFile = File(dir, LEGACY_NAME)
        val newFile = File(dir, NEW_NAME)
        if (!oldFile.isFile) return
        if (newFile.isFile) {
            // 新文件已存在则把旧文件改名归档，不重复迁移
            archive(oldFile)
            return
        }
        try {
            val settings = readLegacy(oldFile)
            GlobalConfigStore(newFile.absolutePath).save(settings)
            Logger.info("已将旧 config.toml 迁移到 global.toml（备份：${oldFile.name}$BACKUP_SUFFIX）")
            archive(oldFile)
        } catch (e: Exception) {
            Logger.error("迁移 config.toml 失败：${e.message}")
        }
    }

    private fun archive(file: File) {
        val backup = File(file.parentFile, file.name + BACKUP_SUFFIX)
        if (backup.exists()) backup.delete()
        if (!file.renameTo(backup)) {
            Logger.warn("无法归档旧配置文件：${file.absolutePath}")
        }
    }

    private fun readLegacy(file: File): GlobalLaunchSettings {
        val doc = Toml.parse(file.readText())
        val mc = doc.section("minecraft").values
        val java = doc.section("java").values
        val account = doc.section("account").values
        val download = doc.section("download").values
        val launch = doc.section("launch").values
        return GlobalLaunchSettings(
            minecraftPath = mc["path"].orEmpty(),
            javaPath = java["path"].orEmpty(),
            defaultAccountId = account["account_id"].orEmpty(),
            defaultUsername = account["username"].orEmpty().ifBlank { "TestUser" },
            defaultMaxMemory = "2048M",
            defaultJvmArgs = launch["jvm_args"].orEmpty(),
            defaultGameArgs = launch["game_args"].orEmpty(),
            downloadSource = download["mirror_source"] ?: "auto",
        )
    }
}