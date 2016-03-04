package com.todoroo.astrid.service;

import android.content.ContentValues;
import android.net.Uri;
import android.text.TextUtils;

import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;

import org.tasks.preferences.Preferences;

import javax.inject.Inject;

public class TaskCreator {

    private final TaskService taskService;
    private final GCalHelper gcalHelper;
    private Preferences preferences;

    @Inject
    public TaskCreator(TaskService taskService, GCalHelper gcalHelper, Preferences preferences) {
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
        addToCalendar(task);

        return task;
    }

    public void addToCalendar(Task task) {
        boolean gcalCreateEventEnabled = preferences.isDefaultCalendarSet() && task.hasDueDate(); //$NON-NLS-1$
        if (!TextUtils.isEmpty(task.getTitle()) && gcalCreateEventEnabled && TextUtils.isEmpty(task.getCalendarURI())) {
            Uri calendarUri = gcalHelper.createTaskEvent(task, new ContentValues());
            task.setCalendarUri(calendarUri.toString());
            task.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
            taskService.save(task);
        }
    }
}
