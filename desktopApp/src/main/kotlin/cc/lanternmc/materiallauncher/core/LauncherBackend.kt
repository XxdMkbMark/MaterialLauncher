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
package cc.lanternmc.materiallauncher.core

import java.io.File
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.swing.JFileChooser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import kotlinx.serialization.json.Json
import cc.lanternmc.materiallauncher.api.Account
import cc.lanternmc.materiallauncher.api.DownloadConfig
import cc.lanternmc.materiallauncher.api.DownloadProgress
import cc.lanternmc.materiallauncher.api.GameInstance
import cc.lanternmc.materiallauncher.api.JavaInstallation
import cc.lanternmc.materiallauncher.api.JavaReleaseInfo
import cc.lanternmc.materiallauncher.api.LauncherApi
import cc.lanternmc.materiallauncher.api.LauncherEvent
import cc.lanternmc.materiallauncher.api.LauncherEventBus
import cc.lanternmc.materiallauncher.api.LaunchRequest
import cc.lanternmc.materiallauncher.api.MinecraftVersionEntry
import cc.lanternmc.materiallauncher.api.RunningGameInfo
import cc.lanternmc.materiallauncher.core.auth.MicrosoftAuthService
import cc.lanternmc.materiallauncher.core.config.AppDataPathsResolver
import cc.lanternmc.materiallauncher.core.config.AuthStore
import cc.lanternmc.materiallauncher.core.config.DownloadConfigStore
import cc.lanternmc.materiallauncher.core.config.InstanceStore
import cc.lanternmc.materiallauncher.core.download.AssetDownloader
import cc.lanternmc.materiallauncher.core.download.DownloadMirrorSource
import cc.lanternmc.materiallauncher.core.download.JavaVersionService
import cc.lanternmc.materiallauncher.core.download.LibraryDownloader
import cc.lanternmc.materiallauncher.core.download.MinecraftVersionService
import cc.lanternmc.materiallauncher.core.download.MirrorUrlRewriter
import cc.lanternmc.materiallauncher.core.java.JavaFinder
import cc.lanternmc.materiallauncher.core.java.JavaIndexer
import cc.lanternmc.materiallauncher.core.launch.GameProcessManager
import cc.lanternmc.materiallauncher.core.launch.LauncherException
import cc.lanternmc.materiallauncher.core.launch.LaunchService
import cc.lanternmc.materiallauncher.core.model.ClientArtifact
import cc.lanternmc.materiallauncher.core.model.VersionJson
import cc.lanternmc.materiallauncher.core.util.ArchiveExtractor
import cc.lanternmc.materiallauncher.core.util.HttpUtil
import cc.lanternmc.materiallauncher.core.util.Logger
import cc.lanternmc.materiallauncher.core.util.Sha1
import cc.lanternmc.materiallauncher.core.util.Sha256
import cc.lanternmc.materiallauncher.core.util.compareMinecraftVersion

/**
 * 后端实现：实现 [LauncherApi]（前端调用）与 [LauncherEventBus]（前端订阅）。
 * 前端只依赖 [cc.lanternmc.materiallauncher.api]，绝不直接触碰本类以外的东西。
 */
class LauncherBackend : LauncherApi, LauncherEventBus {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val json = Json { ignoreUnknownKeys = true }

    private val paths = AppDataPathsResolver.resolve()
    internal val configStore = DownloadConfigStore(paths)
    internal val authStore = AuthStore(File(paths.directory, "auth.toml").absolutePath)
    internal val instanceStore = InstanceStore(File(paths.directory, "instances.toml").absolutePath)
    private val assetDownloader = AssetDownloader()
    private val libraryDownloader = LibraryDownloader()
    private val gameProcessManager = GameProcessManager()
    private val launchService = LaunchService(scope, assetDownloader, libraryDownloader, gameProcessManager)
    private val javaIndexer = JavaIndexer(paths.javaIndex, scope) { event -> _events.tryEmit(event) }
    private val downloadId = AtomicLong(0)
    private var microsoftLoginJob: Job? = null

    /** 正在进行的下载任务，key 形如 "minecraft:<id>" / "java:<id>"，用于去重与取消。 */
    internal val activeDownloads = ConcurrentHashMap<String, Job>()

    private val _events = MutableSharedFlow<LauncherEvent>(extraBufferCapacity = 1024)
    override val events: SharedFlow<LauncherEvent> = _events.asSharedFlow()

    // ---------- 日志 ----------

    override fun logInfo(message: String) = Logger.info("[ui] $message")
    override fun logWarn(message: String) = Logger.warn("[ui] $message")
    override fun logError(message: String) = Logger.error("[ui] $message")

    // ---------- 配置 ----------

    override suspend fun getDownloadConfig(): DownloadConfig = withContext(Dispatchers.IO) {
        configStore.load()
    }

    override suspend fun saveDownloadConfig(config: DownloadConfig) {
        withContext(Dispatchers.IO) { configStore.save(config) }
    }

    override suspend fun getDefaultMinecraftDir(): String = withContext(Dispatchers.IO) {
        configStore.defaultMinecraftDir()
    }

    override suspend fun getLauncherMinecraftDir(): String = withContext(Dispatchers.IO) {
        configStore.launcherMinecraftDir()
    }

    override suspend fun getLauncherJavaDir(): String = withContext(Dispatchers.IO) {
        configStore.launcherJavaDir()
    }

    // ---------- 已安装内容 ----------

    override suspend fun getInstalledMinecraftVersions(mcPath: String): List<String> = withContext(Dispatchers.IO) {
        val versionsDir = File(mcPath, "versions")
        if (!versionsDir.isDirectory) return@withContext emptyList()
        versionsDir.listFiles()
            ?.filter { it.isDirectory && File(it, "${it.name}.json").isFile }
            ?.map { it.name }
            ?.sortedWith { a, b -> compareMinecraftVersion(b, a) }
            ?: emptyList()
    }

    override suspend fun findJavaPaths(): List<JavaInstallation> = withContext(Dispatchers.IO) {
        javaIndexer.ensureQuickProbe()
        javaIndexer.start(force = false)
        javaIndexer.cachedResults()
    }

    override fun refreshJavaIndex(): Boolean = javaIndexer.start(force = true)

    override suspend fun deleteMinecraftVersion(gameDir: String, versionId: String): Boolean =
        withContext(Dispatchers.IO) {
            val versionDir = File(gameDir, "versions/$versionId")
            if (!versionDir.isDirectory) return@withContext false
            val ok = runCatching { versionDir.deleteRecursively() }.getOrDefault(false)
            if (ok) {
                Logger.info("已卸载 Minecraft 版本: $versionId")
            } else {
                Logger.warn("卸载 Minecraft 版本失败: $versionId")
            }
            ok
        }

    override suspend fun deleteJavaInstallation(javaPath: String): Boolean =
        withContext(Dispatchers.IO) {
            val home = File(javaPath).parentFile?.parentFile ?: return@withContext false
            val launcherJavaDir = configStore.launcherJavaDir()
            // 只允许删除启动器自管的 Java（位于启动器 java 目录下），避免误删系统 JDK
            val homePath = home.absolutePath
            if (!homePath.startsWith(launcherJavaDir)) {
                Logger.warn("拒绝删除非启动器管理的 Java: $homePath")
                return@withContext false
            }
            val ok = runCatching { home.deleteRecursively() }.getOrDefault(false)
            if (ok) {
                Logger.info("已卸载 Java: $homePath")
                // 触发一次索引刷新，让列表移除该条目
                javaIndexer.start(force = true)
            }
            ok
        }

    // ---------- 远程版本列表 ----------

    override suspend fun getMinecraftVersions(): List<MinecraftVersionEntry> =
        MinecraftVersionService.fetchVersions()

    override suspend fun getJavaVersions(): List<JavaReleaseInfo> =
        JavaVersionService.fetchVersions()

    // ---------- 下载 ----------

    override fun startMinecraftDownload(versionId: String) {
        startDownload("minecraft:$versionId") { downloadMinecraft(versionId) }
    }

    override fun startJavaDownload(javaVersionId: String) {
        startDownload("java:$javaVersionId") { downloadJava(javaVersionId) }
    }

    override fun cancelDownload(taskKey: String) {
        activeDownloads.remove(taskKey)?.let { it.cancel() }
    }

    /**
     * 以 taskKey 去重：同一目标的任务已在执行时直接忽略，避免重复启动多个下载协程。
     * 任务结束后（含取消 / 异常）自动从表中移除。
     */
    internal fun startDownload(taskKey: String, block: suspend () -> Unit) {
        if (activeDownloads.containsKey(taskKey)) return
        val job = scope.launch {
            try {
                block()
            } finally {
                activeDownloads.remove(taskKey)
            }
        }
        activeDownloads[taskKey] = job
    }

    private fun nextDownloadId(): String = "dl-${downloadId.incrementAndGet()}"

    private suspend fun downloadMinecraft(versionId: String) {
        val id = nextDownloadId()
        fun emitProgress(status: String, downloaded: Long = 0, total: Long = 0, error: String? = null) {
            _events.tryEmit(
                LauncherEvent.DownloadProgressEvent(
                    DownloadProgress(
                        id = id, type = "minecraft", item = versionId,
                        status = status, downloaded = downloaded, total = total, error = error,
                    ),
                ),
            )
        }

        emitProgress("fetching")
        val config = configStore.load()
        val mcPath = config.minecraft.path
        val source = DownloadMirrorSource.fromConfig(config.mirrorSource)
        try {
            val manifest = MinecraftVersionService.fetchVersions(source)
            val entry = manifest.firstOrNull { it.id == versionId }
                ?: throw LauncherException("未找到版本 $versionId")

            val versionDir = File(mcPath, "versions/$versionId")
            versionDir.mkdirs()

            emitProgress("downloading", 0, 0)
            // 版本 JSON 也按镜像策略回退
            val versionJsonBytes = downloadWithMirrorFallback(entry.url, source)
            val versionInfo = json.decodeFromString<VersionJson>(String(versionJsonBytes, Charsets.UTF_8))
            File(versionDir, "$versionId.json").writeBytes(versionJsonBytes)

            val client = versionInfo.downloads?.client
            if (client == null || client.url.isBlank()) {
                throw LauncherException("版本 $versionId 缺少 client 下载信息")
            }
            val clientJar = File(versionDir, "$versionId.jar")
            emitProgress("downloading", 0, client.size)
            // client jar 按镜像策略回退 + 校验失败自动重下
            downloadClientWithRetry(client, clientJar.absolutePath, source) { downloaded, total ->
                emitProgress("downloading", downloaded, if (total > 0) total else client.size)
            }
            if (!Sha1.isFileValid(clientJar.absolutePath, client.sha1, client.size)) {
                clientJar.delete()
                throw LauncherException("版本 $versionId 的 client 文件未通过 SHA-1/大小校验")
            }
            emitProgress("done", client.size, client.size)
        } catch (e: Exception) {
            Logger.error("Minecraft 下载失败: ${e.message}")
            emitProgress("error", error = e.message ?: "unknown error")
        }
    }

    /** 下载 client jar：镜像回退 + 校验失败自动重下（最多 3 次）。 */
    private suspend fun downloadClientWithRetry(
        client: ClientArtifact,
        dest: String,
        source: DownloadMirrorSource,
        onProgress: (Long, Long) -> Unit,
    ) {
        var lastError: Exception? = null
        for (attempt in 1..CLIENT_DOWNLOAD_ATTEMPTS) {
            for (candidate in MirrorUrlRewriter.candidates(client.url, source)) {
                try {
                    File(dest).delete()
                    HttpUtil.downloadFile(candidate, dest, onProgress)
                    if (Sha1.isFileValid(dest, client.sha1, client.size)) return
                    File(dest).delete()
                } catch (e: Exception) {
                    lastError = e
                }
            }
            Logger.warn("client 下载失败（第 $attempt 次，将重试）: ${client.url}")
        }
        throw lastError ?: IllegalStateException("client 下载失败: ${client.url}")
    }

    private suspend fun downloadWithMirrorFallback(
        url: String,
        source: DownloadMirrorSource,
    ): ByteArray {
        var lastError: Exception? = null
        for (candidate in MirrorUrlRewriter.candidates(url, source)) {
            try {
                return HttpUtil.getBytes(candidate)
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("所有下载源均失败: $url")
    }

    private suspend fun downloadJava(javaVersionId: String) {
        val id = nextDownloadId()
        fun emitProgress(status: String, downloaded: Long = 0, total: Long = 0, error: String? = null) {
            _events.tryEmit(
                LauncherEvent.DownloadProgressEvent(
                    DownloadProgress(
                        id = id, type = "java", item = javaVersionId,
                        status = status, downloaded = downloaded, total = total, error = error,
                    ),
                ),
            )
        }

        emitProgress("fetching")
        val javaDir = configStore.load().java.path
        try {
            val selected = JavaVersionService.fetchVersions().firstOrNull { it.id == javaVersionId }
                ?: throw LauncherException("未找到 Java 版本 $javaVersionId")

            val destDir = File(javaDir, "jdk-${selected.version}")
            val archiveName = selected.downloadUrl.substringAfterLast('/')
            val archivePath = File(javaDir, archiveName)
            File(javaDir).mkdirs()

            emitProgress("downloading", 0, selected.downloadSize)
            HttpUtil.downloadFile(selected.downloadUrl, archivePath.absolutePath) { downloaded, total ->
                emitProgress("downloading", downloaded, if (total > 0) total else selected.downloadSize)
            }
            // 完整性校验：先比大小，再比 SHA-256（Adoptium 提供 checksum）。
            if (archivePath.length() != selected.downloadSize) {
                archivePath.delete()
                throw LauncherException("Java 归档大小校验失败: ${archivePath.length()} != ${selected.downloadSize}")
            }
            if (selected.sha256.isNotBlank() && !Sha256.isFileValid(archivePath.absolutePath, selected.sha256)) {
                archivePath.delete()
                throw LauncherException("Java 归档 SHA-256 校验失败")
            }
            emitProgress("extracting", selected.downloadSize, selected.downloadSize)

            ArchiveExtractor.extractArchive(archivePath.absolutePath, destDir.absolutePath)
            archivePath.delete()

            val javaBin = locateJavaBinary(destDir.absolutePath)
            if (javaBin != null) {
                JavaFinder.probeJavaVersion(javaBin)
                    ?.let { Logger.info("Java 下载完成: ${it.path} (${it.version})") }
            }
            emitProgress("done", selected.downloadSize, selected.downloadSize)
        } catch (e: Exception) {
            Logger.error("Java 下载失败: ${e.message}")
            emitProgress("error", error = e.message ?: "unknown error")
        }
    }

    private fun locateJavaBinary(extractDir: String): String? {
        val direct = File(extractDir, "bin/${JavaFinder.javaExecutableName()}")
        if (direct.isFile) return direct.absolutePath
        File(extractDir).listFiles()?.forEach { child ->
            if (child.isDirectory) {
                val candidate = File(child, "bin/${JavaFinder.javaExecutableName()}")
                if (candidate.isFile) return candidate.absolutePath
            }
        }
        return null
    }

    // ---------- 游戏启动 ----------

    override suspend fun launchMinecraft(request: LaunchRequest): Int = withContext(Dispatchers.IO) {
        val (token, uuid) = ensureValidLaunchAccount(request)
        val config = configStore.load()
        val source = DownloadMirrorSource.fromConfig(config.mirrorSource)
        val extraJvmArgs = splitArgs(config.jvmArgs)
        val extraGameArgs = splitArgs(config.gameArgs)
        launchService.fullLaunch(
            javaPath = request.javaPath,
            gameDir = request.gameDir,
            versionId = request.versionId,
            username = request.username,
            maxMem = request.maxMemory,
            isolateVersion = request.isolateVersion,
            accessToken = token,
            uuid = uuid,
            userType = request.userType,
            source = source,
            extraJvmArgs = extraJvmArgs,
            extraGameArgs = extraGameArgs,
            emit = { event -> _events.tryEmit(event) },
        )
    }

    /** 按空白拆分自定义参数（允许引号分组）。 */
    private fun splitArgs(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        val result = mutableListOf<String>()
        val regex = Regex("\"([^\"]*)\"|(\\S+)")
        for (match in regex.findAll(raw)) {
            result.add(match.groupValues[1].ifBlank { match.groupValues[2] })
        }
        return result
    }

    companion object {
        /** token 剩余有效期低于该值即视为"即将过期"，触发自动续期。 */
        internal val TOKEN_RENEW_LEAD = 5 * 60 * 1000L

        /** client jar 最大下载尝试次数。 */
        private const val CLIENT_DOWNLOAD_ATTEMPTS = 3
    }

    /**
     * 正版 / 离线账户的严格区分与在线 token 自动续期。
     *
     * - 离线（userType=legacy）：放行，返回占位 token。
     * - 正版（userType=msa）：必须能通过 accountId / uuid 定位到已登录账户，且
     *   token 不得为占位符；若 token 已过期或 5 分钟内将过期，则用 refresh token
     *   自动续期并回写账户库。
     *
     * @return 用于本次启动的 (accessToken, uuid)
     */
    internal suspend fun ensureValidLaunchAccount(request: LaunchRequest): Pair<String, String> {
        val isOnline = request.userType.equals("msa", ignoreCase = true)
        if (!isOnline) {
            return request.accessToken to request.uuid
        }
        val accounts = authStore.load()
        val account = (request.accountId.takeIf { it.isNotBlank() }
            ?.let { id -> accounts.firstOrNull { it.id == id } })
            ?: accounts.firstOrNull { it.uuid == request.uuid && it.type == "online" }
            ?: throw LauncherException("未找到对应的正版账户，请先在账户页登录微软账号")
        if (request.accessToken.isBlank() || request.accessToken == "0") {
            throw LauncherException("正版账户缺少有效的登录令牌，请重新登录")
        }
        val expiresSoon = account.msExpiresAt > 0 &&
            System.currentTimeMillis() >= account.msExpiresAt - TOKEN_RENEW_LEAD
        if (expiresSoon && account.refreshToken.isNotBlank()) {
            try {
                val refreshed = MicrosoftAuthService.refreshAccount(account)
                authStore.add(refreshed)
                _events.tryEmit(LauncherEvent.AccountsChanged(authStore.load()))
                Logger.info("正版账户 token 即将过期，已自动续期: ${refreshed.username}")
                return refreshed.accessToken to refreshed.uuid
            } catch (e: Exception) {
                Logger.error("正版账户自动续期失败，改用已缓存 token: ${e.message}")
            }
        }
        val cachedToken = account.accessToken.takeIf { it.isNotBlank() } ?: request.accessToken
        val cachedUuid = account.uuid.takeIf { it.isNotBlank() } ?: request.uuid
        return cachedToken to cachedUuid
    }

    /**
     * 根据版本所需 Java 主版本号，在已安装 Java 中解析最合适的启动 Java。
     */
    override suspend fun resolveLaunchJava(gameDir: String, versionId: String, preferred: String): String =
        withContext(Dispatchers.IO) {
            val required = requiredJavaMajor(gameDir, versionId)
            val candidates = javaIndexer.cachedResults()
            val chosen = resolveJavaForRequired(required, candidates, preferred)
            if (chosen != preferred) {
                val chosenVersion = candidates.firstOrNull { it.path == chosen }?.version ?: chosen
                val preferredVersion = candidates.firstOrNull { it.path == preferred }?.version ?: preferred
                Logger.warn("启动 $versionId 需要 Java $required，已自动切换: $preferredVersion -> $chosenVersion")
            }
            chosen
        }

    private fun requiredJavaMajor(gameDir: String, versionId: String): Int = runCatching {
        val file = File(gameDir, "versions/$versionId/$versionId.json")
        val v = json.decodeFromString<VersionJson>(file.readText())
        v.javaVersion?.majorVersion ?: 8
    }.getOrDefault(8)

    private fun javaMajorOfPath(path: String): Int =
        JavaFinder.probeJavaVersion(path)?.version?.let { JavaFinder.javaFeatureVersion(it) } ?: 8

    /**
     * 老版本（需 Java 8 / LWJGL2）优先选 8；新版本优先选 ≥ 需求的最低可用版本。
     */
    private fun resolveJavaForRequired(required: Int, javas: List<JavaInstallation>, preferred: String): String {
        val preferredMajor = javaMajorOfPath(preferred)
        if (required <= 8) {
            if (preferredMajor == 8) return preferred
            javas.firstOrNull { JavaFinder.javaFeatureVersion(it.version) == 8 }?.let { return it.path }
            val lowest = javas.minByOrNull { JavaFinder.javaFeatureVersion(it.version) }
            if (lowest != null && preferredMajor > JavaFinder.javaFeatureVersion(lowest.version)) return lowest.path
            return preferred
        }
        if (preferredMajor >= required) return preferred
        val best = javas.mapNotNull { j ->
            val major = JavaFinder.javaFeatureVersion(j.version)
            if (major >= required) major to j else null
        }.minByOrNull { it.first }?.second?.path
        return best ?: preferred
    }

    // ---------- 游戏进程管理 ----------

    override fun listRunningGames(): List<RunningGameInfo> = gameProcessManager.list()

    override fun stopGame(pid: Int): Boolean = gameProcessManager.stop(pid)

    override fun killGame(pid: Int): Boolean = gameProcessManager.kill(pid)

    // ---------- 内存建议 ----------

    override fun suggestMaxMemoryMb(): Int = gameProcessManager.suggestMaxMemoryMb()

    // ---------- 多实例管理 ----------

    override suspend fun listInstances(): List<GameInstance> = withContext(Dispatchers.IO) {
        instanceStore.load()
    }

    override suspend fun createInstance(name: String, versionId: String): GameInstance =
        withContext(Dispatchers.IO) {
            val instance = InstanceStore.newInstance(
                name = name,
                versionId = versionId,
                baseDir = paths.directory,
            )
            instanceStore.add(instance)
            Logger.info("已创建实例: ${instance.name} (${instance.id.take(8)}), 版本 $versionId")
            instance
        }

    override suspend fun saveInstance(instance: GameInstance) {
        withContext(Dispatchers.IO) { instanceStore.add(instance) }
    }

    override suspend fun deleteInstance(instanceId: String): Boolean = withContext(Dispatchers.IO) {
        val instance = instanceStore.load().firstOrNull { it.id == instanceId }
            ?: return@withContext false
        instanceStore.remove(instanceId)
        // 同时删除实例独立游戏目录（含存档）
        runCatching { File(instance.gameDir).deleteRecursively() }
        Logger.info("已删除实例: ${instance.name}")
        true
    }

    override suspend fun launchInstance(
        instanceId: String,
        username: String,
        accessToken: String,
        uuid: String,
        userType: String,
    ): Int = withContext(Dispatchers.IO) {
        val instance = instanceStore.load().firstOrNull { it.id == instanceId }
            ?: throw LauncherException("未找到实例 $instanceId")
        if (instance.versionId.isBlank()) throw LauncherException("实例 ${instance.name} 未设置版本")
        val config = configStore.load()
        val source = DownloadMirrorSource.fromConfig(config.mirrorSource)
        val (token, effectiveUuid) = ensureValidLaunchAccount(
            LaunchRequest(
                javaPath = "",
                gameDir = instance.gameDir,
                versionId = instance.versionId,
                username = username,
                maxMemory = instance.maxMemory,
                isolateVersion = false,
                accessToken = accessToken,
                uuid = uuid,
                userType = userType,
            ),
        )
        val javaPath = instance.javaPath.ifBlank {
            resolveLaunchJava(instance.gameDir, instance.versionId, javasFirstPath())
        }
        val extraJvmArgs = splitArgs(instance.jvmArgs)
        val extraGameArgs = splitArgs(config.gameArgs)
        val pid = launchService.fullLaunch(
            javaPath = javaPath,
            gameDir = instance.gameDir,
            versionId = instance.versionId,
            username = username,
            maxMem = instance.maxMemory,
            isolateVersion = false,
            accessToken = token,
            uuid = effectiveUuid,
            userType = userType,
            source = source,
            extraJvmArgs = extraJvmArgs,
            extraGameArgs = extraGameArgs,
            emit = { event -> _events.tryEmit(event) },
        )
        // 记录最后启动时间
        instanceStore.add(instance.copy(lastLaunched = OffsetDateTime.now().toString()))
        pid
    }

    /** 取已索引 Java 中的第一个路径作为兜底。 */
    private fun javasFirstPath(): String = javaIndexer.cachedResults().firstOrNull()?.path.orEmpty()

    // ---------- 账户 ----------

    override suspend fun getAccounts(): List<Account> = withContext(Dispatchers.IO) {
        authStore.load()
    }

    override suspend fun addOfflineAccount(username: String): Account = withContext(Dispatchers.IO) {
        val name = username.trim().ifBlank { "TestUser" }
        val account = Account(
            id = UUID.randomUUID().toString(),
            type = "offline",
            username = name,
            uuid = "00000000-0000-0000-0000-000000000000",
            userType = "legacy",
            lastRefreshed = OffsetDateTime.now().toString(),
        )
        authStore.add(account)
        _events.tryEmit(LauncherEvent.AccountsChanged(authStore.load()))
        account
    }

    override suspend fun removeAccount(accountId: String) {
        withContext(Dispatchers.IO) { authStore.remove(accountId) }
        _events.tryEmit(LauncherEvent.AccountsChanged(authStore.load()))
    }

    override suspend fun refreshAccount(accountId: String): Account? = withContext(Dispatchers.IO) {
        val accounts = authStore.load().toMutableList()
        val idx = accounts.indexOfFirst { it.id == accountId }
        if (idx < 0) return@withContext null
        val refreshed = MicrosoftAuthService.refreshAccount(accounts[idx])
        accounts[idx] = refreshed
        authStore.save(accounts)
        _events.tryEmit(LauncherEvent.AccountsChanged(authStore.load()))
        refreshed
    }

    /**
     * 启动微软设备码登录：设备码 → 轮询 → 换取 Minecraft 账户。
     * 进度通过事件推送：`AuthDeviceCodeReceived` / `AuthStatusChanged` / `AuthCompleted` / `AuthFailed`。
     */
    override fun startMicrosoftLogin() {
        if (microsoftLoginJob?.isActive == true) return
        microsoftLoginJob = scope.launch {
            try {
                val deviceCode = MicrosoftAuthService.requestDeviceCode()
                _events.tryEmit(LauncherEvent.AuthDeviceCodeReceived(deviceCode))
                val token = MicrosoftAuthService.pollForToken(deviceCode) { status ->
                    _events.tryEmit(LauncherEvent.AuthStatusChanged(status))
                }
                val account = MicrosoftAuthService.exchangeToAccount(token.accessToken, token.refreshToken)
                withContext(Dispatchers.IO) { authStore.add(account) }
                _events.tryEmit(LauncherEvent.AuthCompleted(account))
                _events.tryEmit(LauncherEvent.AccountsChanged(authStore.load()))
            } catch (e: Exception) {
                Logger.error("Microsoft 登录失败: ${e.message}")
                _events.tryEmit(LauncherEvent.AuthFailed(e.message ?: "unknown error"))
            }
        }
    }

    // ---------- 系统集成 ----------

    override suspend fun openDirectoryDialog(title: String): String? = withContext(Dispatchers.Main) {
        val chooser = JFileChooser().apply {
            dialogTitle = title
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            isAcceptAllFileFilterUsed = false
        }
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile?.absolutePath
        } else {
            null
        }
    }
}
