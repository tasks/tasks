package org.tasks.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import org.tasks.jobs.WorkManager
import org.tasks.sync.SyncSource
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TasksFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var workManager: WorkManager
    @Inject lateinit var pushTokenManager: PushTokenManager

    override fun onMessageReceived(message: RemoteMessage) {
        Timber.d("FCM message received: keys=%s", message.data.keys)
        if (message.data["sync"] == "true") {
            runBlocking {
                workManager.sync(SyncSource.PUSH_NOTIFICATION)
            }
        }
    }

    override fun onNewToken(token: String) {
        Timber.d("New FCM token")
        pushTokenManager.registerTokenForAllAccounts()
    }
}
