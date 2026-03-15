package org.tasks.pebble

import android.content.Context
import com.getpebble.android.kit.PebbleKit
import com.getpebble.android.kit.util.PebbleDictionary
import java.util.UUID

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

    // Watch → Phone message types
    const val MSG_GET_TASKS: Int = 1
    const val MSG_COMPLETE_TASK: Int = 2
    const val MSG_TOGGLE_GROUP: Int = 3
    const val MSG_GET_LISTS: Int = 4
    const val MSG_GET_TASK: Int = 5
    const val MSG_SAVE_TASK: Int = 6
    const val MSG_GET_TASK_COUNT: Int = 7

    // Phone → Watch response types
    const val RESP_TASKS: Int = 101
    const val RESP_COMPLETE_TASK: Int = 102
    const val RESP_TOGGLE_GROUP: Int = 103
    const val RESP_LISTS: Int = 104
    const val RESP_TASK: Int = 105
    const val RESP_SAVE_TASK: Int = 106
    const val RESP_TASK_COUNT: Int = 107

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

    fun truncateTitle(title: String?): String {
        return when {
            title == null -> ""
            title.length > MAX_TITLE_LENGTH -> title.substring(0, MAX_TITLE_LENGTH)
            else -> title
        }
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
