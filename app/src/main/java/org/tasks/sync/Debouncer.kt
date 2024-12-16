package org.tasks.sync

import kotlinx.coroutines.delay
import timber.log.Timber

class Debouncer(private val tag: String, private val block: suspend (Boolean) -> Unit) {
    private var count = 0

    suspend fun sync(immediate: Boolean) {
        if (immediate) {
            block(true)
        } else {
            val thisCount = ++count
            delay(1000)
            if (thisCount == count) {
                block(false)
            } else {
                Timber.v("debouncing $tag")
            }
        }
    }
}
