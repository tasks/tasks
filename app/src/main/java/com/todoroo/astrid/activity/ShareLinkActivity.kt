package com.todoroo.astrid.activity

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.service.TaskCreator
import com.todoroo.astrid.utility.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.analytics.Firebase
import org.tasks.data.TaskAttachment
import org.tasks.files.FileHelper
import org.tasks.intents.TaskIntents
import org.tasks.preferences.Preferences
import timber.log.Timber
import javax.inject.Inject

/**
 * @author joshuagross
 *
 * Create a new task based on incoming links from the "share" menu
 */
@AndroidEntryPoint
class ShareLinkActivity : AppCompatActivity() {
    @Inject lateinit var taskCreator: TaskCreator
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var firebase: Firebase

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        when (intent.action) {
            Intent.ACTION_PROCESS_TEXT -> lifecycleScope.launch {
                val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
                if (text != null) {
                    val task = taskCreator.createWithValues(text.toString())
                    editTask(task)
                    firebase.addTask("clipboard")
                }
                finish()
            }

            Intent.ACTION_SEND -> lifecycleScope.launch {
                val task = taskCreator.create(intent)
                if (hasAttachments(intent)) {
                    task.putTransitory(TaskAttachment.KEY, copyAttachment(intent))
                    firebase.addTask("share_attachment")
                } else {
                    firebase.addTask("share_text")
                }
                editTask(task)
                finish()
            }

            Intent.ACTION_SEND_MULTIPLE -> lifecycleScope.launch {
                val task = taskCreator.create(intent)
                if (hasAttachments(intent)) {
                    task.putTransitory(TaskAttachment.KEY, copyMultipleAttachments(intent))
                    firebase.addTask("share_multiple_attachments")
                } else {
                    firebase.addTask("share_multiple_text")
                }
                editTask(task)
                finish()
            }

            Intent.ACTION_VIEW -> lifecycleScope.launch {
                editTask(taskCreator.createWithValues(""))
                firebase.addTask("action_view")
                finish()
            }

            else -> {
                Timber.e("Unhandled intent: %s", intent)
                finish()
            }
        }
    }

    private fun editTask(task: Task) {
        val intent = TaskIntents.getEditTaskIntent(this, null, task)
        intent.putExtra(MainActivity.FINISH_AFFINITY, true)
        startActivity(intent)
    }

    private fun copyAttachment(intent: Intent): ArrayList<Uri> =
        intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            ?.let { copyAttachments(listOf(it)) }
            ?: arrayListOf()

    private fun copyMultipleAttachments(intent: Intent): ArrayList<Uri> =
        intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            ?.let { copyAttachments(it) }
            ?: arrayListOf()

    private fun copyAttachments(uris: List<Uri>) =
        uris
            .filter {
                it.scheme == ContentResolver.SCHEME_CONTENT
                        && it.authority != Constants.FILE_PROVIDER_AUTHORITY
            }
            .map { FileHelper.copyToUri(this, preferences.attachmentsDirectory!!, it) }
            .let { ArrayList(it) }

    private fun hasAttachments(intent: Intent) =
        intent.type?.let { type -> ATTACHMENT_TYPES.any { type.startsWith(it) } } ?: false

    companion object {
        private val ATTACHMENT_TYPES = listOf("image/", "application/", "audio/")

        private suspend fun TaskCreator.create(intent: Intent): Task {
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            val hasSubject = subject?.isNotBlank() == true
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            val task = createWithValues(if (hasSubject) subject else text)
            if (hasSubject) {
                task.notes = text
            }
            return task
        }
    }
}