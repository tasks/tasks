package org.tasks.pebble

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

class PebbleRefresher @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun refresh() {
        try {
            val dict = mapOf<Int, Any>(
                PebbleProtocol.KEY_MSG_TYPE to PebbleProtocol.MSG_REFRESH,
            )
            PebbleProtocol.sendToPebble(context, dict, 0)
        } catch (e: Exception) {
            Timber.e(e, "Failed to send Pebble refresh")
        }
    }
}
