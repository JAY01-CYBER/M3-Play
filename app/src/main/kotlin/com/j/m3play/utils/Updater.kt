package com.j.m3play.utils

import androidx.datastore.preferences.core.edit
import com.j.m3play.App
import com.j.m3play.BuildConfig
import com.j.m3play.constants.GitHubReleasesEtagKey
import com.j.m3play.constants.GitHubReleasesFingerprintKey
import com.j.m3play.constants.GitHubReleasesJsonKey
import com.j.m3play.constants.GitHubReleasesLastCheckedAtKey
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import org.json.JSONArray

data class GitCommit(
    val sha: String,
    val message: String,
    val author: String,
    val date: String,
    val url: String
)

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String
)

data class ReleaseInfo(
    val tagName: String,
    val name: String,
    val body: String?,
    val publishedAt: String,
    val htmlUrl: String,
    val assets: List<ReleaseAsset> = emptyList()
)

private data class ReleasesNetworkResult(
    val status: HttpStatusCode,
    val body: String?,
    val etag: String?,
)

object Updater {
    private val client = HttpClient()

    private const val RELEASE_CACHE_CHECK_INTERVAL_MS = 0L

    var lastCheckTime = -1L
        private set

    private data class SemVer(
        val major: Int,
        val minor: Int,
        val patch: Int,
        val preRelease: List<String>,
    ) : Comparable<SemVer> {
        override fun compareTo(other: SemVer): Int {
            compareValuesBy(this, other, SemVer::major, SemVer::minor, SemVer::patch).let {
                if (it != 0) return it
            }

            val aStable = preRelease.isEmpty()
            val bStable = other.preRelease.isEmpty()
            if (aStable != bStable) return if (aStable) 1 else -1

            val count = minOf(preRelease.size, other.preRelease.size)
            for (i in 0 until count) {
                val a = preRelease[i]
                val b = other.preRelease[i]
                val aNum = a.toLongOrNull()
                val bNum = b.toLongOrNull()

                val c = when {
                    aNum != null && bNum != null -> aNum.compareTo(bNum)
                    aNum != null -> -1
                    bNum != null -> 1
                    else -> a.compareTo(b)
                }
                if (c != 0) return c
            }
            return preRelease.size.compareTo(other.preRelease.size)
        }

        fun normalizedName(): String =
            buildString {
                append(major).append('.').append(minor).append('.').append(patch)
                if (preRelease.isNotEmpty()) {
                    append('-').append(preRelease.joinToString("."))
                }
            }
    }

    private val semVerRegex =
        Regex("""(?i)\bv?(\d+)\.(\d+)\.(\d+)(?:-([0-9A-Za-z.-]+))?(?:\+[0-9A-Za-z.-]+)?\b""")

    private fun parseSemVerOrNull(value: String): SemVer? {
        val match = semVerRegex.find(value) ?: return null
        return SemVer(
            major = match.groupValues[1].toIntOrNull() ?: return null,
            minor = match.groupValues[2].toIntOrNull() ?: return null,
            patch = match.groupValues[3].toIntOrNull() ?: return null,
            preRelease = match.groupValues[4]
                .takeIf { it.isNotBlank() }
                ?.split('.')
                ?.filter { it.isNotBlank() }
                ?: emptyList()
        )
    }

    private fun parseReleaseVersion(release: ReleaseInfo): SemVer? =
        parseSemVerOrNull(release.tagName)
            ?: parseSemVerOrNull(release.name)

    internal fun isSameVersion(a: String, b: String): Boolean {
        val av = parseSemVerOrNull(a)
        val bv = parseSemVerOrNull(b)
        return if (av != null && bv != null) av == bv
        else a.trim().removePrefix("v") == b.trim().removePrefix("v")
    }

    internal fun isUpdateAvailable(remoteVersion: String, installedVersion: String): Boolean {
        val remote = parseSemVerOrNull(remoteVersion) ?: return false
        val installed = parseSemVerOrNull(installedVersion) ?: return false
        return remote > installed
    }

    internal fun findLatestRelease(releases: List<ReleaseInfo>): ReleaseInfo? {
        val parsed = releases.mapNotNull { release ->
            parseReleaseVersion(release)?.let { version -> version to release }
        }
        if (parsed.isEmpty()) return null

        val stable = parsed.filter { it.first.preRelease.isEmpty() }
        val candidates = if (stable.isNotEmpty()) stable else parsed

        return candidates.maxWithOrNull(
            compareBy<Pair<SemVer, ReleaseInfo>>({ it.first }, { it.second.publishedAt })
        )?.second
    }

    private fun parseReleasesJson(json: String): List<ReleaseInfo> {
        val array = JSONArray(json)
        val releases = ArrayList<ReleaseInfo>(array.length())

        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            if (item.optBoolean("draft", false) || item.optBoolean("prerelease", false)) {
                continue
            }

            val assetsJson = item.optJSONArray("assets")
            val assets = buildList {
                if (assetsJson != null) {
                    for (j in 0 until assetsJson.length()) {
                        val asset = assetsJson.optJSONObject(j) ?: continue
                        val name = asset.optString("name", "")
                        val url = asset.optString("browser_download_url", "")
                        if (name.isNotBlank() && url.isNotBlank()) {
                            add(ReleaseAsset(name, url))
                        }
                    }
                }
            }

            releases += ReleaseInfo(
                tagName = item.optString("tag_name", ""),
                name = item.optString("name", ""),
                body = if (item.has("body")) item.optString("body") else null,
                publishedAt = item.optString("published_at", ""),
                htmlUrl = item.optString("html_url", ""),
                assets = assets
            )
        }

        return releases
    }

    private fun getTopReleaseFingerprint(releases: List<ReleaseInfo>): String {
        val latest = findLatestRelease(releases) ?: return ""
        return listOf(
            latest.tagName,
            latest.name,
            latest.publishedAt,
            latest.body.orEmpty(),
            latest.htmlUrl
        ).joinToString("||")
    }

    private suspend fun fetchReleasesNetwork(
        perPage: Int,
        cachedEtag: String?,
    ): ReleasesNetworkResult {
        val response: HttpResponse =
            client.get("https://api.github.com/repos/JAY01-CYBER/M3-Play/releases?per_page=$perPage") {
                headers {
                    append("Accept", "application/vnd.github+json")
                    append("User-Agent", "M3Play")
                    if (!cachedEtag.isNullOrBlank()) {
                        append("If-None-Match", cachedEtag)
                    }
                }
            }

        return ReleasesNetworkResult(
            status = response.status,
            body = if (response.status == HttpStatusCode.NotModified) null else response.bodyAsText(),
            etag = response.headers["ETag"] ?: cachedEtag
        )
    }

    suspend fun getCachedReleases(): List<ReleaseInfo> {
        val cachedJson = App.instance.dataStore.getAsync(GitHubReleasesJsonKey)
        return cachedJson
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { parseReleasesJson(it) }.getOrNull() }
            ?: emptyList()
    }

    suspend fun getLatestReleaseInfo(): Result<ReleaseInfo> = runCatching {
        val releases = getAllReleases(forceRefresh = true).getOrThrow()
        findLatestRelease(releases) ?: error("No stable GitHub release found")
    }

    suspend fun getLatestVersionName(): Result<String> =
        getLatestReleaseInfo().map { release ->
            parseReleaseVersion(release)?.normalizedName()
                ?: release.name.ifBlank { release.tagName }
        }

    suspend fun getLatestReleaseNotes(): Result<String?> =
        getLatestReleaseInfo().map { it.body }

    fun getAssetFileName(): String {
        return when (BuildConfig.ARCHITECTURE.lowercase()) {
            "arm64", "arm64-v8a" -> "app-arm64-release.apk"
            "armeabi", "armeabi-v7a" -> "app-armeabi-release.apk"
            "x86" -> "app-x86-release.apk"
            "x86_64", "x86-64" -> "app-x86_64-release.apk"
            "universal" -> "M3Play.apk"
            else -> "M3Play.apk"
        }
    }

    fun getDownloadUrl(release: ReleaseInfo): String? {
        val target = getAssetFileName()
        return release.assets.firstOrNull {
            it.name.equals(target, ignoreCase = true)
        }?.downloadUrl
            ?: release.assets.firstOrNull {
                it.name.equals("M3Play.apk", ignoreCase = true)
            }?.downloadUrl
    }

    // Kept non-suspending for existing callers such as AccountSettings.
    // GitHub's /releases/latest/download/ endpoint redirects to the actual asset.
    fun getLatestDownloadUrl(): String {
        val baseUrl = "https://github.com/JAY01-CYBER/M3-Play/releases/latest/download/"
        return baseUrl + getAssetFileName()
    }

    suspend fun getCommitHistory(
        count: Int = 20,
        branch: String = "main",
    ): Result<List<GitCommit>> = runCatching {
        val response = client
            .get("https://api.github.com/repos/JAY01-CYBER/M3-Play/commits?sha=$branch&per_page=$count")
            .bodyAsText()

        val array = JSONArray(response)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val commit = obj.getJSONObject("commit")
                val author = commit.optJSONObject("author")
                add(
                    GitCommit(
                        sha = obj.optString("sha", "").take(7),
                        message = commit.optString("message", "").lineSequence().firstOrNull().orEmpty(),
                        author = author?.optString("name", "Unknown") ?: "Unknown",
                        date = author?.optString("date", "").orEmpty(),
                        url = obj.optString("html_url", "")
                    )
                )
            }
        }
    }

    suspend fun getAllReleases(
        perPage: Int = 30,
        forceRefresh: Boolean = false,
    ): Result<List<ReleaseInfo>> = runCatching {
        val now = System.currentTimeMillis()
        val cachedJson = App.instance.dataStore.getAsync(GitHubReleasesJsonKey)
        val cachedEtag = App.instance.dataStore.getAsync(GitHubReleasesEtagKey)
        val lastCheckedAt = App.instance.dataStore.getAsync(GitHubReleasesLastCheckedAtKey, 0L)
        val cachedFingerprint = App.instance.dataStore.getAsync(GitHubReleasesFingerprintKey)

        val cachedReleases = cachedJson
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { parseReleasesJson(it) }.getOrNull() }

        val shouldCheckNetwork =
            forceRefresh ||
                cachedJson.isNullOrBlank() ||
                now - lastCheckedAt >= RELEASE_CACHE_CHECK_INTERVAL_MS

        if (!shouldCheckNetwork) {
            lastCheckTime = now
            return@runCatching cachedReleases.orEmpty()
        }

        val network = runCatching {
            fetchReleasesNetwork(perPage, cachedEtag)
        }.getOrNull()

        if (network == null) {
            if (cachedReleases != null) {
                lastCheckTime = now
                return@runCatching cachedReleases
            }
            error("Failed to fetch GitHub releases")
        }

        when {
            network.status == HttpStatusCode.NotModified -> {
                App.instance.dataStore.edit { settings ->
                    settings[GitHubReleasesLastCheckedAtKey] = now
                    network.etag?.let { settings[GitHubReleasesEtagKey] = it }
                }
                lastCheckTime = now
                cachedReleases ?: error("GitHub release cache is empty")
            }

            network.status.value in 200..299 && !network.body.isNullOrBlank() -> {
                val body = network.body
                val releases = parseReleasesJson(body)
                val fingerprint = getTopReleaseFingerprint(releases)

                App.instance.dataStore.edit { settings ->
                    settings[GitHubReleasesLastCheckedAtKey] = now
                    network.etag?.let { settings[GitHubReleasesEtagKey] = it }
                    settings[GitHubReleasesJsonKey] = body
                    if (cachedJson != body || cachedFingerprint != fingerprint) {
                        settings[GitHubReleasesFingerprintKey] = fingerprint
                    }
                }

                lastCheckTime = now
                releases
            }

            else -> {
                if (cachedReleases != null) {
                    lastCheckTime = now
                    cachedReleases
                } else {
                    error("GitHub releases request failed: HTTP ${network.status.value}")
                }
            }
        }
    }
}
