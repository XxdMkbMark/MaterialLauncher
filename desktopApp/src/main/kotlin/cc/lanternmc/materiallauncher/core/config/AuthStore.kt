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
import cc.lanternmc.materiallauncher.api.Account
import cc.lanternmc.materiallauncher.core.util.Toml

/**
 * auth.toml 多账户持久化。
 * 结构：根键 version，`account[]` 数组表。
 */
class AuthStore(private val path: String) {

    fun load(): List<Account> {
        val file = File(path)
        if (!file.isFile) return emptyList()
        val doc = runCatching { Toml.parse(file.readText()) }.getOrNull() ?: return emptyList()
        val accountSection = doc.section("account")
        return accountSection.items.mapNotNull { item ->
            val id = item["id"] ?: return@mapNotNull null
            Account(
                id = id,
                type = item["type"] ?: "offline",
                username = item["username"].orEmpty(),
                uuid = item["uuid"].orEmpty(),
                accessToken = item["access_token"].orEmpty(),
                userType = item["user_type"] ?: "legacy",
                msToken = item["ms_token"].orEmpty(),
                refreshToken = item["refresh_token"].orEmpty(),
                msExpiresAt = item["ms_expires_at"]?.toLongOrNull() ?: 0,
                lastRefreshed = item["last_refreshed"].orEmpty(),
            )
        }
    }

    fun save(accounts: List<Account>) {
        val content = buildString {
            appendLine("# Material Launcher accounts. Generated automatically.")
            appendLine("version = 1")
            for (account in accounts) {
                appendLine()
                appendLine("[[account]]")
                appendLine("id = ${Toml.quote(account.id)}")
                appendLine("type = ${Toml.quote(account.type)}")
                appendLine("username = ${Toml.quote(account.username)}")
                appendLine("uuid = ${Toml.quote(account.uuid)}")
                appendLine("access_token = ${Toml.quote(account.accessToken)}")
                appendLine("user_type = ${Toml.quote(account.userType)}")
                appendLine("ms_token = ${Toml.quote(account.msToken)}")
                appendLine("refresh_token = ${Toml.quote(account.refreshToken)}")
                appendLine("ms_expires_at = ${account.msExpiresAt}")
                appendLine("last_refreshed = ${Toml.quote(account.lastRefreshed)}")
            }
        }
        File(path).parentFile?.mkdirs()
        val tmp = File("$path.tmp")
        tmp.writeText(content)
        val target = File(path)
        if (target.exists()) target.delete()
        tmp.renameTo(target)
    }

    fun add(account: Account): List<Account> {
        val accounts = load().toMutableList()
        val idx = accounts.indexOfFirst { it.id == account.id }
        if (idx >= 0) accounts[idx] = account else accounts.add(account)
        save(accounts)
        return accounts
    }

    fun remove(id: String): List<Account> {
        val accounts = load().filterNot { it.id == id }
        save(accounts)
        return accounts
    }
}
