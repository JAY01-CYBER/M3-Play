/*
 * M3Play Utility Module
 *
 * GitHub release updater
 */

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
    val downloadUrl: String,
)

data class ReleaseInfo(
    val tagName: String,
    val name: String,
    val body: String?,
    val publishedAt: String,
    val htmlUrl: String,
    val assets: List<ReleaseAsset> = emptyList(),
)

private data class ReleasesNetworkResult(
    val status: HttpStatusCode,
    val body: String?,
    val etag: String?,
)

object Updater {
    private val client = HttpClient()

    // Force a fresh GitHub check when the updater is opened/checked.
    private const val ReleaseCacheCheckIntervalMs = 0L

    var lastCheckTime = -1L
        private set

    private data class SemVer(
        val major: Int,
        val minor: Int,
        val patch: Int,
        val preRelease: List<PreReleaseIdentifier>,
    ) : Comparable<SemVer> {
        override fun compareTo(other: SemVer): Int {
            major.compareTo(other.major).let { if (it != 0) return it }
            minor.compareTo(other.minor).let { if (it != 0) return it }
            patch.compareTo(other.patch).let { if (it != 0) return it }

            val thisStable = preRelease.isEmpty()
            val otherStable = other.preRelease.isEmpty()

            if (thisStable && !otherStable) return 1
            if (!thisStable && otherStable) return -1

            val count = minOf(preRelease.size, other.preRelease.size)
            for (i in 0 until count) {
                val result = preRelease[i].compareTo(other.preRelease[i])
                if (result != 0) return result
            }

            return preRelease.size.compareTo(other.preRelease.size)
        }

        fun normalizedName(): String =
            if (preRelease.isEmpty()) {
                "$major.$minor.$patch"
            } else {
                "$major.$minor.$patch-" +
                    preRelease.joinToString(".") { it.raw }
            }
    }

    private sealed interface PreReleaseIdentifier :
        Comparable<PreReleaseIdentifier> {
        val raw: String
    }

    private data class NumericIdentifier(
        override val raw: String,
        val value: Long,
    ) : PreReleaseIdentifier {
        override fun compareTo(other: PreReleaseIdentifier): Int =
            when (other) {
                is NumericIdentifier -> value.compareTo(other.value)
                is AlphaIdentifier -> -1
            }
    }

    private data class AlphaIdentifier(
        override val raw: String,
    ) : PreReleaseIdentifier {
        override fun compareTo(other: PreReleaseIdentifier): Int =
            when (other) {
                is NumericIdentifier -> 1
                is AlphaIdentifier -> raw.compareTo(other.raw)
            }
    }

    private val semVerRegex =
        Regex(
            """(?i)\bv?(\d+)\.(\d+)\.(\d+)(?:-([0-9A-Za-z.-]+))?(?:\+[0-9A-Za-z.-]+)?\b"""
        )

    private fun parseSemVerOrNull(text: String): SemVer? {
        val match = semVerRegex.find(text) ?: return null

        val major = match.groupValues.getOrNull(1)?.toIntOrNull() ?: return null
        val minor = match.groupValues.getOrNull(2)?.toIntOrNull() ?: return null
        val patch = match.groupValues.getOrNull(3)?.toIntOrNull() ?: return null

        val preRelease =
            match.groupValues.getOrNull(4)
                ?.takeIf { it.isNotBlank() }
                ?.split(".")
                ?.filter { it.isNotBlank() }
                ?.map { identifier ->
                    if (identifier.all(Char::isDigit)) {
                        NumericIdentifier(
                            raw = identifier,
                            value = identifier.toLongOrNull() ?: 0L,
                        )
                    } else {
                        AlphaIdentifier(identifier)
                    }
                }
                ?: emptyList()

        return SemVer(
            major = major,
            minor = minor,
            patch = patch,
            preRelease = preRelease,
        )
    }

    private fun parseReleaseSemVerOrNull(
        release: ReleaseInfo,
    ): SemVer? =
        parseSemVerOrNull(release.tagName)
            ?: parseSemVerOrNull(release.name)

    internal fun isSameVersion(
        a: String,
        b: String,
    ): Boolean {
        val first = parseSemVerOrNull(a)
        val second = parseSemVerOrNull(b)

        return if (first != null && second != null) {
            first.compareTo(second) == 0
        } else {
            a.trim().removePrefix("v")
                .equals(
                    b.trim().removePrefix("v"),
                    ignoreCase = true,
                )
        }
    }

    /**
     * TRUE only when remoteVersion is newer than installedVersion.
     *
     * Example:
     * installed 3.2.0 / remote 3.1.0 -> false
     * installed 3.1.0 / remote 3.2.0 -> true
     */
    internal fun isUpdateAvailable(
        remoteVersion: String,
        installedVersion: String,
    ): Boolean {
        val remote = parseSemVerOrNull(remoteVersion)
        val installed = parseSemVerOrNull(installedVersion)

        // Never offer a downgrade if a version cannot be parsed safely.
        if (remote == null || installed == null) return false

        return remote > installed
    }

    internal fun findLatestRelease(
        releases: List<ReleaseInfo>,
    ): ReleaseInfo? {
        if (releases.isEmpty()) return null

        val parsed =
            releases
                .filter { !it.tagName.isNullOrBlank() || !it.name.isNullOrBlank() }
                .mapNotNull { release ->
                    parseReleaseSemVerOrNull(release)
                        ?.let { version -> version to release }
                }

        if (parsed.isEmpty()) return null

        // Prefer stable releases.
        val stable = parsed.filter { it.first.preRelease.isEmpty() }
        val candidates = stable.ifEmpty { parsed }

        return candidates
            .maxWithOrNull(
                compareBy<Pair<SemVer, ReleaseInfo>>(
                    { it.first },
                    { it.second.publishedAt },
                )
            )
            ?.second
    }

    private fun preferredReleaseVersionNameOrNull(
        release: ReleaseInfo,
    ): String? =
        parseReleaseSemVerOrNull(release)?.normalizedName()

    private fun parseReleasesJson(
        json: String,
    ): List<ReleaseInfo> {
        val jsonArray = JSONArray(json)
        val releases = ArrayList<ReleaseInfo>(jsonArray.length())

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)

            if (
                item.optBoolean("draft", false) ||
                item.optBoolean("prerelease", false)
            ) {
                continue
            }

            val assetsJson = item.optJSONArray("assets")
            val assets = buildList {
                if (assetsJson != null) {
                    for (assetIndex in 0 until assetsJson.length()) {
                        val asset = assetsJson.optJSONObject(assetIndex) ?: continue
                        val name = asset.optString("name", "")
                        val url = asset.optString("browser_download_url", "")

                        if (name.isNotBlank() && url.isNotBlank()) {
                            add(
                                ReleaseAsset(
                                    name = name,
                                    downloadUrl = url,
                                )
                            )
                        }
                    }
                }
            }

            releases.add(
                ReleaseInfo(
                    tagName = item.optString("tag_name", ""),
                    name = item.optString("name", ""),
                    body = if (item.has("body")) item.optString("body") else null,
                    publishedAt = item.optString("published_at", ""),
                    htmlUrl = item.optString("html_url", ""),
                    assets = assets,
                )
            )
        }

        return releases
    }

    private fun getTopReleaseFingerprint(
        releases: List<ReleaseInfo>,
    ): String {
        val latest = findLatestRelease(releases) ?: return ""

        return listOf(
            latest.tagName,
            latest.name,
            latest.publishedAt,
            latest.body.orEmpty(),
            latest.htmlUrl,
            latest.assets.joinToString("|") {
                "${it.name}:${it.downloadUrl}"
            },
        ).joinToString("||")
    }

    private suspend fun fetchReleasesNetwork(
        perPage: Int,
        cachedEtag: String?,
    ): ReleasesNetworkResult {
        val response: HttpResponse =
            client.get(
                "https://api.github.com/repos/" +
                    "JAY01-CYBER/M3-Play/releases?per_page=$perPage"
            ) {
                headers {
                    append(
                        "Accept",
                        "application/vnd.github+json",
                    )
                    append(
                        "User-Agent",
                        "M3Play",
                    )

                    if (!cachedEtag.isNullOrBlank()) {
                        append(
                            "If-None-Match",
                            cachedEtag,
                        )
                    }
                }
            }

        return ReleasesNetworkResult(
            status = response.status,
            body = if (response.status == HttpStatusCode.NotModified) {
                null
            } else {
                response.bodyAsText()
            },
            etag = response.headers["ETag"],
        )
    }

    suspend fun getCachedReleases(): List<ReleaseInfo> {
        val cachedJson =
            App.instance.dataStore.getAsync(
                GitHubReleasesJsonKey
            )

        return cachedJson
            ?.takeIf { it.isNotBlank() }
            ?.let {
                runCatching {
                    parseReleasesJson(it)
                }.getOrNull()
            }
            ?: emptyList()
    }

    suspend fun getLatestVersionName(): Result<String> =
        getLatestReleaseInfo().map { release ->
            preferredReleaseVersionNameOrNull(release)
                ?: release.name.ifBlank { release.tagName }
        }

    suspend fun getLatestReleaseNotes(): Result<String?> =
        getLatestReleaseInfo().map { it.body }

    suspend fun getLatestReleaseInfo(): Result<ReleaseInfo> =
        runCatching {
            val releases =
                getAllReleases(
                    forceRefresh = true
                ).getOrThrow()

            findLatestRelease(releases)
                ?: throw IllegalStateException(
                    "No stable GitHub releases found"
                )
        }

    /**
     * Exact APK names produced by .github/workflows/release.yml:
     *
     * Universal: M3Play.apk
     * ARM64:     app-arm64-release.apk
     * ARM:       app-armeabi-release.apk
     * x86:       app-x86-release.apk
     * x86_64:    app-x86_64-release.apk
     */
    fun getAssetFileName(): String =
        when (BuildConfig.ARCHITECTURE.lowercase()) {
            "universal" -> "M3Play.apk"
            "arm64" -> "app-arm64-release.apk"
            "armeabi" -> "app-armeabi-release.apk"
            "x86" -> "app-x86-release.apk"
            "x86_64" -> "app-x86_64-release.apk"
            else -> "M3Play.apk"
        }

    /**
     * Keeps the existing non-suspend API used by AccountSettings and
     * UpdateNotificationManager.
     *
     * /releases/latest/download/ resolves to GitHub's published latest
     * release. The filename is now the real universal/ABI asset name.
     */
    fun getLatestDownloadUrl(): String {
        val base =
            "https://github.com/JAY01-CYBER/M3-Play/releases/latest/download/"

        return base + getAssetFileName()
    }

    /**
     * Returns the exact browser_download_url for a specific release.
     * Useful when a caller already has ReleaseInfo from the API.
     */
    fun getDownloadUrl(
        release: ReleaseInfo,
    ): String? {
        val expected = getAssetFileName()

        return release.assets
            .firstOrNull {
                it.name.equals(
                    expected,
                    ignoreCase = true,
                )
            }
            ?.downloadUrl
            ?.takeIf { it.isNotBlank() }
            ?: release.tagName
                .takeIf { it.isNotBlank() }
                ?.let { tag ->
                    "https://github.com/" +
                        "JAY01-CYBER/M3-Play/releases/download/" +
                        "${tag.removePrefix("/")}/$expected"
                }
    }

    suspend fun getCommitHistory(
        count: Int = 20,
        branch: String = "main",
    ): Result<List<GitCommit>> =
        runCatching {
            val response =
                client.get(
                    "https://api.github.com/repos/" +
                        "JAY01-CYBER/M3-Play/commits" +
                        "?sha=$branch&per_page=$count"
                ).bodyAsText()

            val jsonArray = JSONArray(response)
            val commits = mutableListOf<GitCommit>()

            for (i in 0 until jsonArray.length()) {
                val commitObj = jsonArray.getJSONObject(i)
                val commit = commitObj.getJSONObject("commit")
                val authorObj = commit.optJSONObject("author")

                commits.add(
                    GitCommit(
                        sha = commitObj.optString("sha", "").take(7),
                        message = commit
                            .optString("message", "")
                            .lines()
                            .firstOrNull()
                            ?: "",
                        author = authorObj
                            ?.optString("name", "Unknown")
                            ?: "Unknown",
                        date = authorObj
                            ?.optString("date", "")
                            ?: "",
                        url = commitObj.optString("html_url", ""),
                    )
                )
            }

            commits
        }

    suspend fun getAllReleases(
        perPage: Int = 30,
        forceRefresh: Boolean = false,
    ): Result<List<ReleaseInfo>> =
        runCatching {
            val now = System.currentTimeMillis()

            val cachedJson =
                App.instance.dataStore.getAsync(
                    GitHubReleasesJsonKey
                )

            val cachedEtag =
                App.instance.dataStore.getAsync(
                    GitHubReleasesEtagKey
                )

            val lastCheckedAt =
                App.instance.dataStore.getAsync(
                    GitHubReleasesLastCheckedAtKey,
                    0L,
                )

            val cachedFingerprint =
                App.instance.dataStore.getAsync(
                    GitHubReleasesFingerprintKey
                )

            val cachedReleases =
                cachedJson
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        runCatching {
                            parseReleasesJson(it)
                        }.getOrNull()
                    }

            val shouldCheckNetwork =
                forceRefresh ||
                    cachedJson.isNullOrBlank() ||
                    now - lastCheckedAt >= ReleaseCacheCheckIntervalMs

            if (!shouldCheckNetwork) {
                lastCheckTime = now
                return@runCatching cachedReleases ?: emptyList()
            }

            val networkResult =
                runCatching {
                    fetchReleasesNetwork(
                        perPage = perPage,
                        cachedEtag = cachedEtag,
                    )
                }.getOrNull()

            if (networkResult == null) {
                cachedReleases?.let {
                    lastCheckTime = now
                    return@runCatching it
                }

                throw IllegalStateException(
                    "Failed to fetch GitHub releases"
                )
            }

            when {
                networkResult.status ==
                    HttpStatusCode.NotModified -> {

                    App.instance.dataStore.edit { settings ->
                        settings[
                            GitHubReleasesLastCheckedAtKey
                        ] = now

                        networkResult.etag?.let {
                            settings[
                                GitHubReleasesEtagKey
                            ] = it
                        }
                    }

                    cachedReleases?.let {
                        lastCheckTime = now
                        return@runCatching it
                    }

                    throw IllegalStateException(
                        "GitHub release cache is empty"
                    )
                }

                networkResult.status.value in 200..299 &&
                    !networkResult.body.isNullOrBlank() -> {

                    val body = networkResult.body
                    val releases = parseReleasesJson(body)
                    val fingerprint = getTopReleaseFingerprint(releases)

                    App.instance.dataStore.edit { settings ->
                        settings[
                            GitHubReleasesLastCheckedAtKey
                        ] = now

                        networkResult.etag?.let {
                            settings[
                                GitHubReleasesEtagKey
                            ] = it
                        }

                        if (
                            cachedJson != body ||
                            cachedFingerprint != fingerprint ||
                            cachedJson.isNullOrBlank()
                        ) {
                            settings[
                                GitHubReleasesJsonKey
                            ] = body

                            settings[
                                GitHubReleasesFingerprintKey
                            ] = fingerprint
                        }
                    }

                    lastCheckTime = now
                    releases
                }

                else -> {
                    cachedReleases?.let {
                        lastCheckTime = now
                        return@runCatching it
                    }

                    throw IllegalStateException(
                        "GitHub releases request failed: HTTP " +
                            networkResult.status.value
                    )
                }
            }
        }
}
