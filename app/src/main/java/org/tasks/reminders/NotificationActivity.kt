package org.tasks.reminders

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.todoroo.astrid.dao.TaskDaoBlocking
import com.todoroo.astrid.data.Task
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
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
    @Inject lateinit var taskDao: TaskDaoBlocking
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
        notificationManager.cancel(taskId)
        disposables!!.add(
                Single.fromCallable { taskDao.fetchBlocking(taskId) }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { task: Task? ->
                                    startActivity(TaskIntents.getEditTaskIntent(this, null, task))
                                    finish()
                                }
                        ) { Timber.e("Task not found: $taskId") })
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