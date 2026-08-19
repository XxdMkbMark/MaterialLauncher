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
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * 配置文件写入共用的 IO 工具，统一处理"临时文件 → 原子替换"的写入模式，
 * 避免每个 store 各自重复一份 delete-then-rename 逻辑。
 *
 * 安全保证：
 *   - 写入 [content] 到 `<target>.tmp`；写入失败抛 IOException，原文件不受影响
 *   - 使用 [Files.move] + `REPLACE_EXISTING` 替换目标；与 delete-then-rename
 *     不同的是，失败时**不会**留下目标文件被删除而临时文件还没替换上去的空窗
 *   - 优先尝试 `ATOMIC_MOVE`（在同一文件系统上由 OS 保证原子性），
 *     不支持时（FAT32、部分 Windows 配置）回退到普通 `REPLACE_EXISTING`
 */
object ConfigIO {

    /**
     * 把 [content] 原子地写入 [target]。
     *
     * @throws java.io.IOException 临时文件写入或最终 rename 失败时抛出，
     *         此时 [target] 保持原状（不会被清空）
     */
    fun writeAtomically(target: File, content: String) {
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeText(content)
        try {
            try {
                Files.move(
                    tmp.toPath(),
                    target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (e: AtomicMoveNotSupportedException) {
                // FAT32 / 跨文件系统 / 部分 Windows 配置不支持 ATOMIC_MOVE；
                // 回退到普通 REPLACE_EXISTING，仍比 delete-then-rename 安全
                // （不会出现目标被删而临时文件还没就位 的状态窗口）。
                Files.move(
                    tmp.toPath(),
                    target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }
        } catch (e: Exception) {
            tmp.delete()
            throw e
        }
    }
}