package org.tasks.compose.settings

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope

internal fun <T> roundTrip(saver: Saver<T, String>, value: T): T? {
    val saved = with(saver) { SaverScope { true }.save(value) }
    return saver.restore(saved!!)
}
