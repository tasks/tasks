package org.tasks.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.tasks.jobs.WorkManager
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TasksFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var workManager: WorkManager
    @Inject lateinit var pushTokenManager: PushTokenManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onMessageReceived(message: RemoteMessage) {
        Timber.d("FCM message received: keys=%s", message.data.keys)
        if (message.data["sync"] == "true") {
            scope.launch {
                workManager.sync(immediate = true)
            }
        }
    }

    override fun onNewToken(token: String) {
        Timber.d("New FCM token")
        pushTokenManager.registerTokenForAllAccounts()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
