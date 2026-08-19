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
import java.time.OffsetDateTime
import java.util.UUID
import cc.lanternmc.materiallauncher.api.GameInstance
import cc.lanternmc.materiallauncher.core.util.Logger
import cc.lanternmc.materiallauncher.core.util.Toml

/**
 * instances.toml 多实例持久化：
 *   [[instance]]           强类型字段（name/version/gameDir/java/maxMemory/jvmArgs/...）
 *   [[instance_extra]]     String→String KV 扩展（extras）
 *
 * extras 以独立数组表 `[[instance_extra]]` 持久化（每行带 `instance_id` 引用），
 * 避免在 `[[instance]]` 内嵌子表带来的 TOML 兼容性问题（数组表内不允许子表）。
 */
class InstanceStore(private val path: String) {

    fun load(): List<GameInstance> {
        val file = File(path)
        if (!file.isFile) return emptyList()
        val doc = runCatching { Toml.parse(file.readText()) }.getOrNull() ?: return emptyList()

        val instances = doc.section("instance").items.mapNotNull { item ->
            val id = item["id"] ?: return@mapNotNull null
            GameInstance(
                id = id,
                name = item["name"].orEmpty(),
                versionId = item["version_id"].orEmpty(),
                gameDir = item["game_dir"].orEmpty(),
                javaPath = item["java_path"].orEmpty(),
                maxMemory = item["max_memory"] ?: "2048M",
                jvmArgs = item["jvm_args"].orEmpty(),
                createdAt = item["created_at"].orEmpty(),
                lastLaunched = item["last_launched"].orEmpty(),
                extras = emptyMap(),
            )
        }
        val extrasByInstance = doc.section("instance_extra").items
            // 丢弃 instance_id 或 key 为空的损坏条目，避免它们聚到空 id 桶里
            // 后污染某个真实实例的 extras map，或写出一个空字符串 key 覆盖正常值
            .filter { it["instance_id"].orEmpty().isNotBlank() && it["key"].orEmpty().isNotBlank() }
            .groupBy { it["instance_id"]!! }
            .mapValues { entry -> entry.value.associate { it["key"]!! to it["value"].orEmpty() } }

        return instances.map { inst ->
            val merged = extrasByInstance[inst.id].orEmpty()
            if (merged.isEmpty()) inst else inst.copy(extras = merged)
        }
    }

    fun save(instances: List<GameInstance>) {
        val content = buildString {
            appendLine("# Material Launcher instances. Generated automatically.")
            appendLine("version = 1")
            for (instance in instances) {
                appendLine()
                appendLine("[[instance]]")
                appendLine("id = ${Toml.quote(instance.id)}")
                appendLine("name = ${Toml.quote(instance.name)}")
                appendLine("version_id = ${Toml.quote(instance.versionId)}")
                appendLine("game_dir = ${Toml.quote(instance.gameDir)}")
                appendLine("java_path = ${Toml.quote(instance.javaPath)}")
                appendLine("max_memory = ${Toml.quote(instance.maxMemory)}")
                appendLine("jvm_args = ${Toml.quote(instance.jvmArgs)}")
                appendLine("created_at = ${Toml.quote(instance.createdAt)}")
                appendLine("last_launched = ${Toml.quote(instance.lastLaunched)}")
                for ((k, v) in instance.extras) {
                    appendLine()
                    appendLine("[[instance_extra]]")
                    appendLine("instance_id = ${Toml.quote(instance.id)}")
                    appendLine("key = ${Toml.quote(k)}")
                    appendLine("value = ${Toml.quote(v)}")
                }
            }
        }
        ConfigIO.writeAtomically(File(path), content)
    }

    fun add(instance: GameInstance): List<GameInstance> {
        val instances = load().toMutableList()
        val idx = instances.indexOfFirst { it.id == instance.id }
        if (idx >= 0) instances[idx] = instance else instances.add(instance)
        save(instances)
        return instances
    }

    fun remove(id: String): List<GameInstance> {
        val instances = load().filterNot { it.id == id }
        save(instances)
        return instances
    }

    companion object {
        /** 创建实例：分配 UUID、生成独立的游戏目录（启动器数据目录下 instances/<name>-<shortId>）。 */
        fun newInstance(
            name: String,
            versionId: String,
            baseDir: String,
            javaPath: String = "",
            maxMemory: String = "2048M",
            jvmArgs: String = "",
        ): GameInstance {
            val id = UUID.randomUUID().toString()
            val shortId = id.take(8)
            val safeName = name.trim().ifBlank { "Instance" }.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]"), "_")
            val gameDir = File(File(baseDir, "instances"), "$safeName-$shortId").absolutePath
            return GameInstance(
                id = id,
                name = name.trim().ifBlank { "Instance" },
                versionId = versionId,
                gameDir = gameDir,
                javaPath = javaPath,
                maxMemory = maxMemory,
                jvmArgs = jvmArgs,
                createdAt = OffsetDateTime.now().toString(),
            )
        }
    }
}