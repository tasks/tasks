package org.tasks.fcm

import co.touchlab.kermit.Logger
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "FcmTokenProvider"

class GooglePlayFcmTokenProvider : FcmTokenProvider {
    override suspend fun getToken(): String? {
        return try {
            suspendCancellableCoroutine { cont ->
                FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
        } catch (e: Exception) {
            Logger.e(e, tag = TAG) { "Failed to get FCM token" }
            null
        }
    }
}
