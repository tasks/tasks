package org.tasks.reminders

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.dao.TaskDao
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch
import org.tasks.injection.InjectingAppCompatActivity
import org.tasks.intents.TaskIntents
import org.tasks.notifications.NotificationManager
import org.tasks.receivers.CompleteTaskReceiver
import org.tasks.themes.ThemeAccent
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActivity : InjectingAppCompatActivity(), NotificationDialog.NotificationHandler {
    @Inject lateinit var notificationManager: NotificationManager
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var themeAccent: ThemeAccent

    private var taskId: Long = 0
    private var disposables: CompositeDisposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeAccent.applyStyle(theme)
        val intent = intent
        taskId = intent.getLongExtra(EXTRA_TASK_ID, 0L)
        val fragmentManager = supportFragmentManager
        var fragment = fragmentManager.findFragmentByTag(FRAG_TAG_NOTIFICATION_FRAGMENT) as NotificationDialog?
        if (fragment == null) {
            fragment = NotificationDialog()
            fragment.show(fragmentManager, FRAG_TAG_NOTIFICATION_FRAGMENT)
        }
        fragment.setTitle(intent.getStringExtra(EXTRA_TITLE))
    }

    override fun dismiss() {
        finish()
    }

    override fun onResume() {
        super.onResume()
        disposables = CompositeDisposable()
    }

    override fun onPause() {
        super.onPause()
        disposables!!.dispose()
    }

    override fun edit() {
        lifecycleScope.launch {
            notificationManager.cancel(taskId)
            taskDao.fetch(taskId)
                    ?.let {
                        startActivity(
                                TaskIntents.getEditTaskIntent(this@NotificationActivity, null, it))
                    }
                    ?: Timber.e("Failed to find task $taskId")
            finish()
        }
    }

    override fun snooze() {
        finish()
        startActivity(SnoozeActivity.newIntent(this, taskId))
    }

    override fun complete() {
        val intent = Intent(this, CompleteTaskReceiver::class.java)
        intent.putExtra(CompleteTaskReceiver.TASK_ID, taskId)
        sendBroadcast(intent)
        finish()
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_TASK_ID = "extra_task_id"
        private const val FRAG_TAG_NOTIFICATION_FRAGMENT = "frag_tag_notification_fragment"
        fun newIntent(context: Context?, title: String?, id: Long): Intent {
            val intent = Intent(context, NotificationActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent.putExtra(EXTRA_TASK_ID, id)
            intent.putExtra(EXTRA_TITLE, title)
            return intent
        }
    }
}