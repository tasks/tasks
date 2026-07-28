package org.tasks.viewmodel

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.ContinuationInterceptor

private const val TAG = "PendingTaskSaves"

/**
 * Tracks task editor saves that outlive the editor that started them.
 *
 * Navigating away from a task destroys its view model, so the save it triggers has to run on an
 * app-scoped coroutine with nothing left in the composition to observe it. This keeps that save
 * observable: [flushPending] and [awaitIdle] let shutdown ask for and then wait for it,
 * [saveFailures] surfaces failures somewhere still on screen, and [withLock] serializes it against
 * the next editor's load.
 */
class PendingTaskSaves(private val scope: CoroutineScope) {

    /** Per-task locks, keyed by save key, and the lock that guards the map itself. */
    private val locks = mutableMapOf<String, Mutex>()
    private val locksMutex = Mutex()

    private val lockDispatcher =
        scope.coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher
            ?: Dispatchers.Default

    private val inFlight = MutableStateFlow(0)
    private val inFlightByKey = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val flushHandlers = MutableStateFlow<List<FlushHandler>>(emptyList())

    private val _saveFailures = MutableStateFlow(0)

    /**
     * How many failed saves are still waiting to be shown.
     *
     * Deliberately sticky rather than an event stream: the saves that fail are the ones started by
     * an editor that has already gone away, and on Android the whole composition can go with it, so
     * an event published at that moment has nobody listening. A count survives until whatever comes
     * back on screen acknowledges it.
     */
    val saveFailures: StateFlow<Int> = _saveFailures.asStateFlow()

    private val _totalSaveFailures = MutableStateFlow(0)

    /**
     * Every failure ever reported, never decremented.
     *
     * [saveFailures] is a count of what is still owed to the user, so it goes down as well as up -
     * which makes it useless for "did anything fail since I looked", the question shutdown asks.
     * Comparing that count against a snapshot missed a failure whenever a snackbar acknowledged an
     * older one in between, and the app quit with the edit lost. This one only ever grows.
     */
    val totalSaveFailures: StateFlow<Int> = _totalSaveFailures.asStateFlow()

    private class FlushHandler(val key: String, val flush: () -> Unit)

    /**
     * Serializes work on one task, keyed by [key], so that a departing editor's save completes
     * before its replacement reads the same task back.
     *
     * Deliberately per-task rather than global: a save runs uncancellably and can take as long as
     * the calendar provider and sync adapters do, so a single shared lock would let one slow save
     * stall every other editor in the app.
     *
     * Runs on [lockDispatcher] rather than wherever it was called from. Switching here rather than
     * at the call sites is what makes that guarantee hold: the acquire and the release have to be
     * off the main thread, not just the work in between.
     */
    suspend fun <T> withLock(key: String, block: suspend () -> T): T =
        withContext(lockDispatcher) {
            val mutex = locksMutex.withLock { locks.getOrPut(key) { Mutex() } }
            mutex.withLock { block() }
        }

    /**
     * Registers an editor on [key] that can commit its in-progress edit on demand. Returns a handle
     * that the editor must call when it goes away.
     */
    fun registerFlushHandler(key: String, handler: () -> Unit): () -> Unit {
        val entry = FlushHandler(key, handler)
        flushHandlers.update { it + entry }
        return { flushHandlers.update { handlers -> handlers - entry } }
    }

    /**
     * Asks every live editor to commit what it is holding.
     *
     * Shutdown has to call this before [awaitIdle]: at that point the composition is still alive, so
     * no editor has been cleared or stopped and there is nothing enqueued to wait for yet.
     */
    fun flushPending() {
        flushHandlers.value.forEach { it.flush() }
    }

    /**
     * Asks any live editor on [key] to commit what it is holding.
     *
     * Called by a loading editor before it reads the row: the nav host builds a replacement editor
     * while the one it replaces is still alive and still holding an uncommitted edit, so without
     * this the replacement reads a row that is stale by whatever the user last typed.
     */
    fun flushPending(key: String) {
        flushHandlers.value.forEach { if (it.key == key) it.flush() }
    }

    /**
     * Runs [save] on the app scope while holding [key]'s lock, tracking it so [awaitIdle] and
     * [awaitPending] can wait for it. [save] must not take that lock itself - it is already held.
     *
     * The counters are bumped here, synchronously, and that - not the lock - is what orders this
     * save against a replacement editor's load: see [awaitPending].
     */
    fun enqueueLocked(key: String, save: suspend () -> Unit) {
        inFlight.update { it + 1 }
        inFlightByKey.update { current -> current + (key to (current[key] ?: 0) + 1) }
        // ATOMIC and NonCancellable so the bookkeeping below always runs: a coroutine that never
        // starts would leave inFlight above zero, hanging every awaitIdle for the rest of the
        // process.
        scope.launch(start = CoroutineStart.ATOMIC) {
            withContext(NonCancellable) {
                try {
                    withLock(key) { save() }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // The scope this runs on has no exception handler, and the editor that would
                    // have reported this is usually already gone.
                    Logger.e(e, tag = TAG) { "Enqueued save failed" }
                    reportSaveFailure()
                } finally {
                    inFlightByKey.update { current ->
                        val remaining = (current[key] ?: 1) - 1
                        if (remaining <= 0) current - key else current + (key to remaining)
                    }
                    inFlight.update { it - 1 }
                }
            }
        }
    }

    /** Suspends until every enqueued save has finished. */
    suspend fun awaitIdle() {
        inFlight.first { it == 0 }
    }

    /**
     * Suspends until every save enqueued for [key] before this call has finished.
     *
     * This, not the lock, is what orders a loading editor behind a departing one's save.
     * [enqueueLocked] increments the counter synchronously, so a [flushPending] immediately followed
     * by this provably waits for whatever that flush enqueued - however the enqueued save then
     * fares in the race for the mutex.
     */
    suspend fun awaitPending(key: String) {
        inFlightByKey.first { (it[key] ?: 0) == 0 }
    }

    /**
     * Reports a failed save. Each failure is counted separately so that a second one arriving while
     * the first is still on screen isn't swallowed.
     */
    fun reportSaveFailure() {
        _saveFailures.update { it + 1 }
        _totalSaveFailures.update { it + 1 }
    }

    /** Marks one reported failure as shown. */
    fun acknowledgeSaveFailure() {
        _saveFailures.update { (it - 1).coerceAtLeast(0) }
    }
}
