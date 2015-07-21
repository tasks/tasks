package org.tasks.widget;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.RemoteViews;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;

import org.tasks.R;
import org.tasks.injection.ForApplication;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import static org.tasks.date.DateTimeUtils.newDate;

public class DueDateFormatter {

    private final Map<Long, String> dateCache = new HashMap<>();
    private final Context context;
    private final Resources resources;

    @Inject
    public DueDateFormatter(@ForApplication Context context) {
        this.context = context;
        resources = context.getResources();
    }

    public void formatDueDate(RemoteViews row, Task task, int textColor) {
        if (task.hasDueDate() || task.hasDueTime()) {
            row.setViewVisibility(R.id.dueDate, View.VISIBLE);
            row.setTextViewText(R.id.dueDate, task.isCompleted()
                    ? resources.getString(R.string.TAd_completed, formatDate(task.getCompletionDate()))
                    : formatDate(task.getDueDate()));
            row.setTextColor(R.id.dueDate, task.isOverdue() ? resources.getColor(R.color.overdue) : textColor);
        } else {
            row.setViewVisibility(R.id.dueDate, View.GONE);
        }
    }

    private String formatDate(long date) {
        if (dateCache.containsKey(date)) {
            return dateCache.get(date);
        }

        String formatString = "%s %s";
        String string = DateUtilities.getRelativeDay(context, date, false);
        if (Task.hasDueTime(date)) {
            string = String.format(formatString, string, //$NON-NLS-1$
                    DateUtilities.getTimeString(context, date));
        }

        dateCache.put(date, string);
        return string;
    }
}
