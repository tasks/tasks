package com.timsu.astrid.appwidget;

import java.util.ArrayList;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.timsu.astrid.R;
import com.timsu.astrid.data.task.TaskController;
import com.timsu.astrid.data.task.TaskModelForWidget;
import com.todoroo.astrid.activity.TaskEditActivity;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.service.AstridDependencyInjector;

public class AstridAppWidgetProvider extends AppWidgetProvider {

    static {
        AstridDependencyInjector.initialize();
    }

    private final static String TAG           = "AstridAppWidgetProvider";
    public final static int[]   TEXT_IDS      = { R.id.task_1, R.id.task_2,
        R.id.task_3, R.id.task_4, R.id.task_5 };
    public final static int[]   SEPARATOR_IDS = { R.id.separator_1,
        R.id.separator_2, R.id.separator_3, R.id.separator_4 };

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {

        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Log.e(TAG, "onUpdate()");

        // Start in service to prevent Application Not Responding timeout
        context.startService(new Intent(context, UpdateService.class));
    }

    public static class UpdateService extends Service {

        @Override
        public void onStart(Intent intent, int startId) {

            Log.e("UpdateService", "onStart()");

            RemoteViews updateViews = buildUpdate(this);

            ComponentName thisWidget = new ComponentName(this,
                    AstridAppWidgetProvider.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(thisWidget, updateViews);
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        public static RemoteViews buildUpdate(Context context) {
            RemoteViews views = null;

            views = new RemoteViews(context.getPackageName(),
                    R.layout.widget_initialized);

            int[] textIDs = TEXT_IDS;
            int[] separatorIDs = SEPARATOR_IDS;
            int numberOfTasks = 5;

            TaskController taskController = new TaskController(context);
            taskController.open();
            ArrayList<TaskModelForWidget> taskList = taskController
                    .getTasksForWidget(Integer.toString(numberOfTasks));
            taskController.close();

            Intent listIntent = new Intent(context, TaskListActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                    listIntent, 0);
            views.setOnClickPendingIntent(R.id.taskbody, pendingIntent);

            for (int i = 0; i < textIDs.length; i++) {
                TaskModelForWidget taskModel = (i < taskList.size()) ?
                    taskList.get(i) : null;
                String textContent = "";
                int textColor = Color.WHITE;

                if (taskModel != null) {
                    textContent = taskModel.getName();

                    // tweak color if overdue
                    if((taskModel.getPreferredDueDate() != null && taskModel.getPreferredDueDate().getTime() < System.currentTimeMillis()) ||
                            (taskModel.getDefiniteDueDate() != null && taskModel.getDefiniteDueDate().getTime() < System.currentTimeMillis()))
                        textColor = context.getResources().getColor(R.color.task_list_overdue);
                }

                if (i < separatorIDs.length) {
                    if (i < taskList.size() - 1 && taskList.get(i + 1) != null) {
                        views.setViewVisibility(separatorIDs[i], View.VISIBLE);
                    } else {
                        views.setViewVisibility(separatorIDs[i],
                                        View.INVISIBLE);
                    }
                }

                views.setTextViewText(textIDs[i], textContent);
                views.setTextColor(textIDs[i], textColor);
            }

            Intent editIntent = new Intent(context, TaskEditActivity.class);
            pendingIntent = PendingIntent.getActivity(context, 0,
                    editIntent, 0);
            views.setOnClickPendingIntent(R.id.widget_button, pendingIntent);

            return views;
        }

    }
}
