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
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import com.todoroo.astrid.api.Filter
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import org.tasks.R
import org.tasks.activities.FilterSelectionActivity
import org.tasks.intents.TaskIntents
import org.tasks.locale.Locale
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.themes.ThemeColor
import org.tasks.widget.ScrollableViewsFactory.Companion.isDark
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TasksWidget : AppWidgetProvider() {
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var locale: Locale
    @Inject @ApplicationContext lateinit var context: Context

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            try {
                val options = appWidgetManager.getAppWidgetOptions(id)
                appWidgetManager.updateAppWidget(id, createScrollableWidget(context, id, options))
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            newOptions: Bundle?
    ) {
        newOptions?.let {
            appWidgetManager
                    .updateAppWidget(appWidgetId, createScrollableWidget(context, appWidgetId, it))
        }
    }

    private fun createScrollableWidget(context: Context, id: Int, options: Bundle): RemoteViews {
        val widgetPreferences = WidgetPreferences(context, preferences, id)
        widgetPreferences.compact =
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH) < COMPACT_MAX
        val filterId = widgetPreferences.filterId
        val color = ThemeColor(context, widgetPreferences.color)
        val remoteViews = RemoteViews(context.packageName, R.layout.scrollable_widget)
        remoteViews.setInt(R.id.widget, "setLayoutDirection", locale.directionality)
        if (widgetPreferences.showHeader()) {
            remoteViews.setViewVisibility(R.id.widget_header, View.VISIBLE)
            remoteViews.setViewVisibility(
                    R.id.widget_change_list,
                    if (widgetPreferences.showMenu()) View.VISIBLE else View.GONE
            )
            remoteViews.setViewVisibility(
                    R.id.widget_reconfigure,
                    if (widgetPreferences.showSettings()) View.VISIBLE else View.GONE
            )
            remoteViews.removeAllViews(R.id.title_container)
            remoteViews.addView(
                    R.id.title_container,
                    RemoteViews(context.packageName, widgetPreferences.headerLayout)
            )
            val widgetPadding = context.resources.getDimension(R.dimen.widget_padding).toInt()
            val widgetTitlePadding = if (widgetPreferences.showMenu()) 0 else widgetPadding
            val vPad = widgetPreferences.headerSpacing
            remoteViews.setViewPadding(R.id.widget_title, widgetTitlePadding, 0, 0, 0)
            remoteViews.setInt(R.id.widget_title, "setTextColor", color.colorOnPrimary)
            buttons.forEach {
                remoteViews.setInt(it, "setColorFilter", color.colorOnPrimary)
                remoteViews.setViewPadding(it, widgetPadding, vPad, widgetPadding, vPad)
            }
        } else {
            remoteViews.setViewVisibility(R.id.widget_header, View.GONE)
        }
        remoteViews.setInt(
                R.id.widget_header,
                "setBackgroundColor",
                ColorUtils.setAlphaComponent(color.primaryColor, widgetPreferences.headerOpacity))
        val bgColor = getBackgroundColor(widgetPreferences.themeIndex)
        remoteViews.setInt(
                R.id.list_view,
                "setBackgroundColor",
                ColorUtils.setAlphaComponent(bgColor, widgetPreferences.rowOpacity))
        remoteViews.setInt(
                R.id.empty_view,
                "setBackgroundColor",
                ColorUtils.setAlphaComponent(bgColor, widgetPreferences.footerOpacity))
        val filter = runBlocking { defaultFilterProvider.getFilterFromPreference(filterId) }
        remoteViews.setTextViewText(R.id.widget_title, if (widgetPreferences.showTitle()) {
            filter.listingTitle
        } else {
            null
        })
        val cacheBuster = Uri.parse("tasks://widget/" + System.currentTimeMillis())
        remoteViews.setRemoteAdapter(
                R.id.list_view,
                Intent(context, ScrollableWidgetUpdateService::class.java)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                        .setData(cacheBuster))
        setRipple(
                remoteViews, color, R.id.widget_button, R.id.widget_change_list, R.id.widget_reconfigure)
        remoteViews.setOnClickPendingIntent(R.id.widget_title, getOpenListIntent(context, filter, id))
        remoteViews.setOnClickPendingIntent(R.id.widget_button, getNewTaskIntent(context, filter, id))
        remoteViews.setOnClickPendingIntent(R.id.widget_change_list, getChooseListIntent(context, filter, id))
        remoteViews.setOnClickPendingIntent(
                R.id.widget_reconfigure, getWidgetConfigIntent(context, id))
        if (widgetPreferences.openOnFooterClick()) {
            remoteViews.setOnClickPendingIntent(R.id.empty_view, getOpenListIntent(context, filter, id))
        } else {
            remoteViews.setOnClickPendingIntent(R.id.empty_view, null)
        }
        remoteViews.setPendingIntentTemplate(R.id.list_view, getPendingIntentTemplate(context))
        return remoteViews
    }

    private fun setRipple(rv: RemoteViews, color: ThemeColor, vararg views: Int) {
        val drawableRes = if (color.isDark) R.drawable.widget_ripple_circle_light else R.drawable.widget_ripple_circle_dark
        for (view in views) {
            rv.setInt(view, "setBackgroundResource", drawableRes)
        }
    }

    @ColorInt
    private fun getBackgroundColor(themeIndex: Int): Int {
        val background: Int = when (themeIndex) {
            1 -> android.R.color.black
            2 -> R.color.md_background_dark
            3 -> if (context.isDark) R.color.md_background_dark else android.R.color.white
            else -> android.R.color.white
        }
        return context.getColor(background)
    }

    private fun getPendingIntentTemplate(context: Context): PendingIntent =
            PendingIntent.getActivity(
                    context,
                0,
                Intent(context, WidgetClickActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
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
        val intent = TaskIntents.getNewTaskIntent(context, filter)
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
        intent.flags = flags
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
        val intent = Intent(context, FilterSelectionActivity::class.java)
        intent.flags = flags
        intent.putExtra(FilterSelectionActivity.EXTRA_FILTER, filter)
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
        private const val flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        private val buttons = intArrayOf(
                R.id.widget_change_list, R.id.widget_button, R.id.widget_reconfigure
        )
    }
}