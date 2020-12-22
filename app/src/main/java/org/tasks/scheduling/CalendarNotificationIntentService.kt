package org.tasks.scheduling

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.gcal.CalendarAlarmReceiver
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.calendars.CalendarEventProvider
import org.tasks.preferences.Preferences
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class CalendarNotificationIntentService : RecurringIntervalIntentService() {
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var calendarEventProvider: CalendarEventProvider
    @Inject @ApplicationContext lateinit var context: Context

    override val broadcastClass = Broadcast::class.java

    override suspend fun run() {
        val now = DateUtilities.now()
        val end = now + TimeUnit.DAYS.toMillis(1)
        for (event in calendarEventProvider.getEventsBetween(now, end)) {
            val eventAlarm = Intent(context, CalendarAlarmReceiver::class.java)
            eventAlarm.action = CalendarAlarmReceiver.BROADCAST_CALENDAR_REMINDER
            eventAlarm.data = Uri.parse(URI_PREFIX + "://" + event.id)
            val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    CalendarAlarmReceiver.REQUEST_CODE_CAL_REMINDER,
                    eventAlarm,
                    PendingIntent.FLAG_UPDATE_CURRENT)
            val reminderTime = event.start - FIFTEEN_MINUTES
            alarmManager.wakeup(reminderTime, pendingIntent)
            Timber.d("Scheduled reminder for %s at %s", event, reminderTime)
        }
    }

    override fun intervalMillis() =
            if (preferences.getBoolean(R.string.p_calendar_reminders, false))
                TimeUnit.HOURS.toMillis(12)
            else 0

    class Broadcast : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            enqueueWork(context)
        }
    }

    companion object {
        const val URI_PREFIX_POSTPONE = "cal-postpone"
        private val FIFTEEN_MINUTES = TimeUnit.MINUTES.toMillis(15)
        private const val URI_PREFIX = "cal-reminder"
        fun enqueueWork(context: Context?) {
            enqueueWork(
                    context!!,
                    CalendarNotificationIntentService::class.java,
                    JOB_ID_CALENDAR_NOTIFICATION,
                    Intent(context, CalendarNotificationIntentService::class.java))
        }
    }
}