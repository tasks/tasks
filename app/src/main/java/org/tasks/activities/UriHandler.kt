package org.tasks.activities

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.api.TasksContract
import org.tasks.data.dao.TaskDao
import org.tasks.intents.TaskIntents
import org.tasks.provider.TasksContentProvider
import org.tasks.provider.TasksContentProvider.Companion.URI_OPEN_TASK
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class UriHandler : AppCompatActivity() {

    @Inject lateinit var taskDao: TaskDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = intent.data ?: Uri.EMPTY
        val apiTask = TasksContract.Tasks.idIn(data.toString())
        when {
            apiTask != null -> open(apiTask)
            data.toString() == TasksContract.Tasks.CONTENT_URI -> newTask()
            TasksContentProvider.URI_MATCHER.match(data) == URI_OPEN_TASK -> {
                val id = data.lastPathSegment?.toLongOrNull() ?: 0
                if (id > 0) open(id) else newTask()
            }
            else -> {
                if (intent.type == "vnd.android.cursor.item/task") {
                    // pure calendar widget '+'
                    newTask()
                } else {
                    Timber.w("Invalid uri: ${intent.data}")
                    finish()
                }
            }
        }
    }

    private fun open(id: Long) {
        lifecycleScope.launch {
            taskDao.fetch(id)?.let {
                startActivity(TaskIntents.getEditTaskIntent(this@UriHandler, null, it))
            }
            finish()
        }
    }

    private fun newTask() {
        val intent = TaskIntents.getNewTaskIntent(this@UriHandler, null, "content_provider")
        intent.flags = TaskIntents.FLAGS
        startActivity(intent)
        finish()
    }
}