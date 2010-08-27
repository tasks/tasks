package com.todoroo.astrid.widget;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.IBinder;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.ShortcutActivity;
import com.todoroo.astrid.activity.SortSelectionActivity;
import com.todoroo.astrid.activity.TaskEditActivity;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.CoreFilterExposer;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.utility.Preferences;

/**
 * Power Pack widget.  Supports 4x4 size.  Configured via
 * ConfigurePowerWidgetActivity when widget is added to homescreen.
 *
 * @author jwong (jwong@dayspring-tech.com)
 *
 */
@SuppressWarnings("nls")
public class PowerWidget extends AppWidgetProvider {
    static final String LOG_TAG = "PowerWidget";

    static {
        AstridDependencyInjector.initialize();
    }


    static final long ENCOURAGEMENT_CYCLE_TIME = 1000 * 60 * 60 * 4; // 4 hours

    static final String ACTION_MARK_COMPLETE = "com.timsu.astrid.widget.ACTION_MARK_COMPLETE";
    static final String ACTION_SCROLL_UP = "com.timsu.astrid.widget.ACTION_SCROLL_UP";
    static final String ACTION_SCROLL_DOWN = "com.timsu.astrid.widget.ACTION_SCROLL_DOWN";

    // Prefix for Shared Preferences
    static final String PREF_COLOR = "powerwidget-color-";
    static final String PREF_ENABLE_CALENDAR = "powerwidget-enableCalendar-";
    static final String PREF_ENCOURAGEMENTS = "powerwidget-enableEncouragements-";
    static final String PREF_TITLE = "powerwidget-title-";
    static final String PREF_SQL = "powerwidget-sql-";
    static final String PREF_VALUES = "powerwidget-values-";
    static final String PREF_ENCOURAGEMENT_LAST_ROTATION_TIME = "powerwidget-encouragementRotationTime-";
    static final String PREF_ENCOURAGEMENT_CURRENT = "powerwidget-encouragementCurrent-";

    public final static String APP_WIDGET_IDS = "com.timsu.astrid.APP_WIDGET_IDS";
    public final static String COMPLETED_TASK_ID = "com.timsu.astrid.COMPLETED_TASK_ID";
    public final static String EXTRA_SCROLL_OFFSET = "com.timsu.astrid.EXTRA_SCROLL_OFFSET";

    public final static int[] TASK_TITLE = { R.id.task_title1, R.id.task_title2,
        R.id.task_title3, R.id.task_title4, R.id.task_title5, R.id.task_title6,
        R.id.task_title7, R.id.task_title8, R.id.task_title9, R.id.task_title10 };

    public final static int[] TASK_DUE = { R.id.task_due1, R.id.task_due2,
        R.id.task_due3, R.id.task_due4, R.id.task_due5, R.id.task_due6,
        R.id.task_due7, R.id.task_due8, R.id.task_due9, R.id.task_due10 };

    public final static int[] TASK_IMPORTANCE = { R.id.importance1, R.id.importance2,
        R.id.importance3, R.id.importance4, R.id.importance5, R.id.importance6,
        R.id.importance7, R.id.importance8, R.id.importance9, R.id.importance10 };

    public final static int[] TASK_CHECKBOX = { R.id.checkbox1, R.id.checkbox2,
        R.id.checkbox3, R.id.checkbox4, R.id.checkbox5, R.id.checkbox6,
        R.id.checkbox7, R.id.checkbox8, R.id.checkbox9, R.id.checkbox10 };


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {

        try {
            super.onUpdate(context, appWidgetManager, appWidgetIds);

            // Start in service to prevent Application Not Responding timeout
            Intent updateIntent = new Intent(context, UpdateService.class);
            updateIntent.putExtra(APP_WIDGET_IDS, appWidgetIds);
            context.startService(updateIntent);
        } catch (SecurityException e) {
            // :(
        }
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_MARK_COMPLETE.equals(intent.getAction())){
            long taskId = intent.getLongExtra(COMPLETED_TASK_ID, -1);

            Intent updateIntent = new Intent(context, UpdateService.class);
            updateIntent.putExtra(COMPLETED_TASK_ID, taskId);
            context.startService(updateIntent);
        } else if (ACTION_SCROLL_UP.equals(intent.getAction()) || ACTION_SCROLL_DOWN.equals(intent.getAction())){
            int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (id != AppWidgetManager.INVALID_APPWIDGET_ID){
                int scrollOffset = intent.getIntExtra(EXTRA_SCROLL_OFFSET, 0);
                Intent updateIntent = new Intent(context, UpdateService.class);
                updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
                updateIntent.putExtra(EXTRA_SCROLL_OFFSET, scrollOffset);
                context.startService(updateIntent);
            }
        } else {
            super.onReceive(context, intent);
        }
    }



    /**
     * Update all widgets
     * @param id
     */
    public static void updateWidgets(Context context) {
        context.startService(new Intent(ContextManager.getContext(),
                UpdateService.class));
    }

    /**
     * Update widget with the given id
     * @param id
     */
    public static void updateAppWidget(Context context, int appWidgetId){
        Intent updateIntent = new Intent(context, UpdateService.class);
        updateIntent.putExtra(APP_WIDGET_IDS, new int[]{ appWidgetId });
        context.startService(updateIntent);
    }

    public static class UpdateService extends Service {

        @Autowired
        Database database;

        @Autowired
        TaskService taskService;

        @Override
        public void onStart(Intent intent, int startId) {
            ContextManager.setContext(this);

            if (intent != null){
                long taskId = intent.getLongExtra(COMPLETED_TASK_ID, -1);
                if (taskId > 0){
                    Task task = taskService.fetchById(taskId, Task.PROPERTIES);
                    if (task != null){
                        taskService.setComplete(task, true);
                    }
                }
            }

            AppWidgetManager manager = AppWidgetManager.getInstance(this);

            int scrollOffset = 0;
            int[] appWidgetIds = null;
            int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

            if (intent != null){
                scrollOffset = intent.getIntExtra(EXTRA_SCROLL_OFFSET, 0);
                appWidgetIds = intent.getIntArrayExtra(APP_WIDGET_IDS);
                appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            }

            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID){
                RemoteViews views = buildUpdate(this, appWidgetId, scrollOffset);
                manager.updateAppWidget(appWidgetId, views);
            } else {
                if (appWidgetIds == null){
                    appWidgetIds = manager.getAppWidgetIds(new ComponentName(this, PowerWidget.class));
                }
                for (int id : appWidgetIds) {
                    RemoteViews views = buildUpdate(this, id, scrollOffset);
                    manager.updateAppWidget(id, views);
                }
            }
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        public RemoteViews buildUpdate(Context context, int appWidgetId, int scrollOffset) {
            DependencyInjectionService.getInstance().inject(this);

            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.widget_power_44);

            String color = Preferences.getStringValue(PowerWidget.PREF_COLOR + appWidgetId);

            int widgetBackground = R.id.widget_bg_black;
            int textColor = Color.WHITE, overdueColor = context.getResources().getColor(R.color.task_list_overdue);
            if ("Black".equals(color)){
                widgetBackground = R.id.widget_bg_black;
                textColor = Color.WHITE;
                overdueColor = context.getResources().getColor(R.color.task_list_overdue);
            } else if ("Blue".equals(color)){
                widgetBackground = R.id.widget_bg_blue;
                textColor = Color.WHITE;
                overdueColor = context.getResources().getColor(R.color.task_list_overdue);
            } else if ("Red".equals(color)){
                widgetBackground = R.id.widget_bg_red;
                textColor = Color.WHITE;
                overdueColor = context.getResources().getColor(R.color.task_list_overdue);
            } else if ("White".equals(color)){
                widgetBackground = R.id.widget_bg_white;
                textColor = Color.BLACK;
                overdueColor = context.getResources().getColor(R.color.task_list_overdue);
            } else {
                widgetBackground = R.id.widget_bg_black;
                textColor = Color.WHITE;
                overdueColor = context.getResources().getColor(R.color.task_list_overdue);
            }
            views.setViewVisibility(widgetBackground, View.VISIBLE);

            // set encouragement
            boolean showEncouragements = Preferences.getBoolean(PowerWidget.PREF_ENCOURAGEMENTS + appWidgetId, true);
            long lastRotation = Preferences.getLong(PowerWidget.PREF_ENCOURAGEMENT_LAST_ROTATION_TIME + appWidgetId, 0);
            if (showEncouragements){
                // is it time to update the encouragement?
                if (System.currentTimeMillis() - lastRotation > ENCOURAGEMENT_CYCLE_TIME){
                    String[] encouragements =  context.getResources().getStringArray(R.array.PPW_encouragements);
                    int encouragementIdx = (int)Math.floor(Math.random() * encouragements.length);
                    Preferences.setString(PowerWidget.PREF_ENCOURAGEMENT_CURRENT + appWidgetId, encouragements[encouragementIdx]);
                    Preferences.setLong(PowerWidget.PREF_ENCOURAGEMENT_LAST_ROTATION_TIME + appWidgetId, System.currentTimeMillis());
                }
                views.setTextViewText(R.id.encouragement_text, Preferences.getStringValue(PowerWidget.PREF_ENCOURAGEMENT_CURRENT + appWidgetId));
                views.setViewVisibility(R.id.speech_bubble, View.VISIBLE);
                views.setViewVisibility(R.id.icon, View.VISIBLE);
            } else {
                views.setViewVisibility(R.id.speech_bubble, View.GONE);
                views.setViewVisibility(R.id.icon, View.GONE);
            }

            TodorooCursor<Task> cursor = null;
            Filter filter = null;
            try {
                filter = getFilter(appWidgetId);
                views.setTextViewText(R.id.widget_title, filter.title);
                views.setTextColor(R.id.widget_title, textColor);

                // create intent to add a new task
                Intent editIntent = new Intent(context, TaskEditActivity.class);
                editIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                if (filter.valuesForNewTasks != null) {
                    String values = AndroidUtilities.contentValuesToSerializedString(filter.valuesForNewTasks);
                    editIntent.putExtra(TaskEditActivity.TOKEN_VALUES, values);
                    editIntent.setType(values);
                } else {
                    editIntent.setType("createNew");
                }
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, editIntent, 0);
                if (showEncouragements){
                    // if encouragements are showing, use icon as plus
                    views.setViewVisibility(R.id.button_plus, View.GONE);
                    views.setOnClickPendingIntent(R.id.icon, pendingIntent);
                } else {
                    views.setViewVisibility(R.id.button_plus, View.VISIBLE);
                    views.setOnClickPendingIntent(R.id.button_plus, pendingIntent);
                }

                Intent listIntent = new Intent(context, TaskListActivity.class);
                listIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                listIntent.putExtra(TaskListActivity.TOKEN_FILTER, filter);
                listIntent.setType(filter.sqlQuery);
                PendingIntent pListIntent = PendingIntent.getActivity(context, 0,
                        listIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                views.setOnClickPendingIntent(R.id.widget_title, pListIntent);

                int flags = Preferences.getInt(SortSelectionActivity.PREF_SORT_FLAGS, 0);
                int sort = Preferences.getInt(SortSelectionActivity.PREF_SORT_SORT, 0);
                String query = SortSelectionActivity.adjustQueryForFlagsAndSort(
                        filter.sqlQuery, flags, sort);
                query = query.replace(Task.COMPLETION_DATE.eq(0).toString(), Criterion.or(Task.COMPLETION_DATE.eq(0),
                        Task.COMPLETION_DATE.gt(DateUtilities.now() - 60000L)).toString());

                database.openForReading();
                cursor = taskService.fetchFiltered(query, null, Task.ID, Task.TITLE,
                        Task.DUE_DATE, Task.IMPORTANCE, Task.COMPLETION_DATE);

                // adjust bounds of scrolling
                if (scrollOffset < 0){
                    scrollOffset = 0;
                } else if (scrollOffset >= cursor.getCount()){
                    scrollOffset = cursor.getCount() - 1 ;
                }

                int[] importanceColors = Task.getImportanceColors(getResources());

                Task task = new Task();
                for (int i = scrollOffset; i < cursor.getCount() && i < 10; i++) {
                    cursor.moveToPosition(i);
                    task.readFromCursor(cursor);

                    String textContent = "";
                    int titleColor = textColor;
                    int dueString = R.string.PPW_due;
                    if(task.isCompleted()) {
                        titleColor = context.getResources().getColor(R.color.task_list_done);
                        dueString = 0;
                    } else if(task.hasDueDate() && task.getValue(Task.DUE_DATE) < DateUtilities.now()){
                        titleColor = overdueColor;
                        dueString = R.string.PPW_past_due;
                    }

                    textContent = task.getValue(Task.TITLE);

                    String dateValue = "";
                    if(dueString != 0 && task.hasDueDate()) {
                        dateValue = getString(dueString) + "\n" +
                            DateUtils.getRelativeTimeSpanString(task.getValue(Task.DUE_DATE));
                    }

                    long taskId = task.getValue(Task.ID);

                    Intent viewTaskIntent = new Intent(context, ShortcutActivity.class);
                    viewTaskIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    viewTaskIntent.putExtra(ShortcutActivity.TOKEN_SINGLE_TASK, taskId);
                    viewTaskIntent.setType(ShortcutActivity.TOKEN_SINGLE_TASK + taskId);
                    Log.d(LOG_TAG, "viewTaskIntent type: "+ShortcutActivity.TOKEN_SINGLE_TASK + taskId);
                    PendingIntent pEditTask = PendingIntent.getActivity(context, 0, viewTaskIntent, PendingIntent.FLAG_CANCEL_CURRENT);

                    Intent markCompleteIntent = new Intent(context, PowerWidget.class);
                    markCompleteIntent.setAction(ACTION_MARK_COMPLETE);
                    markCompleteIntent.putExtra(COMPLETED_TASK_ID, taskId);
                    markCompleteIntent.setType(COMPLETED_TASK_ID + taskId);
                    PendingIntent pMarkCompleteIntent = PendingIntent.getBroadcast(context, 0, markCompleteIntent, 0);

                    // set importance marker
                    views.setInt(TASK_IMPORTANCE[i], "setBackgroundColor", importanceColors[task.getValue(Task.IMPORTANCE)]);
                    // set click listener for checkbox
                    views.setOnClickPendingIntent(TASK_CHECKBOX[i], pMarkCompleteIntent);
                    // set task title
                    if(task.isCompleted())
                        views.setInt(TASK_TITLE[i], "setPaintFlags", Paint.STRIKE_THRU_TEXT_FLAG);
                    views.setTextViewText(TASK_TITLE[i], textContent);
                    views.setTextColor(TASK_TITLE[i], titleColor);
                    // set due date
                    views.setTextViewText(TASK_DUE[i], dateValue);
                    views.setTextColor(TASK_DUE[i], titleColor);
                    // set click listener for text content
                    views.setOnClickPendingIntent(TASK_TITLE[i], pEditTask);
                }


                // create intent to scroll up
                Intent scrollUpIntent = new Intent(context, PowerWidget.class);
                scrollUpIntent.setAction(ACTION_SCROLL_UP);
                scrollUpIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                scrollUpIntent.putExtra(EXTRA_SCROLL_OFFSET, scrollOffset-1);
                scrollUpIntent.setType(AppWidgetManager.EXTRA_APPWIDGET_ID + appWidgetId);
                PendingIntent pScrollUpIntent = PendingIntent.getBroadcast(context, 0, scrollUpIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                views.setOnClickPendingIntent(R.id.scroll_up, pScrollUpIntent);
                if (scrollOffset-1 < 0){
                    // show disabled up button
                    views.setImageViewResource(R.id.scroll_up, R.drawable.scroll_up_disabled);
                } else {
                    views.setImageViewResource(R.id.scroll_up, R.drawable.scroll_up);
                }

                // create intent to scroll down
                Intent scrollDownIntent = new Intent(context, PowerWidget.class);
                scrollDownIntent.setAction(ACTION_SCROLL_DOWN);
                scrollDownIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                scrollDownIntent.putExtra(EXTRA_SCROLL_OFFSET, scrollOffset+1);
                scrollDownIntent.setType(AppWidgetManager.EXTRA_APPWIDGET_ID + appWidgetId);
                PendingIntent pScrollDownIntent = PendingIntent.getBroadcast(context, 0, scrollDownIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                views.setOnClickPendingIntent(R.id.scroll_down, pScrollDownIntent);
                if (scrollOffset+1 >= cursor.getCount()){
                    // show disabled down button
                    views.setImageViewResource(R.id.scroll_down, R.drawable.scroll_down_disabled);
                } else {
                    views.setImageViewResource(R.id.scroll_down, R.drawable.scroll_down);
                }


            } catch (Exception e) {
                // can happen if database is not ready
                Log.e("WIDGET-UPDATE", "Error updating widget", e);
            } finally {
                if(cursor != null)
                    cursor.close();
            }



            return views;
        }

        private Filter getFilter(int widgetId) {
            // base our filter off the inbox filter, replace stuff if we have it
            Filter filter = CoreFilterExposer.buildInboxFilter(getResources());
            String sql = Preferences.getStringValue(PREF_SQL + widgetId);
            if(sql != null)
                filter.sqlQuery = sql;
            String title = Preferences.getStringValue(PREF_TITLE + widgetId);
            if(title != null)
                filter.title = title;
            String contentValues = Preferences.getStringValue(PREF_VALUES + widgetId);
            if(contentValues != null)
                filter.valuesForNewTasks = AndroidUtilities.contentValuesFromSerializedString(contentValues);

            return filter;
        }

    }

}
