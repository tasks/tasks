package com.todoroo.astrid.service;

import android.content.ContentValues;
import android.net.Uri;
import android.text.TextUtils;

import com.google.common.base.Strings;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.helper.UUIDHelper;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.utility.TitleParser;

import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.Tag;
import org.tasks.data.TagDao;
import org.tasks.data.TagData;
import org.tasks.data.TagDataDao;
import org.tasks.preferences.Preferences;

import java.util.ArrayList;
import java.util.Map;

import javax.inject.Inject;

import timber.log.Timber;

import static com.todoroo.andlib.utility.DateUtilities.now;

public class TaskCreator {

    private final GCalHelper gcalHelper;
    private final Preferences preferences;
    private final TagDao tagDao;
    private final GoogleTaskDao googleTaskDao;
    private final Tracker tracker;
    private final TagDataDao tagDataDao;
    private final TaskDao taskDao;
    private final TagService tagService;

    @Inject
    public TaskCreator(GCalHelper gcalHelper, Preferences preferences, TagDataDao tagDataDao,
                       TaskDao taskDao, TagService tagService, TagDao tagDao,
                       GoogleTaskDao googleTaskDao, Tracker tracker) {
        this.gcalHelper = gcalHelper;
        this.preferences = preferences;
        this.tagDataDao = tagDataDao;
        this.taskDao = taskDao;
        this.tagService = tagService;
        this.tagDao = tagDao;
        this.googleTaskDao = googleTaskDao;
        this.tracker = tracker;
    }

    public Task basicQuickAddTask(String title) {
        title = title.trim();

        Task task = createWithValues(null, title);
        taskDao.createNew(task);

        boolean gcalCreateEventEnabled = preferences.isDefaultCalendarSet() && task.hasDueDate(); //$NON-NLS-1$
        if (!TextUtils.isEmpty(task.getTitle()) && gcalCreateEventEnabled && TextUtils.isEmpty(task.getCalendarURI())) {
            Uri calendarUri = gcalHelper.createTaskEvent(task, new ContentValues());
            task.setCalendarUri(calendarUri.toString());
        }

        createTags(task);

        String googleTaskList = task.getTransitory(GoogleTask.KEY);
        if (!Strings.isNullOrEmpty(googleTaskList)) {
            googleTaskDao.insert(new GoogleTask(task.getId(), googleTaskList));
        }

        taskDao.save(task, null);
        return task;
    }

    /**
     * Create task from the given content values, saving it. This version
     * doesn't need to start with a base task model.
     */
    public Task createWithValues(Map<String, Object> values, String title) {
        Task task = new Task();
        task.setCreationDate(now());
        task.setModificationDate(now());
        if (title != null) {
            task.setTitle(title.trim());
        }

        task.setUuid(UUIDHelper.newUUID());

        task.setImportance(preferences.getIntegerFromString(R.string.p_default_importance_key, Task.IMPORTANCE_SHOULD_DO));
        task.setDueDate(Task.createDueDate(
                preferences.getIntegerFromString(R.string.p_default_urgency_key, Task.URGENCY_NONE), 0));
        int setting = preferences.getIntegerFromString(R.string.p_default_hideUntil_key,
                Task.HIDE_UNTIL_NONE);
        task.setHideUntil(task.createHideUntil(setting, 0));
        setDefaultReminders(preferences, task);

        ArrayList<String> tags = new ArrayList<>();

        if (values != null && values.size() > 0) {
            for (Map.Entry<String, Object> item : values.entrySet()) {
                String key = item.getKey();
                Object value = item.getValue();
                if (key.equals(Tag.KEY)) {
                    tags.add((String) value);
                } else if (key.equals(GoogleTask.KEY)) {
                    task.putTransitory(key, value);
                } else {
                    if (value instanceof String) {
                        value = PermaSql.replacePlaceholders((String) value);
                    }

                    if (key.equals("dueDate")) {
                        task.setDueDate(Long.valueOf((String) value));
                    } else if (key.equals("importance")) {
                        task.setImportance(Integer.valueOf((String) value));
                    } else {
                        tracker.reportEvent(Tracking.Events.TASK_CREATION_FAILED, "Unhandled key: " + key);
                    }
                }
            }
        }

        try {
            TitleParser.parse(tagService, task, tags);
        } catch (Throwable e) {
            Timber.e(e, e.getMessage());
        }

        task.setTags(tags);

        return task;
    }

    private static void setDefaultReminders(Preferences preferences, Task task) {
        task.setReminderPeriod(DateUtilities.ONE_HOUR *
                preferences.getIntegerFromString(R.string.p_rmd_default_random_hours,
                        0));
        task.setReminderFlags(preferences.getDefaultReminders() | preferences.getDefaultRingMode());
    }

    public void createTags(Task task) {
        for (String tag : task.getTags()) {
            TagData tagData = tagDataDao.getTagByName(tag);
            if (tagData == null) {
                tagData = new TagData();
                tagData.setName(tag);
                tagDataDao.createNew(tagData);
            }
            Tag link = new Tag(task.getId(), task.getUuid(), tagData.getName(), tagData.getRemoteId());
            tagDao.insert(link);
        }
    }
}
