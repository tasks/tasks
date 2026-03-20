package org.tasks.sync

import co.touchlab.kermit.Logger
import kotlinx.coroutines.delay

class Debouncer<T>(
    private val tag: String,
    private val default: T,
    private val merge: (T, T) -> T = { _, new -> new },
    private val block: suspend (T) -> Unit
) {
    private var count = 0
    private var pending: T = default

    suspend fun sync(value: T) {
        pending = merge(pending, value)
        val thisCount = ++count
        delay(1000)
        if (thisCount == count) {
            val valueToUse = pending
            pending = default
            block(valueToUse)
        } else {
            Logger.v("Debouncer") { "debouncing $tag" }
        }
    }
}
