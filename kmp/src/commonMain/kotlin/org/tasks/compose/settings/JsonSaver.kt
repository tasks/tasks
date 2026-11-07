package org.tasks.compose.settings

import androidx.compose.runtime.saveable.Saver
import co.touchlab.kermit.Logger
import kotlinx.serialization.json.Json

internal inline fun <reified T> jsonSaver(
    tag: String,
    what: String,
    crossinline fallback: (T?) -> T,
): Saver<T, String> = Saver(
    save = { Json.encodeToString(it) },
    restore = { saved ->
        val restored = try {
            Json.decodeFromString<T>(saved)
        } catch (e: Exception) {
            Logger.e(e, tag = tag) { "Could not read back the saved $what" }
            null
        }
        fallback(restored)
    },
)
