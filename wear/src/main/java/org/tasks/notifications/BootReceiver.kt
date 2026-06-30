/**
 * BootReceiver.kt — Reschedules task reminders after device reboot.
 *
 * [AlarmManager] alarms are cleared on reboot. This receiver listens
 * for [Intent.ACTION_BOOT_COMPLETED] and calls
 * [WearNotificationManager.rescheduleAll] to re-register alarms for
 * every pending reminder.
 *
 * Registered in `AndroidManifest.xml` with the `RECEIVE_BOOT_COMPLETED`
 * permission.
 */
package org.tasks.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * Reschedules all task reminders after device boot.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Timber.d("Boot completed - rescheduling all wear reminders")
            val manager = WearNotificationManager.getInstance(context)
            manager.createNotificationChannel()
            manager.rescheduleAll()
        }
    }
}
