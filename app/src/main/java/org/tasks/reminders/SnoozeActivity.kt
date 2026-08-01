package org.tasks.reminders

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.alarms.AlarmService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.activities.DateAndTimePickerActivity
import org.tasks.analytics.Firebase
import org.tasks.compose.pickers.SnoozeDialog
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.preferences.Preferences
import org.tasks.themes.TasksTheme
import org.tasks.themes.Theme
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.ONE_MINUTE
import javax.inject.Inject

@AndroidEntryPoint
class SnoozeActivity : AppCompatActivity() {
    @Inject lateinit var alarmService: AlarmService
    @Inject lateinit var firebase: Firebase
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var theme: Theme

    private val taskIds: MutableList<Long> = ArrayList()
    private var pickingDateTime = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        if (intent.hasExtra(EXTRA_TASK_ID)) {
            taskIds.add(intent.getLongExtra(EXTRA_TASK_ID, -1L))
        } else if (intent.hasExtra(EXTRA_TASK_IDS)) {
            taskIds.addAll(intent.getSerializableExtra(EXTRA_TASK_IDS) as ArrayList<Long>)
        }
        if (savedInstanceState != null) {
            pickingDateTime = savedInstanceState.getBoolean(EXTRA_PICKING_DATE_TIME, false)
            if (pickingDateTime) {
                return
            }
        }
        if (intent.hasExtra(EXTRA_SNOOZE_TIME)) {
            snoozeForTime(intent.getLongExtra(EXTRA_SNOOZE_TIME, 0L))
            return
        }
        setContent {
            TasksTheme(
                theme = theme.themeBase.index,
                primary = theme.themeColor.primaryColor,
            ) {
                var visible by remember { mutableStateOf(true) }
                SnoozeDialog(
                    visible = visible,
                    loadTimes = { preferences.quickPickTimes },
                    is24Hour = is24HourFormat,
                    onSelected = { snoozeForTime(it) },
                    onPickDateTime = {
                        visible = false
                        pickDateTime()
                    },
                    onDismiss = { finish() },
                )
            }
        }
    }

    private fun snoozeForTime(time: Long) {
        firebase.logEvent(R.string.event_notification, R.string.param_type to "snooze_time")
        lifecycleScope.launch(NonCancellable) {
            alarmService.snooze(time, taskIds)
        }
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(EXTRA_PICKING_DATE_TIME, pickingDateTime)
    }

    private fun pickDateTime() {
        pickingDateTime = true
        val intent = Intent(this, DateAndTimePickerActivity::class.java)
        intent.putExtra(
            DateAndTimePickerActivity.EXTRA_TIMESTAMP,
            currentTimeMillis() + PICKER_OFFSET,
        )
        startActivityForResult(intent, REQUEST_DATE_TIME)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_DATE_TIME) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                snoozeForTime(data.getLongExtra(DateAndTimePickerActivity.EXTRA_TIMESTAMP, 0L))
            } else {
                finish()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        private const val FLAGS = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        const val EXTRA_TASK_ID = "id"
        const val EXTRA_TASK_IDS = "ids"
        const val EXTRA_SNOOZE_TIME = "snooze_time"
        private const val EXTRA_PICKING_DATE_TIME = "extra_picking_date_time"
        private const val REQUEST_DATE_TIME = 10101

        private val PICKER_OFFSET = 30 * ONE_MINUTE

        fun newIntent(context: Context?, id: Long?): Intent =
                Intent(context, SnoozeActivity::class.java).apply {
                    flags = FLAGS
                    putExtra(EXTRA_TASK_ID, id)
                }

        fun newIntent(context: Context?, ids: List<Long?>?): Intent =
                Intent(context, SnoozeActivity::class.java).apply {
                    flags = FLAGS
                    putExtra(EXTRA_TASK_IDS, ArrayList<Any?>(ids!!))
                }
    }
}
