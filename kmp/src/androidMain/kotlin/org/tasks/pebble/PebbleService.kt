package org.tasks.pebble

import android.content.Context
import android.content.IntentFilter
import co.touchlab.kermit.Logger
import com.getpebble.android.kit.PebbleKit
import com.getpebble.android.kit.util.PebbleDictionary
import kotlinx.coroutines.CoroutineScope

class PebbleService(
    private val context: Context,
    private val messageHandler: PebbleMessageHandler,
    private val scope: CoroutineScope,
) {
    private var registered = false

    fun register() {
        if (registered) return
        registered = true
        val receiver = object : PebbleKit.PebbleDataReceiver(PebbleProtocol.APP_UUID) {
            override fun receiveData(
                context: Context,
                transactionId: Int,
                data: PebbleDictionary,
            ) {
                PebbleKit.sendAckToPebble(context, transactionId)
                try {
                    val map = PebbleProtocol.toMap(data)
                    messageHandler.handleMessage(context, map, transactionId, scope)
                } catch (e: Exception) {
                    Logger.e("PEBBLE", e) { "Failed to handle Pebble message" }
                }
            }
        }
        context.registerReceiver(
            receiver,
            IntentFilter("com.getpebble.action.app.RECEIVE"),
            Context.RECEIVER_EXPORTED,
        )
        Logger.d("PEBBLE") { "data receiver registered" }
    }
}
