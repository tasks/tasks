package org.tasks.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.widget.RemoteViewsService
import androidx.core.content.IntentCompat
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.subtasks.SubtasksHelper
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.data.TaskDao
import org.tasks.markdown.MarkdownProvider
import org.tasks.preferences.Preferences
import org.tasks.tasklist.HeaderFormatter
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class TasksWidgetAdapter : RemoteViewsService() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var subtasksHelper: SubtasksHelper
    @Inject lateinit var locale: Locale
    @Inject lateinit var chipProvider: WidgetChipProvider
    @Inject lateinit var markdownProvider: MarkdownProvider
    @Inject lateinit var headerFormatter: HeaderFormatter

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory? {
        val widgetId = intent.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID) ?: return null
        val filter = IntentCompat.getParcelableExtra(intent, EXTRA_FILTER, Filter::class.java) ?: return null
        return TasksWidgetViewFactory(
            subtasksHelper,
            preferences,
            filter,
            applicationContext,
            widgetId,
            taskDao,
            locale,
            chipProvider,
            markdownProvider.markdown(false),
            headerFormatter,
        )
    }

    companion object {
        const val EXTRA_FILTER = "extra_filter"
    }
}
