package org.tasks.reminders

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.alarms.AlarmService
import com.todoroo.astrid.dao.TaskDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import org.tasks.activities.DateAndTimePickerActivity
import org.tasks.dialogs.MyTimePickerDialog
import org.tasks.themes.ThemeAccent
import org.tasks.time.DateTime
import javax.inject.Inject

@AndroidEntryPoint
class SnoozeActivity : AppCompatActivity(), SnoozeCallback, DialogInterface.OnCancelListener {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var alarmService: AlarmService
    @Inject lateinit var themeAccent: ThemeAccent

    private val taskIds: MutableList<Long> = ArrayList()
    private var pickingDateTime = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeAccent.applyStyle(theme)
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
            snoozeForTime(DateTime(intent.getLongExtra(EXTRA_SNOOZE_TIME, 0L)))
        } else {
            val fragmentManager = supportFragmentManager
            var fragmentByTag = fragmentManager.findFragmentByTag(FRAG_TAG_SNOOZE_DIALOG) as SnoozeDialog?
            if (fragmentByTag == null) {
                fragmentByTag = SnoozeDialog()
                fragmentByTag.show(fragmentManager, FRAG_TAG_SNOOZE_DIALOG)
            }
            fragmentByTag.setOnCancelListener(this)
            fragmentByTag.setSnoozeCallback(this)
        }
    }

    override fun snoozeForTime(time: DateTime) {
        lifecycleScope.launch(NonCancellable) {
            alarmService.snooze(time.millis, taskIds)
        }
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(EXTRA_PICKING_DATE_TIME, pickingDateTime)
    }

    override fun pickDateTime() {
        pickingDateTime = true
        val intent = Intent(this, DateAndTimePickerActivity::class.java)
        intent.putExtra(
                DateAndTimePickerActivity.EXTRA_TIMESTAMP, DateTime().plusMinutes(30).millis)
        startActivityForResult(intent, REQUEST_DATE_TIME)
    }

    override fun onCancel(dialog: DialogInterface) {
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_DATE_TIME) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val timestamp = data.getLongExtra(MyTimePickerDialog.EXTRA_TIMESTAMP, 0L)
                snoozeForTime(DateTime(timestamp))
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
        private const val FRAG_TAG_SNOOZE_DIALOG = "frag_tag_snooze_dialog"
        private const val EXTRA_PICKING_DATE_TIME = "extra_picking_date_time"
        private const val REQUEST_DATE_TIME = 10101
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