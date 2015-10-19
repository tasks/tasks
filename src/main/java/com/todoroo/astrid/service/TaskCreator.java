package com.todoroo.astrid.service;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;

import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

public class TaskCreator {

    private final Context context;
    private final TaskService taskService;
    private final GCalHelper gcalHelper;
    private Preferences preferences;

    @Inject
    public TaskCreator(@ForApplication Context context, TaskService taskService,
                       GCalHelper gcalHelper, Preferences preferences) {
        this.context = context;
        this.taskService = taskService;
        this.gcalHelper = gcalHelper;
        this.preferences = preferences;
    }

    public Task basicQuickAddTask(String title) {
        if (TextUtils.isEmpty(title)) {
            return null;
        }

        title = title.trim();

        Task task = taskService.createWithValues(null, title);
        addToCalendar(task, title);

        return task;
    }

    public void addToCalendar(Task task, String title) {
        boolean gcalCreateEventEnabled = preferences.isDefaultCalendarSet() && task.hasDueDate(); //$NON-NLS-1$
        if (!TextUtils.isEmpty(title) && gcalCreateEventEnabled && TextUtils.isEmpty(task.getCalendarURI())) {
            Uri calendarUri = gcalHelper.createTaskEvent(task,
                    context.getContentResolver(), new ContentValues());
            task.setCalendarUri(calendarUri.toString());
            task.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
            taskService.save(task);
        }
    }
}
