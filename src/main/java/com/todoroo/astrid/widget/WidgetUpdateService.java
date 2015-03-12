package com.todoroo.astrid.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.IBinder;
import android.view.View;
import android.widget.RemoteViews;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.subtasks.SubtasksHelper;
import com.todoroo.astrid.utility.Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.injection.InjectingService;
import org.tasks.preferences.Preferences;
import org.tasks.widget.DueDateFormatter;
import org.tasks.widget.WidgetHelper;

import javax.inject.Inject;

public class WidgetUpdateService extends InjectingService {

    private static final Logger log = LoggerFactory.getLogger(WidgetUpdateService.class);

    private static final int NUM_VISIBLE_TASKS = 25;

    public static final String EXTRA_WIDGET_ID = "widget_id"; //$NON-NLS-1$

    @Inject Database database;
    @Inject TaskService taskService;
    @Inject WidgetHelper widgetHelper;
    @Inject Preferences preferences;
    @Inject SubtasksHelper subtasksHelper;
    @Inject DueDateFormatter dueDateFormatter;

    @Override
    public void onStart(final Intent intent, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                startServiceInBackgroundThread(intent);
            }
        }).start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startServiceInBackgroundThread(Intent intent) {
        ComponentName thisWidget = new ComponentName(this,
                TasksWidget.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(this);

        int extrasId = AppWidgetManager.INVALID_APPWIDGET_ID;
        if(intent != null) {
            extrasId = intent.getIntExtra(EXTRA_WIDGET_ID, extrasId);
        }
        if(extrasId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            int[] ids;
            try {
                ids = manager.getAppWidgetIds(thisWidget);
                for(int id : ids) {
                    RemoteViews updateViews = buildUpdate(this, id);
                    manager.updateAppWidget(id, updateViews);
                }
            } catch (RuntimeException e) {
                // "System server dead" was sometimes thrown here by the OS. Abort if that happens
                log.error(e.getMessage(), e);
            }
        } else {
            RemoteViews updateViews = buildUpdate(this, extrasId);
            manager.updateAppWidget(extrasId, updateViews);
        }

        stopSelf();
    }

    private RemoteViews buildUpdate(Context context, int widgetId) {
        boolean darkTheme = preferences.getBoolean(WidgetConfigActivity.PREF_DARK_THEME + widgetId, false);
        /**
         * The reason we use a bunch of different but almost identical layouts is that there is a bug with
         * Android 2.1 (level 7) that doesn't allow setting backgrounds on remote views. I know it's lame,
         * but I didn't see a better solution. Alternatively, we could disallow theming widgets on
         * Android 2.1.
         */
        RemoteViews views = new RemoteViews(context.getPackageName(), darkTheme
                ? R.layout.widget_initialized_dark
                : R.layout.widget_initialized);

        int numberOfTasks = NUM_VISIBLE_TASKS;

        TodorooCursor<Task> cursor = null;
        Filter filter = null;
        try {
            filter = widgetHelper.getFilter(context, widgetId);
            if (filter.isTagFilter()) {
                ((FilterWithCustomIntent) filter).customTaskList = new ComponentName(context, TagViewFragment.class); // In case legacy widget was created with subtasks fragment
            }
            views.setTextViewText(R.id.widget_title, filter.title);
            views.removeAllViews(R.id.taskbody);

            int flags = preferences.getSortFlags();
            int sort = preferences.getSortMode();
            String query = SortHelper.adjustQueryForFlagsAndSort(
                    filter.getSqlQuery(), flags, sort).replaceAll("LIMIT \\d+", "") + " LIMIT " + numberOfTasks;

            String tagName = preferences.getStringValue(WidgetConfigActivity.PREF_TITLE + widgetId);
            boolean showDueDates = preferences.getBoolean(WidgetConfigActivity.PREF_DUE_DATE + widgetId, false);
            query = subtasksHelper.applySubtasksToWidgetFilter(filter, query, tagName, numberOfTasks);

            database.openForReading();
            cursor = taskService.fetchFiltered(query, null, Task.ID, Task.TITLE, Task.DUE_DATE, Task.COMPLETION_DATE);
            int i;
            Resources r = context.getResources();
            for (i = 0; i < cursor.getCount() && i < numberOfTasks; i++) {
                cursor.moveToPosition(i);
                Task task = new Task(cursor);
                String textContent = task.getTitle();
                int textColor = r.getColor(darkTheme
                        ? R.color.widget_text_color_dark
                        : R.color.widget_text_color_light);
                if(task.isCompleted()) {
                    textColor = r.getColor(R.color.task_list_done);
                }

                RemoteViews row = new RemoteViews(Constants.PACKAGE, R.layout.widget_row);

                if (showDueDates) {
                    dueDateFormatter.formatDueDate(row, task, textColor);
                } else if(task.hasDueDate() && task.isOverdue()) {
                    textColor = r.getColor(R.color.task_list_overdue);
                }

                row.setTextViewText(R.id.text, textContent);
                row.setTextColor(R.id.text, textColor);

                views.addView(R.id.taskbody, row);

                RemoteViews separator = new RemoteViews(Constants.PACKAGE, R.layout.widget_separator);
                boolean isLastRow = (i == cursor.getCount() - 1) || (i == numberOfTasks - 1);
                if (!isLastRow) {
                    views.addView(R.id.taskbody, separator);
                }
            }
            for (; i < numberOfTasks; i++) {
                RemoteViews row = new RemoteViews(Constants.PACKAGE, R.layout.widget_row);
                row.setViewVisibility(R.id.text, View.INVISIBLE);
                views.addView(R.id.taskbody, row);
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }

        PendingIntent pListIntent = widgetHelper.getListIntent(context, filter, widgetId);
        if (pListIntent != null) {
            views.setOnClickPendingIntent(R.id.taskbody, pListIntent);
        }

        PendingIntent pEditIntent = widgetHelper.getNewTaskIntent(context, filter, widgetId);
        if (pEditIntent != null) {
            views.setOnClickPendingIntent(R.id.widget_button, pEditIntent);
            views.setOnClickPendingIntent(R.id.widget_title, pEditIntent);
        }

        return views;
    }
}
