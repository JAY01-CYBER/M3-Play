package com.zemer.cipher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File

object PlayerJsFetcher {
    private const val TAG = "Zemer_CipherFetcher"
    private const val IFRAME_API_URL = "https://www.youtube.com/iframe_api"
    private const val PLAYER_JS_URL_TEMPLATE = "https://www.youtube.com/s/player/%s/player_ias.vflset/en_GB/base.js"
    private const val CACHE_TTL_MS = 6 * 60 * 60 * 1000L // 6 hours

    /** STS of the currently cached player JS — must match what we send in API requests. */
    /** The STS extracted from one player, held with the hash it came from so the pair can never tear. */
    private class Sts(val value: Int?, val hash: String)

    // ONE volatile holder for both, read once per fast-path decision: two separate fields could be
    // observed mid-update by a concurrent rememberSts (a new player's STS beside the old hash).
    @Volatile
    private var sts: Sts? = null

    val cachedSignatureTimestamp: Int?
        get() = sts?.value

    // One shared client for the whole library — see ZemerCipher.httpClient.
    private val httpClient: OkHttpClient
        get() = ZemerCipher.httpClient

    // Regex to extract player hash from iframe_api response
    private val PLAYER_HASH_REGEX = Regex("""\\?/s\\?/player\\?/([a-zA-Z0-9_-]+)\\?/""")

    // Serializes cache mutations: getPlayerJs has UNsynchronized concurrent callers (prewarm,
    // signatureTimestamp() outside deobfuscateMutex, the app's EjsNTransformSolver), and an
    // unlocked writeToCache purge racing another writer's writeAtomic tmp window would delete
    // the tmp mid-write and silently degrade to a truncating non-atomic write.
    private val cacheWriteLock = Any()

    private fun getCacheDir(): File = File(CipherDeobfuscator.appContext.filesDir, "cipher_cache")

    private fun getCacheFile(hash: String): File = File(getCacheDir(), "player_$hash.js")

    private fun getHashFile(): File = File(getCacheDir(), "current_hash.txt")

    suspend fun getPlayerJs(forceRefresh: Boolean = false): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val cacheDir = getCacheDir()
            if (!cacheDir.exists()) cacheDir.mkdirs()

            // Check cache first (unless forced refresh)
            if (!forceRefresh) {
                val cached = readFromCache()
                if (cached != null) {
                    Timber.tag(TAG).d("Using cached player JS (hash=${cached.second})")
                    val known = sts
                    if (known?.value == null || known.hash != cached.second) {
                        rememberSts(FunctionNameExtractor.extractSignatureTimestamp(cached.first, cached.second), cached.second)
                        Timber.tag(TAG).d("STS from cached player: $cachedSignatureTimestamp")
                    }
                    return@withContext cached
                }
            }

            // Fetch player hash from iframe_api
            val hash = fetchPlayerHash()
            if (hash == null) {
                Timber.tag(TAG).e("Failed to extract player hash from iframe_api")
                return@withContext null
            }
            Timber.tag(TAG).d("Extracted player hash: $hash")

            // Download player JS
            val playerJs = downloadPlayerJs(hash)
            if (playerJs == null) {
                Timber.tag(TAG).e("Failed to download player JS for hash=$hash")
                return@withContext null
            }
            Timber.tag(TAG).d("Downloaded player JS: ${playerJs.length} chars")

            // Cache the result
            writeToCache(hash, playerJs)
            rememberSts(FunctionNameExtractor.extractSignatureTimestamp(playerJs, hash), hash)
            Timber.tag(TAG).d("STS from fresh player ($hash): $cachedSignatureTimestamp")

            Pair(playerJs, hash)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "getPlayerJs exception: ${e.message}")
            null
        }
    }

    /**
     * The STS of the current cached player WITHOUT re-reading the player JS. [getPlayerJs] reads the whole
     * cached base.js on every warm call (~2.8 MB as bytes, then again as a String), and the per-track STS
     * lookup only ever needed the Int it had already extracted - on low-RAM devices that per-track 5+ MB
     * allocation was an OutOfMemoryError site. The fast path trusts the cached Int only while the small hash
     * file still names the same, unexpired player ([stsFastPath]); anything else falls through to the full
     * fetch, so the STS can never lag the cached player.
     */
    suspend fun signatureTimestamp(): Int? = withContext(Dispatchers.IO) {
        val known = sts
        stsFastPath(known?.value, known?.hash, readCachedHash())?.let { return@withContext it }
        getPlayerJs(forceRefresh = false) ?: return@withContext null
        cachedSignatureTimestamp
    }

    /** Pure: the remembered STS is served only when it was extracted from the player the cache currently holds. */
    internal fun stsFastPath(sts: Int?, stsHash: String?, cachedHash: String?): Int? =
        if (sts != null && stsHash != null && cachedHash == stsHash) sts else null

    private fun rememberSts(value: Int?, hash: String) {
        sts = Sts(value, hash)
    }

    fun invalidateCache() {
        synchronized(cacheWriteLock) {
            try {
                val cacheDir = getCacheDir()
                if (cacheDir.exists()) {
                    // Only the player-JS cache (player_*.js + current_hash.txt) belongs to this
                    // fetcher. The dir is shared with PlayerConfigStore (configs_remote.json/
                    // .meta) — do NOT wipe those, or every decipher retry destroys the config
                    // ETag and forces a full non-conditional re-download of the config file.
                    cacheDir.listFiles()
                        ?.filter { it.name.startsWith("player_") || it.name == "current_hash.txt" }
                        ?.forEach { it.delete() }
                }
                Timber.tag(TAG).d("Cache invalidated")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to invalidate cache: ${e.message}")
            }
        }
    }

    /** The hash the small `current_hash.txt` names, or null when absent, malformed or past the TTL. */
    private fun readCachedHash(): String? {
        val hashFile = getHashFile()
        if (!hashFile.exists()) return null
        // A read failure (the file deleted by invalidateCache between exists() and here, or I/O) is a
        // cache miss, never an exception out of the STS fast path.
        return try {
            parseHashEntry(hashFile.readText(), System.currentTimeMillis(), CACHE_TTL_MS)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error reading cached hash: ${e.message}")
            null
        }
    }

    /**
     * Pure: `current_hash.txt` is `<hash>\n<written-at-ms>`; the entry is live only inside [ttlMs] of
     * [now] (withinWindow: a future timestamp from a backward clock step counts as expired, not fresh).
     */
    internal fun parseHashEntry(text: String, now: Long, ttlMs: Long): String? {
        val hashData = text.split("\n")
        if (hashData.size < 2) return null
        val hash = hashData[0]
        if (hash.isBlank()) return null
        val timestamp = hashData[1].toLongOrNull() ?: return null
        if (!PlayerConfigStore.withinWindow(now, timestamp, ttlMs)) {
            Timber.tag(TAG).d("Cache expired (hash=$hash)")
            return null
        }
        return hash
    }

    private fun readFromCache(): Pair<String, String>? {
        try {
            val hash = readCachedHash() ?: return null
            val cacheFile = getCacheFile(hash)
            if (!cacheFile.exists()) return null

            val playerJs = cacheFile.readText()
            if (playerJs.isEmpty()) return null

            return Pair(playerJs, hash)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error reading cache: ${e.message}")
            return null
        }
    }

    private fun writeToCache(hash: String, playerJs: String) {
        synchronized(cacheWriteLock) {
            try {
                val cacheDir = getCacheDir()
                // Clean old cache files
                cacheDir.listFiles()?.filter { it.name.startsWith("player_") }?.forEach { it.delete() }

                // Atomic (temp + rename): a plain writeText truncates first, so process death
                // during a same-hash force-refresh rewrite would leave a truncated player.js
                // that readFromCache happily serves until the TTL expires.
                PlayerConfigStore.writeAtomic(getCacheFile(hash), playerJs)
                PlayerConfigStore.writeAtomic(getHashFile(), "$hash\n${System.currentTimeMillis()}")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error writing cache: ${e.message}")
            }
        }
    }

    private fun fetchPlayerHash(): String? {
        val request = Request.Builder()
            .url(IFRAME_API_URL)
            .header("User-Agent", "Mozilla/5.0")
            .build()

        // .use{} so the response is closed on the error path too (an unread body would
        // otherwise strand its connection).
        val body = httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Timber.tag(TAG).e("iframe_api HTTP ${response.code}")
                return null
            }
            response.body?.string()
        } ?: return null
        val match = PLAYER_HASH_REGEX.find(body)
        return match?.groupValues?.get(1)
    }

    private fun downloadPlayerJs(hash: String): String? {
        val url = PLAYER_JS_URL_TEMPLATE.format(hash)
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()

        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Timber.tag(TAG).e("player JS download HTTP ${response.code}")
                return null
            }
            response.body?.string()
        }
    }
}
