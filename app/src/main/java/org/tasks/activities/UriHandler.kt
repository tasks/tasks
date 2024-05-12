package org.tasks.activities

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
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

        when (TasksContentProvider.URI_MATCHER.match(intent.data ?: Uri.EMPTY)) {
            URI_OPEN_TASK -> {
                val id = intent.data?.lastPathSegment?.toLongOrNull() ?: 0
                if (id > 0) {
                    lifecycleScope.launch {
                        val task = taskDao.fetch(id)
                        task?.let {
                            startActivity(TaskIntents.getEditTaskIntent(this@UriHandler, null, it))
                        }
                        finish()
                    }
                } else {
                    newTask()
                }
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

    private fun newTask() {
        val intent = TaskIntents.getNewTaskIntent(this@UriHandler, null, "content_provider")
        intent.flags = TaskIntents.FLAGS
        startActivity(intent)
        finish()
    }
}