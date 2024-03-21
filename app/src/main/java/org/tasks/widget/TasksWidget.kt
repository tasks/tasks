package org.tasks.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.todoroo.andlib.utility.AndroidUtilities.atLeastS
import com.todoroo.astrid.api.Filter
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import org.tasks.R
import org.tasks.dialogs.FilterPicker
import org.tasks.extensions.setBackgroundColor
import org.tasks.extensions.setColorFilter
import org.tasks.extensions.setRipple
import org.tasks.intents.TaskIntents
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.themes.ThemeColor
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TasksWidget : AppWidgetProvider() {
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject @ApplicationContext lateinit var context: Context

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            try {
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                appWidgetManager.updateAppWidget(
                    appWidgetId,
                    createWidget(context, appWidgetId, options)
                )
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            newOptions: Bundle
    ) {
        appWidgetManager.updateAppWidget(
            appWidgetId,
            createWidget(context, appWidgetId, newOptions)
        )
    }

    private fun createWidget(context: Context, id: Int, options: Bundle): RemoteViews {
        val widgetPreferences = WidgetPreferences(context, preferences, id)
        val settings = widgetPreferences.getWidgetHeaderSettings()
        widgetPreferences.setCompact(
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH) < COMPACT_MAX
        )
        val filter = runBlocking {
            defaultFilterProvider.getFilterFromPreference(widgetPreferences.filterId)
        }
        return RemoteViews(context.packageName, R.layout.scrollable_widget).apply {
            if (settings.showHeader) {
                setViewVisibility(R.id.widget_header, View.VISIBLE)
                setupHeader(settings, filter, id)
            } else {
                setViewVisibility(R.id.widget_header, View.GONE)
            }
            setBackgroundColor(
                viewId = R.id.list_view,
                color = settings.backgroundColor,
                opacity = widgetPreferences.rowOpacity,
            )
            setBackgroundColor(
                viewId = R.id.empty_view,
                color = settings.backgroundColor,
                opacity = widgetPreferences.footerOpacity,
            )
            setOnClickPendingIntent(R.id.empty_view, getOpenListIntent(context, filter, id))
            val cacheBuster = Uri.parse("tasks://widget/" + System.currentTimeMillis())
            setRemoteAdapter(
                R.id.list_view,
                Intent(context, TasksWidgetAdapter::class.java)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    .setData(cacheBuster)
            )
            setPendingIntentTemplate(R.id.list_view, getPendingIntentTemplate(context))
        }
    }

    private fun RemoteViews.setupHeader(
        widgetPreferences: WidgetPreferences.WidgetHeaderSettings,
        filter: Filter,
        id: Int,
    ) {
        val color = ThemeColor(context, widgetPreferences.color)
        setBackgroundColor(
            viewId = R.id.widget_header,
            color = color.primaryColor,
            opacity = widgetPreferences.headerOpacity,
        )
        val hPad = context.resources.getDimension(R.dimen.widget_padding).toInt()
        val vPad = widgetPreferences.headerSpacing
        setupButton(
            viewId = R.id.widget_change_list,
            enabled = widgetPreferences.showMenu,
            color = color,
            vPad = vPad,
            hPad = hPad,
            onClick = getChooseListIntent(context, filter, id),
        )
        setupButton(
            viewId = R.id.widget_reconfigure,
            enabled = widgetPreferences.showSettings,
            color = color,
            vPad = vPad,
            hPad = hPad,
            onClick = getWidgetConfigIntent(context, id),
        )
        setupButton(
            viewId = R.id.widget_button,
            enabled = filter.isWritable,
            color = color,
            vPad = vPad,
            hPad = hPad,
            onClick = getNewTaskIntent(context, filter, id),
        )

        setViewPadding(
            R.id.widget_title,
            if (widgetPreferences.showMenu) 0 else hPad, vPad, 0, vPad
        )
        setTextColor(R.id.widget_title, color.colorOnPrimary)
        setOnClickPendingIntent(R.id.widget_title, getOpenListIntent(context, filter, id))
        setTextViewText(
            R.id.widget_title,
            if (widgetPreferences.showTitle) filter.title else null
        )
    }

    private fun RemoteViews.setupButton(
        viewId: Int,
        enabled: Boolean,
        color: ThemeColor,
        vPad: Int,
        hPad: Int,
        onClick: PendingIntent,
    ) {
        if (enabled) {
            setViewVisibility(viewId, View.VISIBLE)
            setColorFilter(viewId, color.colorOnPrimary)
            setViewPadding(viewId, hPad, vPad, hPad, vPad)
            setRipple(viewId, color.isDark)
            setOnClickPendingIntent(viewId, onClick)
        } else {
            setViewVisibility(viewId, View.GONE)
        }
    }

    private fun getPendingIntentTemplate(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, WidgetClickActivity::class.java),
            if (atLeastS())
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else
                PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun getOpenListIntent(context: Context, filter: Filter, widgetId: Int): PendingIntent {
        val intent = TaskIntents.getTaskListIntent(context, filter)
        intent.action = "open_list"
        return PendingIntent.getActivity(
            context,
            widgetId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getNewTaskIntent(context: Context, filter: Filter, widgetId: Int): PendingIntent {
        val intent = TaskIntents.getNewTaskIntent(context, filter, "widget")
        intent.action = "new_task"
        return PendingIntent.getActivity(
            context,
            widgetId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getWidgetConfigIntent(context: Context, widgetId: Int): PendingIntent {
        val intent = Intent(context, WidgetConfigActivity::class.java)
        intent.flags = FLAGS
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        intent.action = "widget_settings"
        return PendingIntent.getActivity(
            context,
            widgetId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getChooseListIntent(context: Context, filter: Filter, widgetId: Int): PendingIntent {
        val intent = Intent(context, WidgetFilterSelectionActivity::class.java)
        intent.flags = FLAGS
        intent.putExtra(FilterPicker.EXTRA_FILTER, filter)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        intent.action = "choose_list"
        return PendingIntent.getActivity(
            context,
            widgetId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        private const val COMPACT_MAX = 275
        private const val FLAGS = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
}