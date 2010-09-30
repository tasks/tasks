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
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.ShortcutActivity;
import com.todoroo.astrid.activity.SortSelectionActivity;
import com.todoroo.astrid.activity.TaskEditActivity;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.CoreFilterExposer;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.TaskService;

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

    static Class<?> updateService;
    static {
        AstridDependencyInjector.initialize();
        updateService = PowerWidget.UpdateService.class;
    }


    static final long ENCOURAGEMENT_CYCLE_TIME = 1000 * 60 * 60 * 4; // 4 hours

    static final String ACTION_MARK_COMPLETE = "com.todoroo.astrid.widget.ACTION_MARK_COMPLETE";
    static final String ACTION_SCROLL_UP = "com.todoroo.astrid.widget.ACTION_SCROLL_UP";
    static final String ACTION_SCROLL_DOWN = "com.todoroo.astrid.widget.ACTION_SCROLL_DOWN";

    // Prefix for Shared Preferences
    static final String PREF_COLOR = "powerwidget-color-";
    static final String PREF_ENABLE_CALENDAR = "powerwidget-enableCalendar-";
    static final String PREF_ENCOURAGEMENTS = "powerwidget-enableEncouragements-";
    static final String PREF_TITLE = "powerwidget-title-";
    static final String PREF_SQL = "powerwidget-sql-";
    static final String PREF_VALUES = "powerwidget-values-";
    static final String PREF_ENCOURAGEMENT_LAST_ROTATION_TIME = "powerwidget-encouragementRotationTime-";
    static final String PREF_ENCOURAGEMENT_CURRENT = "powerwidget-encouragementCurrent-";
    static final String PREF_LAST_COMPLETED_ID = "powerwidget-lastCompletedId-";
    static final String PREF_LAST_COMPLETED_POS = "powerwidget-lastCompletedPos-";
    static final String PREF_LAST_COMPLETED_DATE = "powerwidget-lastCompletedDate-";

    public final static String APP_WIDGET_IDS = "com.timsu.astrid.APP_WIDGET_IDS";

    /** id of task to complete */
    public final static String COMPLETED_TASK_ID = "compId";

    /** position in list of task that was completed */
    public final static String COMPLETED_TASK_POSITION = "compPos";

    /** new scroll offset */
    public final static String EXTRA_SCROLL_OFFSET = "soff";

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

    // # of rows defined in the xml file
    static int ROW_LIMIT = 10;

    public static int[] IMPORTANCE_DRAWABLES = new int[] {
        R.drawable.importance_1, R.drawable.importance_2, R.drawable.importance_3,
        R.drawable.importance_4, R.drawable.importance_5, R.drawable.importance_6
    };

    @Autowired
    private TaskDao taskDao;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {

        try {
            super.onUpdate(context, appWidgetManager, appWidgetIds);

            // Start in service to prevent Application Not Responding timeout
            Intent updateIntent = new Intent(context, updateService);
            updateIntent.putExtra(APP_WIDGET_IDS, appWidgetIds);
            context.startService(updateIntent);
        } catch (SecurityException e) {
            // :(
        }
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && ACTION_MARK_COMPLETE.equals(intent.getAction())) {
            DependencyInjectionService.getInstance().inject(this);
            long taskId = intent.getLongExtra(COMPLETED_TASK_ID, -1);
            if (taskId > 0) {
                Task task = taskDao.fetch(taskId, Task.ID, Task.COMPLETION_DATE);
                if (task != null) {
                    task.setValue(Task.COMPLETION_DATE,
                            task.isCompleted() ? 0 : DateUtilities.now());
                    taskDao.saveExisting(task);

                    int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                            AppWidgetManager.INVALID_APPWIDGET_ID);
                    Preferences.setLong(PREF_LAST_COMPLETED_ID +
                            appWidgetId, taskId);
                    Preferences.setInt(PREF_LAST_COMPLETED_POS +
                            appWidgetId, intent.getIntExtra(COMPLETED_TASK_POSITION, -1));
                    Preferences.setLong(PREF_LAST_COMPLETED_DATE +
                            appWidgetId, DateUtilities.now());
                    System.err.println("completed business. posn " +
                            intent.getIntExtra(COMPLETED_TASK_POSITION, 0) +
                            " app id: " + appWidgetId);
                }
            }
        }

        if (intent != null && (ACTION_SCROLL_UP.equals(intent.getAction()) ||
                ACTION_SCROLL_DOWN.equals(intent.getAction()) ||
                ACTION_MARK_COMPLETE.equals(intent.getAction()))) {
            Intent updateIntent = new Intent(context, updateService);
            updateIntent.setAction(intent.getAction());
            updateIntent.putExtras(intent.getExtras());
            context.startService(updateIntent);
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
                updateService));
    }

    /**
     * Update widget with the given id
     * @param id
     */
    public static void updateAppWidget(Context context, int appWidgetId){
        Intent updateIntent = new Intent(context, updateService);
        updateIntent.putExtra(APP_WIDGET_IDS, new int[]{ appWidgetId });
        context.startService(updateIntent);
    }

    public static class UpdateService extends Service {
        static Class<?> widgetClass;
        static int widgetLayout;

        static {
            widgetClass = PowerWidget.class;
            widgetLayout = R.layout.widget_power_44;
        }

        private static final int SCROLL_OFFSET_UNSET = -1;

        private static final Property<?>[] properties = new Property<?>[] { Task.ID, Task.TITLE,
                Task.DUE_DATE, Task.IMPORTANCE, Task.COMPLETION_DATE };

        @Autowired
        private Database database;

        @Autowired
        private TaskService taskService;

        public UpdateService() {
            DependencyInjectionService.getInstance().inject(this);
        }

        @Override
        public void onStart(Intent intent, int startId) {
            ContextManager.setContext(this);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);

            int scrollOffset = SCROLL_OFFSET_UNSET;
            int[] appWidgetIds = null;
            int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

            if (intent != null && intent.hasExtra(EXTRA_SCROLL_OFFSET)) {
                scrollOffset = intent.getIntExtra(EXTRA_SCROLL_OFFSET, 0);
                appWidgetIds = intent.getIntArrayExtra(APP_WIDGET_IDS);
                appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            }

            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID){
                RemoteViews views = buildUpdate(this, appWidgetId, scrollOffset);
                manager.updateAppWidget(appWidgetId, views);
            } else {
                if (appWidgetIds == null){
                    appWidgetIds = manager.getAppWidgetIds(new ComponentName(this, widgetClass));
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
                    widgetLayout);

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
                views.setViewVisibility(R.id.scroll_down, View.GONE);
                views.setViewVisibility(R.id.scroll_down_alt, View.VISIBLE);
                views.setViewVisibility(R.id.scroll_up, View.GONE);
                views.setViewVisibility(R.id.scroll_up_alt, View.VISIBLE);
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

                scrollOffset = Math.max(0, scrollOffset);
                query = query.replaceAll("[lL][iI][mM][iI][tT] +[^ ]+", "") + " LIMIT " +
                    scrollOffset + "," + (ROW_LIMIT + 1);

                // load last completed task
                Task lastCompleted = null;
                int lastCompletedPosition = 0;
                if(DateUtilities.now() - Preferences.getLong(PREF_LAST_COMPLETED_DATE+appWidgetId, 0) < 120000L) {
                    lastCompleted = taskService.fetchById(Preferences.getLong(PREF_LAST_COMPLETED_ID+appWidgetId, -1L),
                            properties);
                    lastCompletedPosition = Preferences.getInt(PREF_LAST_COMPLETED_POS+appWidgetId, 0);
                    if(lastCompleted != null)
                    query = query.replace("WHERE", "WHERE " + Task.ID.neq(lastCompleted.getId()) + " AND ");
                }

                database.openForReading();
                cursor = taskService.fetchFiltered(query, null, properties);

                boolean canScrollDown = cursor.getCount() > 1;

                Task task = new Task();
                int position;
                for (position = 0; position < cursor.getCount() && position < ROW_LIMIT; position++) {
                    if(lastCompleted != null && lastCompletedPosition == position + scrollOffset) {
                        task = lastCompleted;
                    } else {
                        cursor.moveToNext();
                        task.readFromCursor(cursor);
                        if(lastCompleted != null && task.getId() == lastCompleted.getId()) {
                            // oops, get the next one
                            cursor.moveToNext();
                            if(cursor.isAfterLast())
                                continue;
                            task.readFromCursor(cursor);
                        }
                    }

                    long taskId = task.getValue(Task.ID);

                    // importance
                    views.setImageViewResource(TASK_IMPORTANCE[position],
                            IMPORTANCE_DRAWABLES[task.getValue(Task.IMPORTANCE)]);

                    // check box
                    Intent markCompleteIntent = new Intent(context, widgetClass);
                    markCompleteIntent.setAction(ACTION_MARK_COMPLETE);
                    markCompleteIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    markCompleteIntent.putExtra(COMPLETED_TASK_ID, taskId);
                    markCompleteIntent.putExtra(COMPLETED_TASK_POSITION, scrollOffset + position);
                    markCompleteIntent.putExtra(EXTRA_SCROLL_OFFSET, scrollOffset);
                    markCompleteIntent.setType(COMPLETED_TASK_ID + taskId);
                    PendingIntent pMarkCompleteIntent = PendingIntent.getBroadcast(context, 0, markCompleteIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
                    views.setOnClickPendingIntent(TASK_CHECKBOX[position], pMarkCompleteIntent);

                    if(task.isCompleted()) {
                        views.setImageViewResource(TASK_CHECKBOX[position], R.drawable.btn_check_buttonless_on);
                        views.setInt(TASK_TITLE[position], "setPaintFlags", Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
                    } else {
                        views.setImageViewResource(TASK_CHECKBOX[position], R.drawable.btn_check_buttonless_off);
                        views.setInt(TASK_TITLE[position], "setPaintFlags", Paint.ANTI_ALIAS_FLAG);
                    }

                    // title
                    int titleColor = textColor;
                    int dueString = R.string.PPW_due;
                    if(task.isCompleted()) {
                        titleColor = context.getResources().getColor(R.color.task_list_done);
                        dueString = 0;
                    } else if(task.hasDueDate() && task.getValue(Task.DUE_DATE) < DateUtilities.now()){
                        titleColor = overdueColor;
                        dueString = R.string.PPW_past_due;
                    }
                    String textContent = task.getValue(Task.TITLE);
                    views.setTextViewText(TASK_TITLE[position], textContent);
                    views.setTextColor(TASK_TITLE[position], titleColor);

                    Intent viewTaskIntent = new Intent(context, ShortcutActivity.class);
                    viewTaskIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    viewTaskIntent.putExtra(ShortcutActivity.TOKEN_SINGLE_TASK, taskId);
                    viewTaskIntent.setType(ShortcutActivity.TOKEN_SINGLE_TASK + taskId);
                    PendingIntent pEditTask = PendingIntent.getActivity(context, 0, viewTaskIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    views.setOnClickPendingIntent(TASK_TITLE[position], pEditTask);

                    // due date
                    String dateValue = "";
                    if(dueString != 0 && task.hasDueDate()) {
                        dateValue = getString(dueString) + "\n" +
                            DateUtils.getRelativeTimeSpanString(task.getValue(Task.DUE_DATE));
                    }
                    views.setTextViewText(TASK_DUE[position], dateValue);
                    views.setTextColor(TASK_DUE[position], titleColor);

                    views.setViewVisibility(TASK_IMPORTANCE[position], View.VISIBLE);
                    views.setViewVisibility(TASK_CHECKBOX[position], View.VISIBLE);
                    views.setViewVisibility(TASK_TITLE[position], View.VISIBLE);
                    views.setViewVisibility(TASK_DUE[position], View.VISIBLE);
                }

                for(; position < ROW_LIMIT; position++) {
                    views.setViewVisibility(TASK_IMPORTANCE[position], View.INVISIBLE);
                    views.setViewVisibility(TASK_CHECKBOX[position], View.INVISIBLE);
                    views.setViewVisibility(TASK_TITLE[position], View.INVISIBLE);
                    views.setViewVisibility(TASK_DUE[position], View.INVISIBLE);
                }

                // create intent to scroll up
                Intent scrollUpIntent = new Intent(context, widgetClass);
                scrollUpIntent.setAction(ACTION_SCROLL_UP);
                scrollUpIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                scrollUpIntent.setType(AppWidgetManager.EXTRA_APPWIDGET_ID + appWidgetId);
                if (scrollOffset-1 < 0){
                    scrollUpIntent.putExtra(EXTRA_SCROLL_OFFSET, scrollOffset);
                    views.setImageViewResource(R.id.scroll_up, R.drawable.scroll_up_disabled);
                    views.setImageViewResource(R.id.scroll_up_alt, R.drawable.scroll_up_disabled);
                } else {
                    scrollUpIntent.putExtra(EXTRA_SCROLL_OFFSET, scrollOffset-1);
                    views.setImageViewResource(R.id.scroll_up, R.drawable.scroll_up);
                    views.setImageViewResource(R.id.scroll_up_alt, R.drawable.scroll_up);
                }
                PendingIntent pScrollUpIntent = PendingIntent.getBroadcast(context, 0, scrollUpIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                views.setOnClickPendingIntent(R.id.scroll_up, pScrollUpIntent);
                views.setOnClickPendingIntent(R.id.scroll_up_alt, pScrollUpIntent);

                // create intent to scroll down
                Intent scrollDownIntent = new Intent(context, widgetClass);
                scrollDownIntent.setAction(ACTION_SCROLL_DOWN);
                scrollDownIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                scrollDownIntent.setType(AppWidgetManager.EXTRA_APPWIDGET_ID + appWidgetId);
                if (!canScrollDown){
                    scrollDownIntent.putExtra(EXTRA_SCROLL_OFFSET, scrollOffset);
                    views.setImageViewResource(R.id.scroll_down, R.drawable.scroll_down_disabled);
                    views.setImageViewResource(R.id.scroll_down_alt, R.drawable.scroll_down_disabled);
                } else {
                    scrollDownIntent.putExtra(EXTRA_SCROLL_OFFSET, scrollOffset+1);
                    views.setImageViewResource(R.id.scroll_down, R.drawable.scroll_down);
                    views.setImageViewResource(R.id.scroll_down_alt, R.drawable.scroll_down);
                }
                PendingIntent pScrollDownIntent = PendingIntent.getBroadcast(context, 0, scrollDownIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                views.setOnClickPendingIntent(R.id.scroll_down, pScrollDownIntent);
                views.setOnClickPendingIntent(R.id.scroll_down_alt, pScrollDownIntent);
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
