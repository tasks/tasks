package org.tasks.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViewsService
import com.todoroo.astrid.subtasks.SubtasksHelper
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import org.tasks.data.dao.TaskDao
import org.tasks.markdown.MarkdownProvider
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.tasklist.HeaderFormatter
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class TasksWidgetAdapter : RemoteViewsService() {
    @ApplicationContext @Inject lateinit var context: Context
    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var subtasksHelper: SubtasksHelper
    @Inject lateinit var locale: Locale
    @Inject lateinit var chipProvider: WidgetChipProvider
    @Inject lateinit var markdownProvider: MarkdownProvider
    @Inject lateinit var headerFormatter: HeaderFormatter

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory? {
        val widgetId = intent.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID) ?: return null
        val widgetPreferences = WidgetPreferences(context, preferences, widgetId)
        val filter = runBlocking {
            defaultFilterProvider.getFilterFromPreference(widgetPreferences.filterId)
        }
        return TasksWidgetViewFactory(
            subtasksHelper,
            widgetPreferences,
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
}
