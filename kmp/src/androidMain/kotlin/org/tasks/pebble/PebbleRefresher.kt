package org.tasks.pebble

import android.content.Context
import co.touchlab.kermit.Logger

class PebbleRefresher(
    private val context: Context,
) {
    fun refresh() {
        try {
            val dict = mapOf<Int, Any>(
                PebbleProtocol.KEY_MSG_TYPE to PebbleProtocol.MSG_REFRESH,
            )
            PebbleProtocol.sendToPebble(context, dict, 0)
        } catch (e: Exception) {
            Logger.e("PEBBLE", e) { "Failed to send Pebble refresh" }
        }
    }
}
