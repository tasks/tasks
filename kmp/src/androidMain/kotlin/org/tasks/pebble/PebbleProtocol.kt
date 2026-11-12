package org.tasks.pebble

import android.content.Context
import com.getpebble.android.kit.PebbleKit
import com.getpebble.android.kit.util.PebbleDictionary
import java.util.UUID
import org.tasks.extensions.truncate

object PebbleProtocol {
    val APP_UUID: UUID = UUID.fromString("a1b2c3d4-1234-5678-abcd-1a5c500e9001")

    // AppMessage keys
    const val KEY_MSG_TYPE = 0
    const val KEY_TRANSACTION_ID = 1
    const val KEY_TOTAL_ITEMS = 2
    const val KEY_CHUNK_INDEX = 3
    const val KEY_CHUNK_COUNT = 4
    const val KEY_POSITION = 5
    const val KEY_LIMIT = 6
    const val KEY_FILTER = 7

    // Task item keys (per-item, offset by item index * ITEM_STRIDE)
    const val KEY_ITEM_BASE = 100
    const val ITEM_STRIDE = 10
    const val ITEM_ID_HIGH = 0
    const val ITEM_ID_LOW = 1
    const val ITEM_TYPE = 2
    const val ITEM_TITLE = 3
    const val ITEM_PRIORITY = 4
    const val ITEM_COMPLETED = 5
    const val ITEM_INDENT = 6
    const val ITEM_COLLAPSED = 7
    const val ITEM_NUM_SUBTASKS = 8
    const val ITEM_EXTRA = 9 // timestamp for tasks, icon for lists, color for lists

    // Single task keys (for GET_TASK response and COMPLETE_TASK request)
    const val KEY_TASK_ID_HIGH = 10
    const val KEY_TASK_ID_LOW = 11
    const val KEY_TASK_TITLE = 12
    const val KEY_TASK_PRIORITY = 13
    const val KEY_TASK_COMPLETED = 14
    const val KEY_TASK_REPEATING = 15
    const val KEY_TASK_DESCRIPTION = 16
    const val KEY_TASK_COUNT = 17
    const val KEY_TASK_COMPLETED_COUNT = 18

    // Toggle group keys
    const val KEY_GROUP_VALUE_HIGH = 20
    const val KEY_GROUP_VALUE_LOW = 21
    const val KEY_GROUP_COLLAPSED = 22

    // List item extra keys
    const val KEY_LIST_ICON = 23
    const val KEY_LIST_COLOR = 24
    const val KEY_LIST_COUNT = 25

    // Session ID (for replay detection)
    const val KEY_SESSION_ID = 8

    // Settings keys
    const val KEY_SHOW_HIDDEN = 28
    const val KEY_SHOW_COMPLETED = 29
    const val KEY_SORT_MODE = 30
    const val KEY_GROUP_MODE = 31

    // Capability key: every watch request carries the size of a single
    // inbound message that watch build supports, so response chunks can be
    // packed to fit (mirrors PROTOCOL_INBOX_SIZE in pebble/src/c/protocol.h)
    const val KEY_MAX_MSG_SIZE = 32

    // Assumed message capacity when KEY_MAX_MSG_SIZE is absent (older watch
    // app builds). Matches the non-aplite inbox the watch opens.
    const val DEFAULT_MAX_MSG_SIZE = 2016L

    // Watches advertising an inbox at or below this enter "small message"
    // mode: one item per chunk, short titles, and index tokens instead of
    // filter ids. aplite (original Pebble/Steel) can only be relied on for
    // 124-byte AppMessages.
    const val SMALL_MESSAGE_LIMIT = 1024L

    const val SMALL_CHUNK_SIZE = 1

    // Wraps an Int that fits in 7 bits (<= 127) so toPebbleDictionary encodes it
    // as a 1-byte uint tuple instead of a uint32. Every integer field that fits
    // -- message type, transaction id, chunk index/count, and the per-item
    // flags -- is 3 bytes cheaper on the wire this way, which is what lets a
    // whole task item fit aplite's 124-byte messages alongside the 33-byte
    // chunk header. Transaction ids are capped at 127 by the watch for this.
    class SmallInt(val value: Int) {
        init {
            require(value in 0..127) { "SmallInt value $value does not fit 7 bits" }
        }
    }

    // Serialized size budgets on aplite (124-byte AppMessage): each tuple
    // costs 5 header bytes (keyLength + 1-byte key + type + uint16 length)
    // plus its data, and the chunk header (KEY_MSG_TYPE,
    // KEY_TRANSACTION_ID, KEY_TOTAL_ITEMS, KEY_CHUNK_INDEX,
    // KEY_CHUNK_COUNT) costs 33 bytes once the type/txn/index/count tuples
    // are uint8.
    // A task item's fixed fields are two uint32 ids (18 bytes) plus six uint8
    // flags (36), so 33 + 54 + (6 + title) fits a title of up to
    // 24 bytes. Item timestamps (ITEM_EXTRA) are omitted in small mode.
    const val SMALL_MAX_TASK_TITLE = 24
    const val SMALL_MAX_DESC = 50

    // A list item's uint32 color/textColor cost 18, its type tuple 6, and the
    // id token at most ~8 ("w" + a few digits, null-terminated), so
    // 33 + 24 + ~8 + (6 + title) leaves ~50 bytes of title; titles are
    // byte-truncated to 40. Icon and count are omitted in small mode
    // (the watch parser ignores them anyway).
    const val SMALL_MAX_LIST_TITLE = 40

    // Filter ids can be longer than a small message (e.g. Google Tasks list
    // ids), so small mode sends "w<index>" tokens that map back to the
    // filter on the phone. Real filter ids are "<digits>:<...>" and header
    // ids are "<TYPE>_<id>", so the tokens can never collide.
    const val LIST_TOKEN_PREFIX = "w"

    // Watch → Phone message types
    const val MSG_GET_TASKS: Int = 1
    const val MSG_COMPLETE_TASK: Int = 2
    const val MSG_TOGGLE_GROUP: Int = 3
    const val MSG_GET_LISTS: Int = 4
    const val MSG_GET_TASK: Int = 5
    const val MSG_SAVE_TASK: Int = 6
    const val MSG_GET_TASK_COUNT: Int = 7
    const val MSG_TOGGLE_LIST: Int = 8

    // Phone → Watch response types
    const val RESP_TASKS: Int = 101
    const val RESP_COMPLETE_TASK: Int = 102
    const val RESP_TOGGLE_GROUP: Int = 103
    const val RESP_LISTS: Int = 104
    const val RESP_TASK: Int = 105
    const val RESP_SAVE_TASK: Int = 106
    const val RESP_TASK_COUNT: Int = 107
    const val RESP_TOGGLE_LIST: Int = 108

    // Push notification
    const val MSG_REFRESH: Int = 200

    // UiItem types
    const val TYPE_HEADER: Int = 0
    const val TYPE_TASK: Int = 1

    // Max items per chunk
    const val CHUNK_SIZE = 5

    // Max title length
    const val MAX_TITLE_LENGTH = 50

    fun splitLong(value: Long): Pair<Int, Int> {
        val high = (value ushr 32).toInt()
        val low = (value and 0xFFFFFFFFL).toInt()
        return high to low
    }

    fun combineLong(high: Int, low: Int): Long {
        return (high.toLong() and 0xFFFFFFFFL shl 32) or (low.toLong() and 0xFFFFFFFFL)
    }

    fun truncateTitle(title: String?): String = title.orEmpty().truncate(MAX_TITLE_LENGTH)

    // Max byte length is what matters on the wire, not characters, so small
    // mode has to measure UTF-8 bytes and never split a multibyte sequence.
    fun truncateBytes(text: String?, maxBytes: Int): String {
        if (text == null) return ""
        val bytes = text.toByteArray(Charsets.UTF_8)
        if (bytes.size <= maxBytes) return text
        var end = maxBytes
        while (end > 0 && (bytes[end].toInt() and 0xC0) == 0x80) {
            end--
        }
        return String(bytes, 0, end, Charsets.UTF_8)
    }

    fun maxMessageSize(data: Map<Int, Any>): Long =
        (data[KEY_MAX_MSG_SIZE] as? Long) ?: DEFAULT_MAX_MSG_SIZE

    fun isSmallMode(data: Map<Int, Any>): Boolean =
        maxMessageSize(data) <= SMALL_MESSAGE_LIMIT

    fun listToken(index: Int): String = "$LIST_TOKEN_PREFIX$index"

    fun isListToken(value: String): Boolean =
        value.length > LIST_TOKEN_PREFIX.length &&
                value.startsWith(LIST_TOKEN_PREFIX) &&
                with(value.substring(LIST_TOKEN_PREFIX.length)) {
                    isNotEmpty() && all { it.isDigit() }
                }

    fun toMap(dict: PebbleDictionary): Map<Int, Any> {
        val result = mutableMapOf<Int, Any>()
        for (tuple in dict) {
            val key = tuple.key
            val value = tuple.value
            when (value) {
                is String -> result[key] = value
                is Number -> result[key] = value.toLong()
            }
        }
        return result
    }

    fun toPebbleDictionary(entries: Map<Int, Any>): PebbleDictionary {
        val dict = PebbleDictionary()
        for ((key, value) in entries) {
            when (value) {
                is String -> dict.addString(key, value)
                is Int -> dict.addUint32(key, value)
                is Long -> dict.addUint32(key, value.toInt())
                is SmallInt -> dict.addUint8(key, value.value.toByte())
            }
        }
        return dict
    }

    fun sendToPebble(context: Context, dict: Map<Int, Any>, transactionId: Int) {
        val pebbleDict = toPebbleDictionary(dict)
        PebbleKit.sendDataToPebbleWithTransactionId(
            context, APP_UUID, pebbleDict, transactionId
        )
    }
}
