package com.todoroo.astrid.widget;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.IBinder;
import android.view.View;
import android.widget.RemoteViews;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskEditActivity;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.CoreFilterExposer;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.TaskService;

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

        super.onUpdate(context, appWidgetManager, appWidgetIds);

        // Start in service to prevent Application Not Responding timeout
        context.startService(new Intent(context, UpdateService.class));
    }

    public static class UpdateService extends Service {

        @Autowired
        Database database;

        @Autowired
        TaskService taskService;

        @Override
        public void onStart(Intent intent, int startId) {
            RemoteViews updateViews = buildUpdate(this);

            ComponentName thisWidget = new ComponentName(this,
                    TasksWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(thisWidget, updateViews);
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @SuppressWarnings("nls")
        public RemoteViews buildUpdate(Context context) {
            RemoteViews views = null;

            views = new RemoteViews(context.getPackageName(),
                    R.layout.widget_initialized);

            int[] textIDs = TEXT_IDS;
            int[] separatorIDs = SEPARATOR_IDS;
            int numberOfTasks = 5;

            Intent listIntent = new Intent(context, TaskListActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                    listIntent, 0);
            views.setOnClickPendingIntent(R.id.taskbody, pendingIntent);

            Filter inboxFilter = CoreFilterExposer.buildInboxFilter(getResources());
            inboxFilter.sqlQuery += TaskService.defaultTaskOrder() + " LIMIT " + numberOfTasks;
            DependencyInjectionService.getInstance().inject(this);
            TodorooCursor<Task> cursor = null;
            try {
                database.openForWriting();
                cursor = taskService.fetchFiltered(inboxFilter, Task.TITLE, Task.DUE_DATE);
                Task task = new Task();
                for (int i = 0; i < cursor.getCount(); i++) {
                    cursor.moveToPosition(i);
                    task.readFromCursor(cursor);

                    String textContent = "";
                    int textColor = Color.WHITE;

                    textContent = task.getValue(Task.TITLE);
                    if(task.hasDueDate() && task.getValue(Task.DUE_DATE) < DateUtilities.now())
                        textColor = context.getResources().getColor(R.color.task_list_overdue);

                    if(i > 0)
                    views.setViewVisibility(separatorIDs[i-1], View.VISIBLE);
                    views.setTextViewText(textIDs[i], textContent);
                    views.setTextColor(textIDs[i], textColor);
                }

                for(int i = cursor.getCount() - 1; i < separatorIDs.length; i++)
                    views.setViewVisibility(separatorIDs[i], View.INVISIBLE);
            } catch (Exception e) {
                // can happen if database is not ready
            } finally {
                if(cursor != null)
                    cursor.close();
            }

            Intent editIntent = new Intent(context, TaskEditActivity.class);
            pendingIntent = PendingIntent.getActivity(context, 0,
                    editIntent, 0);
            views.setOnClickPendingIntent(R.id.widget_button, pendingIntent);

            return views;
        }

    }
}
