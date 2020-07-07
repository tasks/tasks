package com.todoroo.astrid.activity

import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.service.TaskCreator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.injection.InjectingAppCompatActivity
import org.tasks.intents.TaskIntents
import javax.inject.Inject

@AndroidEntryPoint
class TaskEditActivity : InjectingAppCompatActivity() {
    @Inject lateinit var taskCreator: TaskCreator
    @Inject lateinit var taskDao: TaskDao

    override fun onResume() {
        super.onResume()

        val taskId = intent.getLongExtra(TOKEN_ID, 0)
        if (taskId > 0) {
            lifecycleScope.launch {
                val task = taskDao.fetch(taskId)
                task?.let {
                    startActivity(TaskIntents.getEditTaskIntent(this@TaskEditActivity, it))
                }
                finish()
            }
        } else {
            startActivity(TaskIntents.getEditTaskIntent(this, taskCreator.createWithValues("")))
            finish()
        }
    }

    companion object {
        private const val TOKEN_ID = "id"
    }
}