package com.mocharealm.accompanist.lyrics.phonetics

internal const val HEADER_SIZE = 64
internal const val BLOCK_SIZE = 32
internal const val RECORD_SIZE = 20
internal fun ByteSource.u16(at: Int): Int = byteAt(at) or (byteAt(at + 1) shl 8)
internal fun ByteSource.i32(at: Int): Int = u16(at) or (u16(at + 2) shl 16)
private class Cursor(val data: ByteSource, var at: Int) {
    fun variable(): Int {
        var value = 0
        for (shift in 0..28 step 7) {
            val b = data.byteAt(at++)
            require(shift < 28 || b <= 7) { "Invalid varint" }
            value = value or ((b and 127) shl shift)
            if (b < 128) return value
        }
        error("Invalid varint")
    }
}
data class Lexeme(val reading: String, val cost: Int, val leftId: Int, val rightId: Int, val aligned: Boolean, val sourceIds: List<String>,
    val weakPreference: Int = 0, val selectionRules: List<String> = emptyList())
enum class LexicalRole { OTHER, NOUN, PROPER_NOUN, NUMERAL, COUNTER, PARTICLE, VERB, ADJECTIVE, AUXILIARY }

/** Index choice belongs to the pack implementation, never to the unified parsing API. */
interface ReadingLexicon {
    val locale: ReadingLocale
    val maxKeyLength: Int
    fun lookup(surface: String, workspace: ParseWorkspace): List<Lexeme>
    fun connectionCost(previousRight: Int, nextLeft: Int): Int = 0
    /** Optional language-data metadata; custom dictionaries need not use these categories. */
    fun lexicalRole(entry: Lexeme): LexicalRole = LexicalRole.OTHER
    fun isAttributive(entry: Lexeme): Boolean = false
}

/** Front-coded sorted keys, independently decodable blocks, pooled readings and flat records.
 * Opening reads ONLY the header. Keys/records/matrix are accessed directly in their storage.
 */
class CompiledDictionary(val storage: ByteSource) : ReadingLexicon {
    override val locale: ReadingLocale
    val surfaceCount: Int
    val variantCount: Int
    override val maxKeyLength: Int
    private val blocks: Int
    private val index: Int
    private val keys: Int
    private val records: Int
    private val strings: Int
    private val matrix: Int
    private val rightSize: Int
    private val leftSize: Int

    init {
        require(storage.size >= HEADER_SIZE && storage.i32(0) == 0x3144504C && storage.i32(4) == 1) { "Unsupported LPD format" }
        locale = ReadingLocale.entries.getOrNull(storage.i32(8)) ?: error("Invalid locale")
        surfaceCount = storage.i32(12); variantCount = storage.i32(16); maxKeyLength = storage.i32(20)
        blocks = storage.i32(24); index = storage.i32(28); keys = storage.i32(32)
        records = storage.i32(36); strings = storage.i32(40); matrix = storage.i32(44)
        rightSize = storage.i32(48); leftSize = storage.i32(52)
        require(storage.i32(56) == storage.size && storage.i32(60) == 0) { "Truncated/unsupported pack" }
        require(surfaceCount > 0 && variantCount > 0 && maxKeyLength in 1..4096 && blocks.toLong() == (surfaceCount.toLong() + 31) / 32)
        require(index == HEADER_SIZE && keys.toLong() == index.toLong() + blocks.toLong() * 4)
        require(keys <= records && records.toLong() + variantCount.toLong() * RECORD_SIZE == strings.toLong())
        require(strings <= matrix && matrix <= storage.size && rightSize >= 0 && leftSize >= 0)
        require(matrix.toLong() + rightSize.toLong() * leftSize * 2 == storage.size.toLong())
    }

    private fun blockCursor(block: Int): Cursor {
        val offset = storage.i32(index + block * 4)
        require(offset >= 0 && keys.toLong() + offset < records) { "Invalid key offset" }
        return Cursor(storage, keys + offset)
    }
    private fun decodeKey(cursor: Cursor, previous: String): String {
        val prefix = cursor.variable(); val suffix = cursor.variable()
        require(prefix <= previous.length && prefix + suffix <= maxKeyLength && cursor.at.toLong() + suffix * 2 <= records)
        return buildString(prefix + suffix) {
            append(previous, 0, prefix)
            repeat(suffix) { append(storage.u16(cursor.at).toChar()); cursor.at += 2 }
        }
    }
    override fun lookup(surface: String, workspace: ParseWorkspace): List<Lexeme> {
        workspace.cached(this, surface)?.let { return it }
        var low = 0; var high = blocks - 1; var chosen = -1
        while (low <= high) {
            val middle = (low + high) ushr 1
            val first = decodeKey(blockCursor(middle), "")
            if (first <= surface) { chosen = middle; low = middle + 1 } else high = middle - 1
        }
        var result: List<Lexeme> = emptyList()
        if (chosen >= 0) {
            val cursor = blockCursor(chosen)
            var key = ""
            repeat(minOf(BLOCK_SIZE, surfaceCount - chosen * BLOCK_SIZE)) {
                key = decodeKey(cursor, key)
                val count = cursor.variable(); val first = cursor.variable()
                require(count > 0 && first >= 0 && first.toLong() + count <= variantCount)
                if (key >= surface) {
                    if (key == surface) result = List(count) { readLexeme(first + it) }
                    workspace.cache(this, surface, result)
                    return result
                }
            }
        }
        workspace.cache(this, surface, result)
        return result
    }
    private fun poolString(offset: Int): String {
        require(offset >= 0 && strings.toLong() + offset < matrix)
        val cursor = Cursor(storage, strings + offset)
        val length = cursor.variable()
        require(cursor.at.toLong() + length <= matrix)
        return ByteArray(length) { storage.byteAt(cursor.at + it).toByte() }.decodeToString(throwOnInvalidSequence = true)
    }
    private fun readLexeme(id: Int): Lexeme {
        val at = records + id * RECORD_SIZE
        return Lexeme(poolString(storage.i32(at)), storage.i32(at + 8), storage.u16(at + 12), storage.u16(at + 14),
            storage.i32(at + 16) == 1, poolString(storage.i32(at + 4)).split(','))
    }
    override fun connectionCost(previousRight: Int, nextLeft: Int): Int {
        if (rightSize == 0) return 0
        require(previousRight in 0 until rightSize && nextLeft in 0 until leftSize) { "Invalid context ID" }
        return storage.u16(matrix + (previousRight * leftSize + nextLeft) * 2).toShort().toInt()
    }
}
