package org.tasks.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Paint;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.activity.TaskEditFragment;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.subtasks.SubtasksHelper;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.widget.TasksWidget;
import com.todoroo.astrid.widget.WidgetConfigActivity;

import org.tasks.R;
import org.tasks.preferences.Preferences;

import timber.log.Timber;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ScrollableViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Database database;
    private final TaskService taskService;
    private final SubtasksHelper subtasksHelper;
    private final Preferences preferences;
    private final Context context;
    private final Filter filter;
    private final int widgetId;
    private final boolean dark;
    private final boolean showDueDates;
    private final boolean hideCheckboxes;
    private final DueDateFormatter dueDateFormatter;

    private TodorooCursor<Task> cursor;

    public ScrollableViewsFactory(
            SubtasksHelper subtasksHelper,
            Preferences preferences,
            Context context,
            Filter filter,
            int widgetId,
            Database database,
            TaskService taskService) {
        this.subtasksHelper = subtasksHelper;
        this.preferences = preferences;
        this.context = context;
        this.filter = filter;
        this.widgetId = widgetId;
        this.database = database;
        this.taskService = taskService;

        dueDateFormatter = new DueDateFormatter(context);
        dark = preferences.useDarkWidgetTheme(widgetId);
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

    private int getCheckbox(Task task) {
        boolean completed = task.isCompleted();

        int value = task.getImportance();
        if (value >= TaskAdapter.IMPORTANCE_RESOURCES.length) {
            value = TaskAdapter.IMPORTANCE_RESOURCES.length - 1;
        }
        int[] boxes;
        if (!TextUtils.isEmpty(task.getRecurrence())) {
            boxes = completed ? TaskAdapter.IMPORTANCE_REPEAT_RESOURCES_CHECKED : TaskAdapter.IMPORTANCE_REPEAT_RESOURCES;
        } else {
            boxes = completed ? TaskAdapter.IMPORTANCE_RESOURCES_CHECKED : TaskAdapter.IMPORTANCE_RESOURCES;
        }
        return boxes[value];
    }

    public RemoteViews buildUpdate(int position) {
        try {
            Task task = getTask(position);
            if (task == null) {
                return null;
            }

            String textContent;
            Resources r = context.getResources();
            int textColor = r.getColor(dark ? R.color.widget_text_color_dark : R.color.widget_text_color_light);

            textContent = task.getTitle();

            RemoteViews row = new RemoteViews(Constants.PACKAGE, R.layout.widget_row);

            if (task.isCompleted()) {
                textColor = r.getColor(R.color.task_list_done);
                row.setInt(R.id.text, "setPaintFlags", Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
            } else {
                row.setInt(R.id.text, "setPaintFlags", Paint.ANTI_ALIAS_FLAG);
            }

            if (showDueDates) {
                dueDateFormatter.formatDueDate(row, task, textColor);
            } else if (task.hasDueDate() && task.isOverdue()) {
                textColor = r.getColor(R.color.overdue);
            }

            row.setTextViewText(R.id.text, textContent);
            row.setTextColor(R.id.text, textColor);
            row.setImageViewResource(R.id.completeBox, getCheckbox(task));

            Intent editIntent = new Intent();
            editIntent.setAction(TasksWidget.EDIT_TASK);
            editIntent.putExtra(TaskListFragment.TOKEN_FILTER, filter);
            editIntent.putExtra(TaskEditFragment.TOKEN_ID, task.getId());
            editIntent.putExtra(TaskListActivity.OPEN_TASK, task.getId());
            row.setOnClickFillInIntent(R.id.text, editIntent);

            if (hideCheckboxes) {
                row.setViewVisibility(R.id.completeBox, View.GONE);
            } else {
                Intent completeIntent = new Intent();
                completeIntent.setAction(TasksWidget.COMPLETE_TASK);
                completeIntent.putExtra(TaskEditFragment.TOKEN_ID, task.getId());
                row.setOnClickFillInIntent(R.id.completeBox, completeIntent);
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

        String query = SortHelper.adjustQueryForFlagsAndSort(preferences,
                filter.getSqlQuery(), sort).replaceAll("LIMIT \\d+", "");

        String tagName = preferences.getStringValue(WidgetConfigActivity.PREF_TITLE + widgetId);

        return subtasksHelper.applySubtasksToWidgetFilter(filter, query, tagName, 0);
    }
}
