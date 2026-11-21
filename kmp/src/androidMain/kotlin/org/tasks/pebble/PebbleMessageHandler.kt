package org.tasks.pebble

import android.content.Context
import co.touchlab.kermit.Logger
import com.getpebble.android.kit.PebbleKit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.tasks.analytics.Analytics
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
import org.tasks.pebble.PebbleProtocol.MSG_TOGGLE_LIST
import org.tasks.pebble.PebbleProtocol.RESP_COMPLETE_TASK
import org.tasks.pebble.PebbleProtocol.RESP_LISTS
import org.tasks.pebble.PebbleProtocol.RESP_SAVE_TASK
import org.tasks.pebble.PebbleProtocol.RESP_TASK
import org.tasks.pebble.PebbleProtocol.RESP_TASKS
import org.tasks.pebble.PebbleProtocol.RESP_TASK_COUNT
import org.tasks.pebble.PebbleProtocol.RESP_TOGGLE_GROUP
import org.tasks.pebble.PebbleProtocol.RESP_TOGGLE_LIST
import org.tasks.pebble.PebbleProtocol.TYPE_HEADER
import org.tasks.pebble.PebbleProtocol.TYPE_TASK
import org.tasks.resources.getString
import org.tasks.watch.WatchListItem
import org.tasks.watch.WatchService
import org.tasks.watch.WatchUiItem
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.event_pebble
import tasks.kmp.generated.resources.param_device_model
import kotlin.math.ceil

class PebbleMessageHandler(
    private val watchService: WatchService,
    private val analytics: Analytics,
) {
    companion object {
        private const val CHUNK_DELAY_MS = 100L
        // Small-mode chunks are one item (~117 bytes). Measured against the
        // Rebble phone app: 30ms pacing streamed fine for a few pages, then
        // 14 sends inside a single second stalled its PebbleKit pipeline
        // dead -- no AppMessage flowed in either direction again and the
        // watch's outbox sat on APP_MSG_BUSY until relaunch. Keep small
        // chunks at the same proven spacing.
        private const val SMALL_CHUNK_DELAY_MS = 100L
        private const val HEADER_COMPLETED = -2L
        private const val VERBOSE_LOGGING = false
    }

    private val collapsedGroups = mutableSetOf(HEADER_COMPLETED)
    private val collapsedLists = mutableSetOf<String>()
    private var lastFilter: String? = null
    // Session-based replay detection. The watch generates a random session ID
    // on each launch and includes it in state-changing messages. Replayed
    // messages from a previous session carry the old session ID, allowing
    // the phone to identify and block them.
    private var currentSessionId = 0L
    private val processedActions = mutableSetOf<Int>()

    // Small-message (aplite) mode sends "w<index>" tokens instead of filter
    // ids, since ids like Google Tasks list ids are longer than a
    // 124-byte AppMessage can carry. Every lists response refreshes the
    // mapping; entries are dropped when the process dies, at which point the
    // watch has to re-open the filter picker to select a list again.
    private val listIds = mutableMapOf<String, String>()

    // Token received back from the watch → real filter id; a watch built
    // before small mode exists never sends tokens, so this is a passthrough
    // for it
    private fun resolveFilterId(raw: String?): String? =
        if (raw != null && PebbleProtocol.isListToken(raw)) {
            listIds[raw] ?: raw
        } else {
            raw
        }

    private fun truncateTaskTitle(title: String?, data: Map<Int, Any>): String =
        if (PebbleProtocol.isSmallMode(data)) {
            PebbleProtocol.truncateBytes(title, PebbleProtocol.SMALL_MAX_TASK_TITLE)
        } else {
            PebbleProtocol.truncateTitle(title)
        }

    private fun truncateListTitle(title: String?, data: Map<Int, Any>): String =
        if (PebbleProtocol.isSmallMode(data)) {
            PebbleProtocol.truncateBytes(title, PebbleProtocol.SMALL_MAX_LIST_TITLE)
        } else {
            PebbleProtocol.truncateTitle(title)
        }

    fun handleMessage(
        context: Context,
        data: Map<Int, Any>,
        transactionId: Int,
        scope: CoroutineScope,
    ) {
        val msgType = (data[KEY_MSG_TYPE] as? Long)?.toInt() ?: return
        // Use protocol txn from the watch, not PebbleKit's system txn
        val protocolTxn = (data[KEY_TRANSACTION_ID] as? Long)?.toInt() ?: transactionId

        val isStateChanging = msgType == MSG_COMPLETE_TASK
                || msgType == MSG_SAVE_TASK || msgType == MSG_TOGGLE_GROUP
                || msgType == MSG_TOGGLE_LIST
        if (isStateChanging) {
            val sessionId = (data[PebbleProtocol.KEY_SESSION_ID] as? Long) ?: 0L
            if (sessionId != currentSessionId) {
                // New session — clear old dedup cache
                currentSessionId = sessionId
                processedActions.clear()
                Logger.d("PEBBLE") { "new session: $sessionId" }
            }
            val key = (msgType shl 8) or (protocolTxn and 0xFF)
            if (!processedActions.add(key)) {
                Logger.d("PEBBLE") { "ignoring replayed msg=$msgType txn=$protocolTxn session=$sessionId" }
                return
            }
        }

        scope.launch {
            logPebbleEvent(context)
            try {
                when (msgType) {
                    MSG_GET_TASKS -> handleGetTasks(context, data, protocolTxn)
                    MSG_COMPLETE_TASK -> handleCompleteTask(context, data, protocolTxn)
                    MSG_TOGGLE_GROUP -> handleToggleGroup(context, data, protocolTxn)
                    MSG_GET_LISTS -> handleGetLists(context, data, protocolTxn)
                    MSG_TOGGLE_LIST -> handleToggleList(context, data, protocolTxn)
                    MSG_GET_TASK -> handleGetTask(context, data, protocolTxn)
                    MSG_SAVE_TASK -> handleSaveTask(context, data, protocolTxn)
                    MSG_GET_TASK_COUNT -> handleGetTaskCount(context, data, protocolTxn)
                    else -> Logger.w("PEBBLE") { "Unknown Pebble message type: $msgType" }
                }
            } catch (e: Exception) {
                Logger.e("PEBBLE", e) { "Error handling Pebble message type $msgType" }
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
        val filter = resolveFilterId(data[KEY_FILTER] as? String)
        val sortMode = (data[PebbleProtocol.KEY_SORT_MODE] as? Long)?.toInt()
        val groupMode = (data[PebbleProtocol.KEY_GROUP_MODE] as? Long)?.toInt()
        val showHidden = (data[PebbleProtocol.KEY_SHOW_HIDDEN] as? Long)?.toInt() == 1
        val showCompleted = (data[PebbleProtocol.KEY_SHOW_COMPLETED] as? Long)?.toInt() == 1
        val small = PebbleProtocol.isSmallMode(data)
        val chunkSize = if (small) PebbleProtocol.SMALL_CHUNK_SIZE else CHUNK_SIZE

        if (VERBOSE_LOGGING) Logger.d("PEBBLE") { "GET_TASKS: pos=$position limit=$limit filter=$filter sort=$sortMode group=$groupMode hidden=$showHidden completed=$showCompleted collapsed=$collapsedGroups txn=$transactionId" }

        if (filter != lastFilter) {
            lastFilter = filter
            collapsedGroups.clear()
            collapsedGroups.add(HEADER_COMPLETED)
            if (VERBOSE_LOGGING) Logger.d("PEBBLE") { "filter changed, reset collapsed to $collapsedGroups" }
        }

        val result = watchService.getTasks(
            filterPreference = filter,
            position = position,
            limit = limit,
            showHidden = showHidden,
            showCompleted = showCompleted,
            sortMode = sortMode,
            groupMode = groupMode,
            collapsed = collapsedGroups,
        )

        if (VERBOSE_LOGGING) {
            Logger.d("PEBBLE") { "result: ${result.items.size} items, totalItems=${result.totalItems}" }
            result.items.forEachIndexed { i, item ->
                when (item) {
                    is WatchUiItem.Header -> Logger.d("PEBBLE") { "  [$i] HEADER id=${item.id} title=${item.title} collapsed=${item.collapsed}" }
                    is WatchUiItem.Task -> Logger.d("PEBBLE") { "  [$i] TASK id=${item.id} title=${item.title}" }
                }
            }
        }

        val chunkCount = ceil(result.items.size.toDouble() / chunkSize).toInt()
            .coerceAtLeast(1)

        if (VERBOSE_LOGGING) Logger.d("PEBBLE") { "sending $chunkCount chunks" }

        for (chunkIndex in 0 until chunkCount) {
            val start = chunkIndex * chunkSize
            val end = (start + chunkSize).coerceAtMost(result.items.size)
            val chunkItems = result.items.subList(start, end)

            val dict = chunkHeader(
                small, RESP_TASKS, transactionId, result.totalItems,
                chunkIndex, chunkCount,
            )

            chunkItems.forEachIndexed { index, item ->
                val base = KEY_ITEM_BASE + index * ITEM_STRIDE
                when (item) {
                    is WatchUiItem.Header -> {
                        val (high, low) = PebbleProtocol.splitLong(item.id)
                        dict[base + ITEM_ID_HIGH] = high
                        dict[base + ITEM_ID_LOW] = low
                        dict[base + ITEM_TYPE] = intTuple(small, TYPE_HEADER)
                        dict[base + ITEM_TITLE] = truncateListTitle(item.title, data)
                        dict[base + ITEM_COLLAPSED] = intTuple(small, if (item.collapsed) 1 else 0)
                    }
                    is WatchUiItem.Task -> {
                        val (high, low) = PebbleProtocol.splitLong(item.id)
                        dict[base + ITEM_ID_HIGH] = high
                        dict[base + ITEM_ID_LOW] = low
                        dict[base + ITEM_TYPE] = intTuple(small, TYPE_TASK)
                        dict[base + ITEM_TITLE] = truncateTaskTitle(item.title, data)
                        dict[base + ITEM_PRIORITY] = intTuple(small, item.priority)
                        dict[base + ITEM_COMPLETED] = intTuple(small, if (item.completed) 1 else 0)
                        dict[base + ITEM_INDENT] = intTuple(small, item.indent)
                        dict[base + ITEM_COLLAPSED] = intTuple(small, if (item.collapsed) 1 else 0)
                        dict[base + ITEM_NUM_SUBTASKS] = intTuple(small, item.numSubtasks and 0x7F)
                        // Timestamps don't fit a small message alongside the
                        // rest of the item; the watch can live without them
                        if (!small && item.timestamp != null) {
                            dict[base + ITEM_EXTRA] = PebbleProtocol.truncateTitle(item.timestamp)
                        }
                    }
                }
            }

            PebbleProtocol.sendToPebble(context, dict, transactionId)
            if (chunkIndex < chunkCount - 1) {
                delay(if (small) SMALL_CHUNK_DELAY_MS else CHUNK_DELAY_MS)
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

        watchService.completeTask(taskId, completed, "pebble")

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
        val collapsed = (data[KEY_GROUP_COLLAPSED] as? Long)?.toInt() == 1

        if (collapsed) {
            collapsedGroups.add(value)
        } else {
            collapsedGroups.remove(value)
        }

        val dict = mapOf<Int, Any>(
            KEY_MSG_TYPE to RESP_TOGGLE_GROUP,
            KEY_TRANSACTION_ID to transactionId,
        )
        PebbleProtocol.sendToPebble(context, dict, transactionId)
    }

    private fun realId(item: WatchListItem): String = when (item) {
        is WatchListItem.Header -> item.id
        is WatchListItem.FilterItem -> item.id
    }

    // uint8 tuple in small mode, uint32 otherwise — must round to a width the
    // watch build reads (protocol_tuple_value accepts either)
    private fun intTuple(small: Boolean, value: Int): Any =
        if (small) PebbleProtocol.SmallInt(value) else value

    private fun chunkHeader(
        small: Boolean,
        responseType: Int,
        transactionId: Int,
        totalItems: Int,
        chunkIndex: Int,
        chunkCount: Int,
    ): MutableMap<Int, Any> = mutableMapOf(
        KEY_MSG_TYPE to intTuple(small, responseType),
        KEY_TRANSACTION_ID to intTuple(small, transactionId),
        // Can exceed 255 -- stays uint32
        KEY_TOTAL_ITEMS to totalItems,
        KEY_CHUNK_INDEX to intTuple(small, chunkIndex),
        KEY_CHUNK_COUNT to intTuple(small, chunkCount),
    )

    private suspend fun handleGetLists(
        context: Context,
        data: Map<Int, Any>,
        transactionId: Int,
    ) {
        val position = (data[KEY_POSITION] as? Long)?.toInt() ?: 0
        val limit = (data[KEY_LIMIT] as? Long)?.toInt() ?: 0
        val small = PebbleProtocol.isSmallMode(data)
        val chunkSize = if (small) PebbleProtocol.SMALL_CHUNK_SIZE else CHUNK_SIZE

        val result = watchService.getLists(position, limit, collapsedLists)

        val chunkCount = ceil(result.items.size.toDouble() / chunkSize).toInt()
            .coerceAtLeast(1)

        for (chunkIndex in 0 until chunkCount) {
            val start = chunkIndex * chunkSize
            val end = (start + chunkSize).coerceAtMost(result.items.size)
            val chunkItems = result.items.subList(start, end)

            val dict = chunkHeader(
                small, RESP_LISTS, transactionId, result.totalItems,
                chunkIndex, chunkCount,
            )

            chunkItems.forEachIndexed { index, item ->
                val base = KEY_ITEM_BASE + index * ITEM_STRIDE
                // Fully-qualified filter ids can be longer than a small
                // message can carry, so the watch stores a token instead and
                // sends it back; resolveFilterId() maps it to the real id
                val id = if (small) {
                    val token = PebbleProtocol.listToken(position + start + index)
                    listIds[token] = realId(item)
                    token
                } else {
                    realId(item)
                }
                when (item) {
                    is WatchListItem.Header -> {
                        dict[base + ITEM_TYPE] = intTuple(small, TYPE_HEADER)
                        dict[base + ITEM_TITLE] = truncateListTitle(item.title, data)
                        dict[base + ITEM_EXTRA] = id
                        dict[base + ITEM_COLLAPSED] = intTuple(small, if (item.collapsed) 1 else 0)
                    }
                    is WatchListItem.FilterItem -> {
                        dict[base + ITEM_TYPE] = intTuple(small, TYPE_TASK)
                        dict[base + ITEM_TITLE] = truncateListTitle(item.title, data)
                        dict[base + ITEM_EXTRA] = id
                        // Stored as int colors -- stays uint32
                        dict[base + ITEM_COMPLETED] = item.textColor
                        if (!small) {
                            item.icon?.let { dict[base + KEY_LIST_ICON] = it }
                            dict[base + KEY_LIST_COUNT] = item.taskCount
                        }
                        dict[base + KEY_LIST_COLOR] = item.color
                    }
                }
            }

            PebbleProtocol.sendToPebble(context, dict, transactionId)
            if (chunkIndex < chunkCount - 1) {
                delay(if (small) SMALL_CHUNK_DELAY_MS else CHUNK_DELAY_MS)
            }
        }
    }

    private suspend fun handleToggleList(
        context: Context,
        data: Map<Int, Any>,
        transactionId: Int,
    ) {
        val id = resolveFilterId(data[KEY_FILTER] as? String)
        val collapsed = (data[KEY_GROUP_COLLAPSED] as? Long)?.toInt() == 1

        if (id != null) {
            if (collapsed) {
                collapsedLists.add(id)
            } else {
                collapsedLists.remove(id)
            }
        }

        val dict = mapOf<Int, Any>(
            KEY_MSG_TYPE to RESP_TOGGLE_LIST,
            KEY_TRANSACTION_ID to transactionId,
        )
        PebbleProtocol.sendToPebble(context, dict, transactionId)
    }

    private suspend fun handleGetTask(
        context: Context,
        data: Map<Int, Any>,
        transactionId: Int,
    ) {
        val high = (data[KEY_TASK_ID_HIGH] as? Long)?.toInt() ?: 0
        val low = (data[KEY_TASK_ID_LOW] as? Long)?.toInt() ?: 0
        val taskId = PebbleProtocol.combineLong(high, low)

        val task = watchService.getTask(taskId)

        val small = PebbleProtocol.isSmallMode(data)
        // Compact encoding in small mode (uint8 tuples); a 124-byte message
        // holds the msgtype/txn (6+6), title (6+24), priority/completed/
        // repeating flags (3x6), and a 50-byte description (5+50+1)
        val dict = mutableMapOf<Int, Any>(
            KEY_MSG_TYPE to intTuple(small, RESP_TASK),
            KEY_TRANSACTION_ID to intTuple(small, transactionId),
        )
        if (small) {
            dict[KEY_TASK_TITLE] =
                PebbleProtocol.truncateBytes(task.title, PebbleProtocol.SMALL_MAX_TASK_TITLE)
            dict[KEY_TASK_DESCRIPTION] =
                PebbleProtocol.truncateBytes(task.description, PebbleProtocol.SMALL_MAX_DESC)
        } else {
            dict[KEY_TASK_TITLE] = PebbleProtocol.truncateTitle(task.title)
            dict[KEY_TASK_DESCRIPTION] = PebbleProtocol.truncateTitle(task.description)
        }
        dict[KEY_TASK_PRIORITY] = intTuple(small, task.priority)
        dict[KEY_TASK_COMPLETED] = intTuple(small, if (task.completed) 1 else 0)
        dict[KEY_TASK_REPEATING] = intTuple(small, if (task.repeating) 1 else 0)
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
        val filter = resolveFilterId(data[KEY_FILTER] as? String)

        val resultId = watchService.saveTask(
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
        val filter = resolveFilterId(data[KEY_FILTER] as? String)

        val result = watchService.getTaskCount(
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

    private suspend fun logPebbleEvent(context: Context) {
        val params = try {
            val fw = PebbleKit.getWatchFWVersion(context)
            if (fw != null) {
                arrayOf(getString(Res.string.param_device_model) to fw.tag as Any)
            } else {
                emptyArray()
            }
        } catch (e: Exception) {
            emptyArray<Pair<String, Any>>()
        }
        analytics.logEventOncePerDay(getString(Res.string.event_pebble), *params)
    }
}
