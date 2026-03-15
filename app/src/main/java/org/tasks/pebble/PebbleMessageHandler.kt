package org.tasks.pebble

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.tasks.pebble.PebbleProtocol.CHUNK_SIZE
import org.tasks.pebble.PebbleProtocol.ITEM_COLLAPSED
import org.tasks.pebble.PebbleProtocol.ITEM_COMPLETED
import org.tasks.pebble.PebbleProtocol.ITEM_EXTRA
import org.tasks.pebble.PebbleProtocol.ITEM_ID_HIGH
import org.tasks.pebble.PebbleProtocol.ITEM_ID_LOW
import org.tasks.pebble.PebbleProtocol.ITEM_INDENT
import org.tasks.pebble.PebbleProtocol.ITEM_NUM_SUBTASKS
import org.tasks.pebble.PebbleProtocol.ITEM_PRIORITY
import org.tasks.pebble.PebbleProtocol.ITEM_STRIDE
import org.tasks.pebble.PebbleProtocol.ITEM_TITLE
import org.tasks.pebble.PebbleProtocol.ITEM_TYPE
import org.tasks.pebble.PebbleProtocol.KEY_CHUNK_COUNT
import org.tasks.pebble.PebbleProtocol.KEY_CHUNK_INDEX
import org.tasks.pebble.PebbleProtocol.KEY_FILTER
import org.tasks.pebble.PebbleProtocol.KEY_GROUP_COLLAPSED
import org.tasks.pebble.PebbleProtocol.KEY_GROUP_VALUE_HIGH
import org.tasks.pebble.PebbleProtocol.KEY_GROUP_VALUE_LOW
import org.tasks.pebble.PebbleProtocol.KEY_ITEM_BASE
import org.tasks.pebble.PebbleProtocol.KEY_LIMIT
import org.tasks.pebble.PebbleProtocol.KEY_LIST_COLOR
import org.tasks.pebble.PebbleProtocol.KEY_LIST_COUNT
import org.tasks.pebble.PebbleProtocol.KEY_LIST_ICON
import org.tasks.pebble.PebbleProtocol.KEY_MSG_TYPE
import org.tasks.pebble.PebbleProtocol.KEY_POSITION
import org.tasks.pebble.PebbleProtocol.KEY_TASK_COMPLETED
import org.tasks.pebble.PebbleProtocol.KEY_TASK_COMPLETED_COUNT
import org.tasks.pebble.PebbleProtocol.KEY_TASK_COUNT
import org.tasks.pebble.PebbleProtocol.KEY_TASK_DESCRIPTION
import org.tasks.pebble.PebbleProtocol.KEY_TASK_ID_HIGH
import org.tasks.pebble.PebbleProtocol.KEY_TASK_ID_LOW
import org.tasks.pebble.PebbleProtocol.KEY_TASK_PRIORITY
import org.tasks.pebble.PebbleProtocol.KEY_TASK_REPEATING
import org.tasks.pebble.PebbleProtocol.KEY_TASK_TITLE
import org.tasks.pebble.PebbleProtocol.KEY_TOTAL_ITEMS
import org.tasks.pebble.PebbleProtocol.KEY_TRANSACTION_ID
import org.tasks.pebble.PebbleProtocol.MSG_COMPLETE_TASK
import org.tasks.pebble.PebbleProtocol.MSG_GET_LISTS
import org.tasks.pebble.PebbleProtocol.MSG_GET_TASK
import org.tasks.pebble.PebbleProtocol.MSG_GET_TASKS
import org.tasks.pebble.PebbleProtocol.MSG_GET_TASK_COUNT
import org.tasks.pebble.PebbleProtocol.MSG_SAVE_TASK
import org.tasks.pebble.PebbleProtocol.MSG_TOGGLE_GROUP
import org.tasks.pebble.PebbleProtocol.RESP_COMPLETE_TASK
import org.tasks.pebble.PebbleProtocol.RESP_LISTS
import org.tasks.pebble.PebbleProtocol.RESP_SAVE_TASK
import org.tasks.pebble.PebbleProtocol.RESP_TASK
import org.tasks.pebble.PebbleProtocol.RESP_TASKS
import org.tasks.pebble.PebbleProtocol.RESP_TASK_COUNT
import org.tasks.pebble.PebbleProtocol.RESP_TOGGLE_GROUP
import org.tasks.pebble.PebbleProtocol.TYPE_HEADER
import org.tasks.pebble.PebbleProtocol.TYPE_TASK
import org.tasks.watch.WatchListItem
import org.tasks.watch.WatchServiceLogic
import org.tasks.watch.WatchUiItem
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.ceil

class PebbleMessageHandler @Inject constructor(
    private val watchServiceLogic: WatchServiceLogic,
) {
    companion object {
        private const val CHUNK_DELAY_MS = 100L
    }

    private val collapsedGroups = mutableSetOf<Long>()

    fun handleMessage(
        context: Context,
        data: Map<Int, Any>,
        transactionId: Int,
        scope: CoroutineScope,
    ) {
        val msgType = (data[KEY_MSG_TYPE] as? Long)?.toInt() ?: return
        scope.launch {
            try {
                when (msgType) {
                    MSG_GET_TASKS -> handleGetTasks(context, data, transactionId)
                    MSG_COMPLETE_TASK -> handleCompleteTask(context, data, transactionId)
                    MSG_TOGGLE_GROUP -> handleToggleGroup(context, data, transactionId)
                    MSG_GET_LISTS -> handleGetLists(context, data, transactionId)
                    MSG_GET_TASK -> handleGetTask(context, data, transactionId)
                    MSG_SAVE_TASK -> handleSaveTask(context, data, transactionId)
                    MSG_GET_TASK_COUNT -> handleGetTaskCount(context, data, transactionId)
                    else -> Timber.w("Unknown Pebble message type: $msgType")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error handling Pebble message type $msgType")
            }
        }
    }

    private suspend fun handleGetTasks(
        context: Context,
        data: Map<Int, Any>,
        transactionId: Int,
    ) {
        val position = (data[KEY_POSITION] as? Long)?.toInt() ?: 0
        val limit = (data[KEY_LIMIT] as? Long)?.toInt() ?: 0
        val filter = data[KEY_FILTER] as? String

        val result = watchServiceLogic.getTasks(
            filterPreference = filter,
            position = position,
            limit = limit,
            collapsed = collapsedGroups,
        )

        val chunkCount = ceil(result.items.size.toDouble() / CHUNK_SIZE).toInt()
            .coerceAtLeast(1)

        for (chunkIndex in 0 until chunkCount) {
            val start = chunkIndex * CHUNK_SIZE
            val end = (start + CHUNK_SIZE).coerceAtMost(result.items.size)
            val chunkItems = result.items.subList(start, end)

            val dict = mutableMapOf<Int, Any>(
                KEY_MSG_TYPE to RESP_TASKS,
                KEY_TRANSACTION_ID to transactionId,
                KEY_TOTAL_ITEMS to result.totalItems,
                KEY_CHUNK_INDEX to chunkIndex,
                KEY_CHUNK_COUNT to chunkCount,
            )

            chunkItems.forEachIndexed { index, item ->
                val base = KEY_ITEM_BASE + index * ITEM_STRIDE
                when (item) {
                    is WatchUiItem.Header -> {
                        val (high, low) = PebbleProtocol.splitLong(item.id)
                        dict[base + ITEM_ID_HIGH] = high
                        dict[base + ITEM_ID_LOW] = low
                        dict[base + ITEM_TYPE] = TYPE_HEADER
                        dict[base + ITEM_TITLE] = PebbleProtocol.truncateTitle(item.title)
                        dict[base + ITEM_COLLAPSED] = if (item.collapsed) 1 else 0
                    }
                    is WatchUiItem.Task -> {
                        val (high, low) = PebbleProtocol.splitLong(item.id)
                        dict[base + ITEM_ID_HIGH] = high
                        dict[base + ITEM_ID_LOW] = low
                        dict[base + ITEM_TYPE] = TYPE_TASK
                        dict[base + ITEM_TITLE] = PebbleProtocol.truncateTitle(item.title)
                        dict[base + ITEM_PRIORITY] = item.priority
                        dict[base + ITEM_COMPLETED] = if (item.completed) 1 else 0
                        dict[base + ITEM_INDENT] = item.indent
                        dict[base + ITEM_COLLAPSED] = if (item.collapsed) 1 else 0
                        dict[base + ITEM_NUM_SUBTASKS] = item.numSubtasks
                        if (item.timestamp != null) {
                            dict[base + ITEM_EXTRA] = PebbleProtocol.truncateTitle(item.timestamp)
                        }
                    }
                }
            }

            PebbleProtocol.sendToPebble(context, dict, transactionId)
            if (chunkIndex < chunkCount - 1) {
                delay(CHUNK_DELAY_MS)
            }
        }
    }

    private suspend fun handleCompleteTask(
        context: Context,
        data: Map<Int, Any>,
        transactionId: Int,
    ) {
        val high = (data[KEY_TASK_ID_HIGH] as? Long)?.toInt() ?: 0
        val low = (data[KEY_TASK_ID_LOW] as? Long)?.toInt() ?: 0
        val taskId = PebbleProtocol.combineLong(high, low)
        val completed = (data[KEY_TASK_COMPLETED] as? Long)?.toInt() != 0

        watchServiceLogic.completeTask(taskId, completed, "pebble")

        val dict = mapOf<Int, Any>(
            KEY_MSG_TYPE to RESP_COMPLETE_TASK,
            KEY_TRANSACTION_ID to transactionId,
            KEY_TASK_COMPLETED to if (completed) 1 else 0,
        )
        PebbleProtocol.sendToPebble(context, dict, transactionId)
    }

    private suspend fun handleToggleGroup(
        context: Context,
        data: Map<Int, Any>,
        transactionId: Int,
    ) {
        val high = (data[KEY_GROUP_VALUE_HIGH] as? Long)?.toInt() ?: 0
        val low = (data[KEY_GROUP_VALUE_LOW] as? Long)?.toInt() ?: 0
        val value = PebbleProtocol.combineLong(high, low)
        val collapsed = (data[KEY_GROUP_COLLAPSED] as? Long)?.toInt() != 0

        if (collapsed) {
            collapsedGroups.add(value)
        } else {
            collapsedGroups.remove(value)
        }
        watchServiceLogic.toggleGroup(value, collapsed)

        val dict = mapOf<Int, Any>(
            KEY_MSG_TYPE to RESP_TOGGLE_GROUP,
            KEY_TRANSACTION_ID to transactionId,
        )
        PebbleProtocol.sendToPebble(context, dict, transactionId)
    }

    private suspend fun handleGetLists(
        context: Context,
        data: Map<Int, Any>,
        transactionId: Int,
    ) {
        val position = (data[KEY_POSITION] as? Long)?.toInt() ?: 0
        val limit = (data[KEY_LIMIT] as? Long)?.toInt() ?: 0

        val result = watchServiceLogic.getLists(position, limit)

        val chunkCount = ceil(result.items.size.toDouble() / CHUNK_SIZE).toInt()
            .coerceAtLeast(1)

        for (chunkIndex in 0 until chunkCount) {
            val start = chunkIndex * CHUNK_SIZE
            val end = (start + CHUNK_SIZE).coerceAtMost(result.items.size)
            val chunkItems = result.items.subList(start, end)

            val dict = mutableMapOf<Int, Any>(
                KEY_MSG_TYPE to RESP_LISTS,
                KEY_TRANSACTION_ID to transactionId,
                KEY_TOTAL_ITEMS to result.totalItems,
                KEY_CHUNK_INDEX to chunkIndex,
                KEY_CHUNK_COUNT to chunkCount,
            )

            chunkItems.forEachIndexed { index, item ->
                val base = KEY_ITEM_BASE + index * ITEM_STRIDE
                when (item) {
                    is WatchListItem.Header -> {
                        dict[base + ITEM_TYPE] = TYPE_HEADER
                        dict[base + ITEM_TITLE] = PebbleProtocol.truncateTitle(item.title)
                    }
                    is WatchListItem.FilterItem -> {
                        dict[base + ITEM_TYPE] = TYPE_TASK
                        dict[base + ITEM_TITLE] = PebbleProtocol.truncateTitle(item.title)
                        dict[base + ITEM_EXTRA] = item.id
                        dict[base + ITEM_COMPLETED] = item.textColor
                        item.icon?.let { dict[base + KEY_LIST_ICON] = it }
                        dict[base + KEY_LIST_COLOR] = item.color
                        dict[base + KEY_LIST_COUNT] = item.taskCount
                    }
                }
            }

            PebbleProtocol.sendToPebble(context, dict, transactionId)
            if (chunkIndex < chunkCount - 1) {
                delay(CHUNK_DELAY_MS)
            }
        }
    }

    private suspend fun handleGetTask(
        context: Context,
        data: Map<Int, Any>,
        transactionId: Int,
    ) {
        val high = (data[KEY_TASK_ID_HIGH] as? Long)?.toInt() ?: 0
        val low = (data[KEY_TASK_ID_LOW] as? Long)?.toInt() ?: 0
        val taskId = PebbleProtocol.combineLong(high, low)

        val task = watchServiceLogic.getTask(taskId)

        val dict = mapOf<Int, Any>(
            KEY_MSG_TYPE to RESP_TASK,
            KEY_TRANSACTION_ID to transactionId,
            KEY_TASK_TITLE to PebbleProtocol.truncateTitle(task.title),
            KEY_TASK_PRIORITY to task.priority,
            KEY_TASK_COMPLETED to if (task.completed) 1 else 0,
            KEY_TASK_REPEATING to if (task.repeating) 1 else 0,
            KEY_TASK_DESCRIPTION to PebbleProtocol.truncateTitle(task.description),
        )
        PebbleProtocol.sendToPebble(context, dict, transactionId)
    }

    private suspend fun handleSaveTask(
        context: Context,
        data: Map<Int, Any>,
        transactionId: Int,
    ) {
        val high = (data[KEY_TASK_ID_HIGH] as? Long)?.toInt() ?: 0
        val low = (data[KEY_TASK_ID_LOW] as? Long)?.toInt() ?: 0
        val taskId = PebbleProtocol.combineLong(high, low)
        val title = data[KEY_TASK_TITLE] as? String ?: ""
        val completed = (data[KEY_TASK_COMPLETED] as? Long)?.toInt() != 0
        val filter = data[KEY_FILTER] as? String

        val resultId = watchServiceLogic.saveTask(
            taskId = taskId,
            title = title,
            completed = completed,
            filterPreference = filter,
            source = "pebble",
        )

        val (rHigh, rLow) = PebbleProtocol.splitLong(resultId)
        val dict = mapOf<Int, Any>(
            KEY_MSG_TYPE to RESP_SAVE_TASK,
            KEY_TRANSACTION_ID to transactionId,
            KEY_TASK_ID_HIGH to rHigh,
            KEY_TASK_ID_LOW to rLow,
        )
        PebbleProtocol.sendToPebble(context, dict, transactionId)
    }

    private suspend fun handleGetTaskCount(
        context: Context,
        data: Map<Int, Any>,
        transactionId: Int,
    ) {
        val filter = data[KEY_FILTER] as? String

        val result = watchServiceLogic.getTaskCount(
            filterPreference = filter,
            showHidden = false,
            showCompleted = false,
        )

        val dict = mapOf<Int, Any>(
            KEY_MSG_TYPE to RESP_TASK_COUNT,
            KEY_TRANSACTION_ID to transactionId,
            KEY_TASK_COUNT to result.count,
            KEY_TASK_COMPLETED_COUNT to result.completedCount,
        )
        PebbleProtocol.sendToPebble(context, dict, transactionId)
    }
}
