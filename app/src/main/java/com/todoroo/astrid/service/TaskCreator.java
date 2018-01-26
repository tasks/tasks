package com.todoroo.astrid.service;

import android.content.ContentValues;
import android.net.Uri;
import android.text.TextUtils;

import com.google.common.base.Strings;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.helper.UUIDHelper;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.utility.TitleParser;

import org.tasks.R;
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

public class TaskCreator {

    private final GCalHelper gcalHelper;
    private final Preferences preferences;
    private final TagDao tagDao;
    private final GoogleTaskDao googleTaskDao;
    private final TagDataDao tagDataDao;
    private final TaskDao taskDao;
    private final TagService tagService;

    @Inject
    public TaskCreator(GCalHelper gcalHelper, Preferences preferences, TagDataDao tagDataDao,
                       TaskDao taskDao, TagService tagService, TagDao tagDao, GoogleTaskDao googleTaskDao) {
        this.gcalHelper = gcalHelper;
        this.preferences = preferences;
        this.tagDataDao = tagDataDao;
        this.taskDao = taskDao;
        this.tagService = tagService;
        this.tagDao = tagDao;
        this.googleTaskDao = googleTaskDao;
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

        taskDao.save(task);
        return task;
    }

    /**
     * Create task from the given content values, saving it. This version
     * doesn't need to start with a base task model.
     */
    public Task createWithValues(Map<String, Object> values, String title) {
        Task task = new Task();
        if (title != null) {
            task.setTitle(title.trim());
        }

        task.setUuid(UUIDHelper.newUUID());

        ArrayList<String> tags = new ArrayList<>();
        try {
            parseQuickAddMarkup(task, tags);
        } catch (Throwable e) {
            Timber.e(e, e.getMessage());
        }

        if (values != null && values.size() > 0) {
            ContentValues forTask = new ContentValues();
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

                    AndroidUtilities.putInto(forTask, key, value);
                }
            }
            task.mergeWithoutReplacement(forTask);
        }

        if (!task.isModified(Task.IMPORTANCE)) {
            task.setImportance(preferences.getIntegerFromString(R.string.p_default_importance_key, Task.IMPORTANCE_SHOULD_DO));
        }

        if(!task.isModified(Task.DUE_DATE)) {
            task.setDueDate(Task.createDueDate(
                    preferences.getIntegerFromString(R.string.p_default_urgency_key, Task.URGENCY_NONE), 0));
        }

        if(!task.isModified(Task.HIDE_UNTIL)) {
            int setting = preferences.getIntegerFromString(R.string.p_default_hideUntil_key,
                    Task.HIDE_UNTIL_NONE);
            task.setHideUntil(task.createHideUntil(setting, 0));
        }

        setDefaultReminders(preferences, task);

        task.setTags(tags);

        return task;
    }

    public static void setDefaultReminders(Preferences preferences, Task task) {
        if(!task.isModified(Task.REMINDER_PERIOD)) {
            task.setReminderPeriod(DateUtilities.ONE_HOUR *
                    preferences.getIntegerFromString(R.string.p_rmd_default_random_hours,
                            0));
        }

        if(!task.isModified(Task.REMINDER_FLAGS)) {
            task.setReminderFlags(preferences.getDefaultReminders() | preferences.getDefaultRingMode());
        }
    }

    public void createTags(Task task) {
        for (String tag : task.getTags()) {
            TagData tagData = tagDataDao.getTagByName(tag);
            if (tagData == null) {
                tagData = new TagData();
                tagData.setName(tag);
                tagDataDao.persist(tagData);
            }
            Tag link = new Tag(task.getId(), task.getUuid(), tagData.getName(), tagData.getRemoteId());
            tagDao.insert(link);
        }
    }

    /**
     * Parse quick add markup for the given task
     * @param tags an empty array to apply tags to
     */
    void parseQuickAddMarkup(Task task, ArrayList<String> tags) {
        TitleParser.parse(tagService, task, tags);
    }
}
