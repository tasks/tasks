package org.tasks.fcm

import co.touchlab.kermit.Logger
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.tasks.jobs.BackgroundWork
import org.tasks.sync.SyncSource

private const val TAG = "FCM"

class TasksFirebaseMessagingService : FirebaseMessagingService() {

    private val backgroundWork: BackgroundWork by inject()
    private val pushTokenManager: PushTokenManager by inject()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onMessageReceived(message: RemoteMessage) {
        Logger.d(TAG) { "FCM message received: keys=${message.data.keys}" }
        if (message.data["sync"] == "true") {
            scope.launch {
                backgroundWork.sync(SyncSource.PUSH_NOTIFICATION)
            }
        }
    }

    override fun onNewToken(token: String) {
        Logger.d(TAG) { "New FCM token" }
        pushTokenManager.registerTokenForAllAccounts()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
