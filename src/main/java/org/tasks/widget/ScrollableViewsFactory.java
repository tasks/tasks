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
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.subtasks.SubtasksHelper;

import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;
import org.tasks.preferences.Theme;
import org.tasks.ui.WidgetCheckBoxes;

import timber.log.Timber;

public class ScrollableViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final WidgetCheckBoxes checkBoxes;
    private final int themeTextColor;
    private final int widgetId;
    private final Database database;
    private final TaskService taskService;
    private final DefaultFilterProvider defaultFilterProvider;
    private final SubtasksHelper subtasksHelper;
    private final Preferences preferences;
    private final Context context;
    private final String filterId;
    private final boolean showDueDates;
    private final boolean hideCheckboxes;

    private TodorooCursor<Task> cursor;

    public ScrollableViewsFactory(
            SubtasksHelper subtasksHelper,
            Preferences preferences,
            Context context,
            String filterId,
            Theme theme,
            int widgetId,
            Database database,
            TaskService taskService,
            DefaultFilterProvider defaultFilterProvider) {
        this.subtasksHelper = subtasksHelper;
        this.preferences = preferences;
        this.context = context;
        this.filterId = filterId;
        this.widgetId = widgetId;
        this.database = database;
        this.taskService = taskService;
        this.defaultFilterProvider = defaultFilterProvider;

        themeTextColor = theme.getTextColor();
        checkBoxes = new WidgetCheckBoxes(context);
        showDueDates = preferences.getBoolean(WidgetConfigActivity.PREF_SHOW_DUE_DATE + widgetId, false);
        hideCheckboxes = preferences.getBoolean(WidgetConfigActivity.PREF_HIDE_CHECKBOXES + widgetId, false);
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

    public RemoteViews buildUpdate(int position) {
        try {
            Task task = getTask(position);
            if (task == null) {
                return null;
            }
            String textContent;
            Resources r = context.getResources();
            int textColor = themeTextColor;

            textContent = task.getTitle();

            RemoteViews row = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.widget_row);

            if (task.isCompleted()) {
                textColor = r.getColor(R.color.task_list_done);
                row.setInt(R.id.widget_text, "setPaintFlags", Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
            } else {
                row.setInt(R.id.widget_text, "setPaintFlags", Paint.ANTI_ALIAS_FLAG);
            }

            if (showDueDates) {
                formatDueDate(row, task, textColor);
            } else if (task.hasDueDate() && task.isOverdue()) {
                textColor = r.getColor(R.color.overdue);
            }

            row.setTextViewText(R.id.widget_text, textContent);
            row.setTextColor(R.id.widget_text, textColor);
            row.setImageViewBitmap(R.id.widget_complete_box, getCheckbox(task));

            long taskId = task.getId();
            Intent editIntent = new Intent(TasksWidget.EDIT_TASK);
            editIntent.putExtra(TasksWidget.EXTRA_FILTER_ID, filterId);
            editIntent.putExtra(TasksWidget.EXTRA_ID, taskId);
            row.setOnClickFillInIntent(R.id.widget_row, editIntent);

            if (hideCheckboxes) {
                row.setViewVisibility(R.id.widget_complete_box, View.GONE);
            } else {
                Intent completeIntent = new Intent(TasksWidget.COMPLETE_TASK);
                completeIntent.putExtra(TasksWidget.EXTRA_ID, taskId);
                row.setOnClickFillInIntent(R.id.widget_complete_box, completeIntent);
            }

            return row;
        } catch (Exception e) {
            Timber.e(e, e.getMessage());
        }

        return null;
    }

    private TodorooCursor<Task> getCursor() {
        String query = getQuery();
        return taskService.fetchFiltered(query, null, Task.ID, Task.TITLE, Task.DUE_DATE, Task.COMPLETION_DATE, Task.IMPORTANCE, Task.RECURRENCE);
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
        Filter filter = defaultFilterProvider.getFilterFromPreference(filterId);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.scrollable_widget);
        rv.setTextViewText(R.id.widget_title, filter.listingTitle);
        appWidgetManager.partiallyUpdateAppWidget(widgetId, rv);
        String query = SortHelper.adjustQueryForFlagsAndSort(preferences, filter.getSqlQuery(), sort).replaceAll("LIMIT \\d+", "");
        return subtasksHelper.applySubtasksToWidgetFilter(filter, query, filter.listingTitle, 0);
    }

    public void formatDueDate(RemoteViews row, Task task, int textColor) {
        if (task.hasDueDate()) {
            Resources resources = context.getResources();
            row.setViewVisibility(R.id.widget_due_date, View.VISIBLE);
            row.setTextViewText(R.id.widget_due_date, task.isCompleted()
                    ? resources.getString(R.string.TAd_completed, DateUtilities.getRelativeDateStringWithTime(context, task.getCompletionDate()))
                    : DateUtilities.getRelativeDateStringWithTime(context, task.getDueDate()));
            row.setTextColor(R.id.widget_due_date, task.isOverdue() ? resources.getColor(R.color.overdue) : textColor);
        } else {
            row.setViewVisibility(R.id.widget_due_date, View.GONE);
        }
    }
}
