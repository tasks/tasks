package org.tasks.pebble

import android.content.Context
import com.getpebble.android.kit.PebbleKit
import com.getpebble.android.kit.util.PebbleDictionary
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import org.tasks.injection.ApplicationScope
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PebbleService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val messageHandler: PebbleMessageHandler,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun register() {
        PebbleKit.registerReceivedDataHandler(
            context,
            object : PebbleKit.PebbleDataReceiver(PebbleProtocol.APP_UUID) {
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
                        Timber.e(e, "Failed to handle Pebble message")
                    }
                }
            }
        )
        Timber.d("Pebble data receiver registered")
    }
}
