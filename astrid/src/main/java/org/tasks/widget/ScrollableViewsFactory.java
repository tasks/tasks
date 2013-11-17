package org.tasks.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.utility.Constants;

import org.tasks.R;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ScrollableViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    @Autowired
    Database database;

    @Autowired
    TaskService taskService;

    private final Context context;
    private String query;
    private boolean dark;

    private TodorooCursor<Task> cursor;

    public ScrollableViewsFactory(Context context, String query, boolean dark) {
        this.context = context;
        this.query = query;
        this.dark = dark;
    }

    @Override
    public void onCreate() {
        DependencyInjectionService.getInstance().inject(this);

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
        return getTask(position).getId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    public RemoteViews buildUpdate(int position) {
        try {
            Task task = getTask(position);

            String textContent;
            Resources r = context.getResources();
            int textColor = r.getColor(dark ? R.color.widget_text_color_dark : R.color.widget_text_color_light);

            textContent = task.getValue(Task.TITLE);

            if (task.isCompleted()) {
                textColor = r.getColor(R.color.task_list_done);
            } else if (task.hasDueDate() && task.isOverdue()) {
                textColor = r.getColor(R.color.task_list_overdue);
            }

            RemoteViews row = new RemoteViews(Constants.PACKAGE, R.layout.widget_row);

            row.setTextViewText(R.id.text, textContent);
            row.setTextColor(R.id.text, textColor);

            row.setOnClickFillInIntent(R.id.text, new Intent());

            return row;
        } catch (Exception e) {
            // can happen if database is not ready
            Log.e("WIDGET-UPDATE", "Error updating widget", e);
        }

        return null;
    }

    private TodorooCursor<Task> getCursor() {
        return taskService.fetchFiltered(query, null, Task.ID, Task.TITLE, Task.DUE_DATE, Task.COMPLETION_DATE);
    }

    private Task getTask(int position) {
        cursor.moveToPosition(position);
        return new Task(cursor);
    }
}
