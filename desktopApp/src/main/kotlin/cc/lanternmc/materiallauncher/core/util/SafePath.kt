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

/**
 * 路径安全工具：所有来自外部输入（版本 JSON、manifest、用户输入）的路径片段
 * 在拼接到本地文件路径前必须经过校验，防止目录穿越（`..`）写入/删除越界。
 */
object SafePath {

    /**
     * 校验一个来自网络 JSON 的相对路径片段（如 library 的 path、asset 的 id）
     * 是否安全：不允许绝对路径、不允许 `..` 穿越、不允许盘符。
     *
     * @return 合法时返回 true
     */
    fun isSafeRelativePath(rel: String): Boolean {
        if (rel.isBlank()) return false
        if (rel.startsWith("/") || rel.startsWith("\\")) return false
        if (rel.matches(Regex("^[A-Za-z]:.*"))) return false // 盘符如 C:\
        val segments = rel.split('/', '\\')
        // 不允许空段之外的穿越段
        for (seg in segments) {
            if (seg == "..") return false
        }
        return true
    }

    /** 校验 asset hash（应为 40 位小写 hex）；拒绝任何路径字符。 */
    fun isSafeAssetHash(hash: String): Boolean =
        hash.matches(Regex("[0-9a-f]{40}"))
}
