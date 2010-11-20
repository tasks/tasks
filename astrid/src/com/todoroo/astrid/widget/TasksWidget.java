package com.todoroo.astrid.widget;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.TaskEditActivity;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.CoreFilterExposer;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.utility.AstridPreferences;

public class TasksWidget extends AppWidgetProvider {

    static {
        AstridDependencyInjector.initialize();
    }

    public final static int[]   TEXT_IDS      = { R.id.task_1, R.id.task_2,
        R.id.task_3, R.id.task_4, R.id.task_5 };
    public final static int[]   SEPARATOR_IDS = { R.id.separator_1,
        R.id.separator_2, R.id.separator_3, R.id.separator_4 };

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {

        try {
            ContextManager.setContext(context);
            super.onUpdate(context, appWidgetManager, appWidgetIds);

            // Start in service to prevent Application Not Responding timeout
            updateWidgets(context);
        } catch (Exception e) {
            Log.e("astrid-update-widget", "widget update error", e); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /**
     * Update all widgets
     * @param id
     */
    public static void updateWidgets(Context context) {
        context.startService(new Intent(context,
                TasksWidget.UpdateService.class));
    }

    /**
     * Update widget with the given id
     * @param id
     */
    public static void updateWidget(Context context, int id) {
        Intent intent = new Intent(ContextManager.getContext(),
                TasksWidget.UpdateService.class);
        intent.putExtra(UpdateService.EXTRA_WIDGET_ID, id);
        context.startService(intent);
    }

    public static class ConfigActivity extends WidgetConfigActivity {
        @Override
        public void updateWidget() {
            TasksWidget.updateWidget(this, mAppWidgetId);
        }
    }

    public static class UpdateService extends Service {

        public static String EXTRA_WIDGET_ID = "widget_id"; //$NON-NLS-1$

        @Autowired
        Database database;

        @Autowired
        TaskService taskService;

        @Override
        public void onStart(final Intent intent, int startId) {
            ContextManager.setContext(this);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    startServiceInBackgroundThread(intent);
                }
            }).start();
        }

        public void startServiceInBackgroundThread(Intent intent) {
            ComponentName thisWidget = new ComponentName(this,
                    TasksWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);

            int extrasId = AppWidgetManager.INVALID_APPWIDGET_ID;
            if(intent != null)
                extrasId = intent.getIntExtra(EXTRA_WIDGET_ID, extrasId);
            if(extrasId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                for(int id : manager.getAppWidgetIds(thisWidget)) {
                    RemoteViews updateViews = buildUpdate(this, id);
                    manager.updateAppWidget(id, updateViews);
                }
            } else {
                int id = extrasId;
                RemoteViews updateViews = buildUpdate(this, id);
                manager.updateAppWidget(id, updateViews);
            }

            stopSelf();
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @SuppressWarnings("nls")
        public RemoteViews buildUpdate(Context context, int widgetId) {
            DependencyInjectionService.getInstance().inject(this);

            RemoteViews views = null;

            views = new RemoteViews(context.getPackageName(),
                    R.layout.widget_initialized);

            int[] textIDs = TEXT_IDS;
            int[] separatorIDs = SEPARATOR_IDS;
            int numberOfTasks = 5;

            for(int i = 0; i < textIDs.length; i++)
                views.setTextViewText(textIDs[i], "");

            TodorooCursor<Task> cursor = null;
            Filter filter = null;
            try {
                filter = getFilter(widgetId);
                views.setTextViewText(R.id.widget_title, filter.title);

                SharedPreferences publicPrefs = AstridPreferences.getPublicPrefs(this);
                int flags = publicPrefs.getInt(SortHelper.PREF_SORT_FLAGS, 0);
                int sort = publicPrefs.getInt(SortHelper.PREF_SORT_SORT, 0);
                String query = SortHelper.adjustQueryForFlagsAndSort(
                        filter.sqlQuery, flags, sort) + " LIMIT " + numberOfTasks;

                database.openForReading();
                cursor = taskService.fetchFiltered(query, null, Task.ID, Task.TITLE, Task.DUE_DATE, Task.COMPLETION_DATE);
                Task task = new Task();
                for (int i = 0; i < cursor.getCount() && i < numberOfTasks; i++) {
                    cursor.moveToPosition(i);
                    task.readFromCursor(cursor);

                    String textContent = "";
                    int textColor = Color.WHITE;

                    textContent = task.getValue(Task.TITLE);

                    if(task.isCompleted())
                        textColor = context.getResources().getColor(R.color.task_list_done);
                    else if(task.hasDueDate() && task.getValue(Task.DUE_DATE) < DateUtilities.now())
                        textColor = context.getResources().getColor(R.color.task_list_overdue);

                    if(i > 0)
                        views.setViewVisibility(separatorIDs[i-1], View.VISIBLE);
                    views.setTextViewText(textIDs[i], textContent);
                    views.setTextColor(textIDs[i], textColor);
                }

                for(int i = cursor.getCount() - 1; i < separatorIDs.length; i++) {
                    if(i >= 0)
                        views.setViewVisibility(separatorIDs[i], View.INVISIBLE);
                }
            } catch (Exception e) {
                // can happen if database is not ready
                Log.e("WIDGET-UPDATE", "Error updating widget", e);
            } finally {
                if(cursor != null)
                    cursor.close();
            }

            Intent listIntent = new Intent(context, TaskListActivity.class);
            listIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            if(filter != null) {
                listIntent.putExtra(TaskListActivity.TOKEN_FILTER, filter);
                listIntent.setType(filter.sqlQuery);
            }
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                    listIntent, 0);
            views.setOnClickPendingIntent(R.id.taskbody, pendingIntent);

            Intent editIntent = new Intent(context, TaskEditActivity.class);
            editIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            if(filter != null && filter.valuesForNewTasks != null) {
                String values = AndroidUtilities.contentValuesToSerializedString(filter.valuesForNewTasks);
                editIntent.putExtra(TaskEditActivity.TOKEN_VALUES, values);
                editIntent.setType(values);
            }
            pendingIntent = PendingIntent.getActivity(context, 0,
                    editIntent, 0);
            views.setOnClickPendingIntent(R.id.widget_button, pendingIntent);

            return views;
        }

        private Filter getFilter(int widgetId) {
            // base our filter off the inbox filter, replace stuff if we have it
            Filter filter = CoreFilterExposer.buildInboxFilter(getResources());
            String sql = Preferences.getStringValue(WidgetConfigActivity.PREF_SQL + widgetId);
            if(sql != null)
                filter.sqlQuery = sql;
            String title = Preferences.getStringValue(WidgetConfigActivity.PREF_TITLE + widgetId);
            if(title != null)
                filter.title = title;
            String contentValues = Preferences.getStringValue(WidgetConfigActivity.PREF_VALUES + widgetId);
            if(contentValues != null)
                filter.valuesForNewTasks = AndroidUtilities.contentValuesFromSerializedString(contentValues);

            return filter;
        }

    }
}
