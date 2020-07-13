package com.todoroo.astrid.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.google.common.collect.Lists
import com.google.common.io.Files
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.service.TaskCreator
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.Strings.isNullOrEmpty
import org.tasks.data.TaskAttachment
import org.tasks.files.FileHelper
import org.tasks.injection.InjectingAppCompatActivity
import org.tasks.intents.TaskIntents
import org.tasks.preferences.Preferences
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 * @author joshuagross
 *
 * Create a new task based on incoming links from the "share" menu
 */
@AndroidEntryPoint
class ShareLinkActivity : InjectingAppCompatActivity() {
    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var taskCreator: TaskCreator
    @Inject lateinit var preferences: Preferences

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        val action = intent.action
        if (Intent.ACTION_PROCESS_TEXT == action) {
            val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
            if (text != null) {
                val task = taskCreator.createWithValues(text.toString())
                editTask(task)
            }
        } else if (Intent.ACTION_SEND == action) {
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            val task = taskCreator.createWithValues(subject)
            task.notes = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (hasAttachments(intent)) {
                task.putTransitory(TaskAttachment.KEY, copyAttachment(intent))
            }
            editTask(task)
        } else if (Intent.ACTION_SEND_MULTIPLE == action) {
            val task = taskCreator.createWithValues(intent.getStringExtra(Intent.EXTRA_SUBJECT))
            task.notes = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (hasAttachments(intent)) {
                task.putTransitory(TaskAttachment.KEY, copyMultipleAttachments(intent))
            }
            editTask(task)
        } else {
            Timber.e("Unhandled intent: %s", intent)
        }
        finish()
    }

    private fun editTask(task: Task) {
        val intent = TaskIntents.getEditTaskIntent(this, null, task)
        intent.putExtra(MainActivity.FINISH_AFFINITY, true)
        startActivity(intent)
    }

    private fun copyAttachment(intent: Intent): ArrayList<Uri> {
        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        var filename = FileHelper.getFilename(context, uri)
        if (isNullOrEmpty(filename)) {
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            filename = if (isNullOrEmpty(subject)) uri.lastPathSegment else subject.substring(0, Math.min(subject.length, FileHelper.MAX_FILENAME_LENGTH))
        }
        val basename = Files.getNameWithoutExtension(filename!!)
        return Lists.newArrayList(FileHelper.copyToUri(context, preferences.attachmentsDirectory, uri, basename))
    }

    private fun copyMultipleAttachments(intent: Intent): ArrayList<Uri> {
        val result = ArrayList<Uri>()
        val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
        if (uris != null) {
            for (uri in uris) {
                result.add(FileHelper.copyToUri(context, preferences.attachmentsDirectory, uri))
            }
        }
        return result
    }

    private fun hasAttachments(intent: Intent): Boolean {
        val type = intent.type
        return type != null && (type.startsWith("image/") || type.startsWith("application/"))
    }
}