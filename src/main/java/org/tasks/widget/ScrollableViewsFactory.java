package org.tasks.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.subtasks.SubtasksHelper;

import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.locale.Locale;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.WidgetTheme;
import org.tasks.ui.WidgetCheckBoxes;

import timber.log.Timber;

import static android.support.v4.content.ContextCompat.getColor;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybeanMR1;

class ScrollableViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final WidgetCheckBoxes checkBoxes;
    private final ThemeCache themeCache;
    private final int widgetId;
    private final Database database;
    private final TaskDao taskDao;
    private final DefaultFilterProvider defaultFilterProvider;
    private final SubtasksHelper subtasksHelper;
    private final Preferences preferences;
    private final WidgetPreferences widgetPreferences;
    private final Context context;

    private boolean showDueDates;
    private boolean showCheckboxes;
    private float textSize;
    private float dueDateTextSize;
    private String filterId;
    private int textColorPrimary;
    private int textColorSecondary;

    private TodorooCursor<Task> cursor;

    ScrollableViewsFactory(
            SubtasksHelper subtasksHelper,
            Preferences preferences,
            Context context,
            int widgetId,
            Database database,
            TaskDao taskDao,
            DefaultFilterProvider defaultFilterProvider,
            WidgetCheckBoxes checkBoxes,
            ThemeCache themeCache) {
        this.subtasksHelper = subtasksHelper;
        this.preferences = preferences;
        this.context = context;
        this.widgetId = widgetId;
        this.database = database;
        this.taskDao = taskDao;
        this.defaultFilterProvider = defaultFilterProvider;
        this.checkBoxes = checkBoxes;
        this.themeCache = themeCache;

        widgetPreferences = new WidgetPreferences(context, preferences, widgetId);

        updateSettings();
    }

    @Override
    public void onCreate() {
        database.openForReading();
        cursor = getCursor();
    }

    @Override
    public void onDataSetChanged() {
        cursor = getCursor();
    }

    @Override
    public void onDestroy() {
        cursor.close();
    }

    @Override
    public int getCount() {
        return cursor.getCount();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        return buildUpdate(position);
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        Task task = getTask(position);
        return task == null ? 0 : task.getId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private Bitmap getCheckbox(Task task) {
        if (task.isCompleted()) {
            return checkBoxes.getCompletedCheckbox(task.getImportance());
        } else if (TextUtils.isEmpty(task.getRecurrence())) {
            return checkBoxes.getCheckBox(task.getImportance());
        } else {
            return checkBoxes.getRepeatingCheckBox(task.getImportance());
        }
    }

    private RemoteViews buildUpdate(int position) {
        try {
            Task task = getTask(position);
            if (task == null) {
                return null;
            }
            String textContent;
            int textColorTitle = textColorPrimary;

            textContent = task.getTitle();

            RemoteViews row = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.widget_row);

            if (task.isCompleted()) {
                textColorTitle = textColorSecondary;
                row.setInt(R.id.widget_text, "setPaintFlags", Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
            } else {
                row.setInt(R.id.widget_text, "setPaintFlags", Paint.ANTI_ALIAS_FLAG);
            }
            row.setFloat(R.id.widget_text, "setTextSize", textSize);
            if (showDueDates) {
                formatDueDate(row, task);
            } else {
                row.setViewVisibility(R.id.widget_due_date, View.GONE);
                if (task.hasDueDate() && task.isOverdue()) {
                    textColorTitle = getColor(context, R.color.overdue);
                }
            }

            row.setTextViewText(R.id.widget_text, textContent);
            row.setTextColor(R.id.widget_text, textColorTitle);
            row.setImageViewBitmap(R.id.widget_complete_box, getCheckbox(task));

            long taskId = task.getId();
            Intent editIntent = new Intent(TasksWidget.EDIT_TASK);
            editIntent.putExtra(TasksWidget.EXTRA_FILTER_ID, filterId);
            editIntent.putExtra(TasksWidget.EXTRA_ID, taskId);
            row.setOnClickFillInIntent(R.id.widget_row, editIntent);

            if (showCheckboxes) {
                row.setViewVisibility(R.id.widget_complete_box, View.VISIBLE);
                Intent completeIntent = new Intent(TasksWidget.COMPLETE_TASK);
                completeIntent.putExtra(TasksWidget.EXTRA_ID, taskId);
                row.setOnClickFillInIntent(R.id.widget_complete_box, completeIntent);
            } else {
                row.setViewVisibility(R.id.widget_complete_box, View.GONE);
            }

            if (atLeastJellybeanMR1()) {
                row.setInt(R.id.widget_row, "setLayoutDirection", Locale.getInstance(context).getDirectionality());
            }

            return row;
        } catch (Exception e) {
            Timber.e(e, e.getMessage());
        }

        return null;
    }

    private TodorooCursor<Task> getCursor() {
        String query = getQuery();
        return taskDao.fetchFiltered(query, Task.ID, Task.TITLE, Task.DUE_DATE, Task.COMPLETION_DATE, Task.IMPORTANCE, Task.RECURRENCE);
    }

    private Task getTask(int position) {
        if (position < cursor.getCount()) {
            cursor.moveToPosition(position);
            return new Task(cursor);
        }
        Timber.w("requested task at position %s, cursor count is %s", position, cursor.getCount());
        return null;
    }

    private String getQuery() {
        int sort = preferences.getSortMode();
        if(sort == 0) {
            sort = SortHelper.SORT_WIDGET;
        }
        updateSettings();
        Filter filter = defaultFilterProvider.getFilterFromPreference(filterId);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.scrollable_widget);
        rv.setTextViewText(R.id.widget_title, filter.listingTitle);
        if (atLeastJellybeanMR1()) {
            rv.setInt(R.id.widget, "setLayoutDirection", Locale.getInstance(context).getDirectionality());
        }
        appWidgetManager.partiallyUpdateAppWidget(widgetId, rv);
        String query = SortHelper.adjustQueryForFlagsAndSort(preferences, filter.getSqlQuery(), sort).replaceAll("LIMIT \\d+", "");
        return subtasksHelper.applySubtasksToWidgetFilter(filter, query);
    }

    private void formatDueDate(RemoteViews row, Task task) {
        if (task.hasDueDate()) {
            Resources resources = context.getResources();
            row.setViewVisibility(R.id.widget_due_date, View.VISIBLE);
            row.setTextViewText(R.id.widget_due_date, task.isCompleted()
                    ? resources.getString(R.string.TAd_completed, DateUtilities.getRelativeDateStringWithTime(context, task.getCompletionDate()))
                    : DateUtilities.getRelativeDateStringWithTime(context, task.getDueDate()));
            //noinspection ResourceAsColor
            row.setTextColor(R.id.widget_due_date, task.isOverdue() ? getColor(context, R.color.overdue) : textColorSecondary);
            row.setFloat(R.id.widget_due_date, "setTextSize", dueDateTextSize);
        } else {
            row.setViewVisibility(R.id.widget_due_date, View.GONE);
        }
    }

    private void updateSettings() {
        WidgetTheme widgetTheme = themeCache.getWidgetTheme(widgetPreferences.getThemeIndex());
        textColorPrimary = widgetTheme.getTextColorPrimary();
        textColorSecondary = widgetTheme.getTextColorSecondary();
        showDueDates = widgetPreferences.showDueDate();
        showCheckboxes = widgetPreferences.showCheckboxes();
        textSize = widgetPreferences.getFontSize();
        dueDateTextSize = Math.max(10, textSize - 2);
        filterId = widgetPreferences.getFilterId();
    }
}
