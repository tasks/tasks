package org.tasks.intents

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.todoroo.astrid.activity.MainActivity
import org.tasks.data.entity.Task
import org.tasks.filters.Filter

object TaskIntents {
    const val FLAGS: Int = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

    fun getNewTaskIntent(
        context: Context,
        filter: Filter?,
        createSource: String
    ): Intent {
        val intent = Intent(context, MainActivity::class.java)
        if (filter != null) {
            intent.putExtra(MainActivity.OPEN_FILTER, filter)
        }
        intent.putExtra(MainActivity.CREATE_TASK, 0L)
        intent.putExtra(MainActivity.CREATE_SOURCE, createSource)
        intent.putExtra(MainActivity.REMOVE_TASK, true)
        return intent
    }

    fun getEditTaskIntent(context: Context, filter: Filter?, task: Task?): Intent {
        val intent = Intent(context, MainActivity::class.java)
        if (filter != null) {
            intent.putExtra(MainActivity.OPEN_FILTER, filter)
        }
        intent.putExtra(MainActivity.OPEN_TASK, task)
        intent.putExtra(MainActivity.REMOVE_TASK, true)
        return intent
    }

    fun getTaskListIntent(context: Context, filter: Filter?): Intent {
        val intent = Intent(context, MainActivity::class.java)
        intent.setFlags(FLAGS)
        if (filter != null) {
            intent.putExtra(MainActivity.OPEN_FILTER, filter)
        }
        return intent
    }

    fun getTaskListByIdIntent(context: Context, filterId: String?): Intent {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.setComponent(ComponentName(context, MainActivity::class.java))
        intent.putExtra(MainActivity.LOAD_FILTER, filterId)
        return intent
    }
}
