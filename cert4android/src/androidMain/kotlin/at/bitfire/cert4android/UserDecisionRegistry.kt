/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.bitfire.cert4android

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.cert.X509Certificate
import java.util.logging.Logger
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class UserDecisionRegistry private constructor(
    private val context: Context
) {

    companion object {

        @SuppressLint("StaticFieldLeak")
        private var instance: UserDecisionRegistry? = null

        @Synchronized
        fun getInstance(context: Context): UserDecisionRegistry {
            instance?.let {
                return it
            }

            val newInstance = UserDecisionRegistry(context.applicationContext)
            instance = newInstance
            return newInstance
        }

    }

    private val logger
        get() = Logger.getLogger(javaClass.name)

    internal val pendingDecisions = mutableMapOf<X509Certificate, MutableList<Continuation<Boolean>>>()

    /**
     * Tries to retrieve a trust decision from the user about a given certificate.
     *
     * Thread-safe, can handle multiple requests for various certificates and/or the same certificate at once.
     *
     * @param cert              certificate to ask user about
     * @param appInForeground   whether the app is currently in foreground = whether it can directly launch an Activity
     * @return *true* if the user explicitly trusts the certificate, *false* if unknown or untrusted
     */
    suspend fun check(cert: X509Certificate, appInForeground: Boolean): Boolean = suspendCancellableCoroutine { cont ->
        // check whether we're able to retrieve user feedback (= start an Activity and/or show a notification)
        val notificationsPermitted = NotificationUtils.notificationsPermitted(context)
        val userDecisionPossible = appInForeground || notificationsPermitted

        if (userDecisionPossible) {
            // User decision possible → remember request in pendingDecisions so that a later decision will be applied to this request

            cont.invokeOnCancellation {
                synchronized(pendingDecisions) {
                    val decisionsList = pendingDecisions[cert]

                    // remove from pending decisions on cancellation
                    decisionsList?.remove(cont)

                    // Remove decisions list if empty
                    if (decisionsList?.isEmpty() == true)
                        pendingDecisions -= cert
                }

                val nm = NotificationUtils.createChannels(context)
                nm.cancel(CertUtils.getTag(cert), NotificationUtils.ID_CERT_DECISION)
            }

            val requestDecision: Boolean
            synchronized(pendingDecisions) {
                if (pendingDecisions.containsKey(cert)) {
                    // There are already pending decisions for this request, just add our request
                    pendingDecisions[cert]!! += cont
                    requestDecision = false
                } else {
                    // First decision for this certificate, show UI
                    pendingDecisions[cert] = mutableListOf(cont)
                    requestDecision = true
                }
            }

            if (requestDecision)
                requestDecision(cert, launchActivity = appInForeground, showNotification = notificationsPermitted)

        } else {
            // We're not able to retrieve user feedback, directly reject request
            logger.warning("App not in foreground and missing notification permission, rejecting certificate")
            cont.resume(false)
        }
    }

    /**
     * Starts UI for retrieving feedback (accept/reject) for a certificate from the user.
     *
     * Ensure that required permissions are granted/conditions are met before setting [launchActivity]
     * or [showNotification].
     *
     * @param cert              certificate to ask user about
     * @param launchActivity    whether to launch a [TrustCertificateActivity]
     * @param showNotification  whether to show a certificate notification (caller must check notification permissions before passing *true*)
     *
     * @throws IllegalArgumentException  when both [launchActivity] and [showNotification] are *false*
     */
    @SuppressLint("MissingPermission")
    internal fun requestDecision(cert: X509Certificate, launchActivity: Boolean, showNotification: Boolean) {
        if (!launchActivity && !showNotification)
            throw IllegalArgumentException("User decision requires certificate Activity and/or notification")

        val rawCert = cert.encoded
        val decisionIntent = Intent(context, TrustCertificateActivity::class.java).apply {
            putExtra(TrustCertificateActivity.EXTRA_CERTIFICATE, rawCert)
        }

        if (showNotification) {
            val rejectIntent = Intent(context, TrustCertificateActivity::class.java).apply {
                putExtra(TrustCertificateActivity.EXTRA_CERTIFICATE, rawCert)
                putExtra(TrustCertificateActivity.EXTRA_TRUSTED, false)
            }

            val id = rawCert.contentHashCode()
            val notify = NotificationCompat.Builder(context, NotificationUtils.CHANNEL_CERTIFICATES)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_lock_open_white)
                .setContentTitle(context.getString(R.string.certificate_notification_connection_security))
                .setContentText(context.getString(R.string.certificate_notification_user_interaction))
                .setSubText(cert.subjectDN.name)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentIntent(
                    TaskStackBuilder.create(context)
                        .addNextIntent(decisionIntent)
                        .getPendingIntent(id, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                )
                .setDeleteIntent(
                    TaskStackBuilder.create(context)
                        .addNextIntent(rejectIntent)
                        .getPendingIntent(id + 1, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                )
                .build()

            val nm = NotificationUtils.createChannels(context)
            nm.notify(CertUtils.getTag(cert), NotificationUtils.ID_CERT_DECISION, notify)
        }

        if (launchActivity) {
            decisionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(decisionIntent)
        }
    }

    fun onUserDecision(cert: X509Certificate, trusted: Boolean) {
        // cancel notification
        val nm = NotificationUtils.createChannels(context)
        nm.cancel(CertUtils.getTag(cert), NotificationUtils.ID_CERT_DECISION)

        // save decision
        val customCertStore = CustomCertStore.getInstance(context)
        if (trusted)
            customCertStore.setTrustedByUser(cert)
        else
            customCertStore.setUntrustedByUser(cert)

        // continue work that's waiting for decisions
        synchronized(pendingDecisions) {
            pendingDecisions[cert]?.iterator()?.let { iter ->
                while (iter.hasNext()) {
                    iter.next().resume(trusted)
                    iter.remove()
                }
            }

            // remove certificate from pendingDecisions so UI can be shown again in future
            pendingDecisions.remove(cert)
        }
    }

}
