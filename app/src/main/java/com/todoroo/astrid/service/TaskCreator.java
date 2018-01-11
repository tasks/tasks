package com.todoroo.astrid.service;

import android.content.ContentValues;
import android.net.Uri;
import android.text.TextUtils;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.utility.TitleParser;

import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.Tag;
import org.tasks.data.TagDao;
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
        addToCalendar(task);

        return task;
    }

    public void addToCalendar(Task task) {
        boolean gcalCreateEventEnabled = preferences.isDefaultCalendarSet() && task.hasDueDate(); //$NON-NLS-1$
        if (!TextUtils.isEmpty(task.getTitle()) && gcalCreateEventEnabled && TextUtils.isEmpty(task.getCalendarURI())) {
            Uri calendarUri = gcalHelper.createTaskEvent(task, new ContentValues());
            task.setCalendarUri(calendarUri.toString());
            task.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
            taskDao.save(task);
        }
    }

    /**
     * Create task from the given content values, saving it. This version
     * doesn't need to start with a base task model.
     */
    public Task createWithValues(Map<String, Object> values, String title) {
        return createWithValues(new Task(), values, title);
    }

    Task createWithValues(Task task, Map<String, Object> values, String title) {
        if (title != null) {
            task.setTitle(title.trim());
        }

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
                if (key.equals(Tag.KEY) || key.equals(GoogleTask.KEY)) {
                    continue;
                }

                if (value instanceof String) {
                    value = PermaSql.replacePlaceholders((String) value);
                }

                AndroidUtilities.putInto(forTask, key, value);
            }
            task.mergeWithoutReplacement(forTask);
        }

        saveWithoutPublishingFilterUpdate(task);

        if (values != null) {
            if (values.containsKey(Tag.KEY)) {
                createLink(task, (String) values.get(Tag.KEY));
            }
            if (values.containsKey(GoogleTask.KEY)) {
                GoogleTask googleTask = new GoogleTask(task.getId(), (String) values.get(GoogleTask.KEY));
                googleTaskDao.insert(googleTask);
            }
        }

        for(String tag : tags) {
            createLink(task, tag);
        }

        return task;
    }

    private void saveWithoutPublishingFilterUpdate(Task item) {
        taskDao.save(item);
    }

    private void createLink(Task task, String tagName) {
        TagData tagData = tagDataDao.getTagByName(tagName);
        if (tagData == null) {
            tagData = new TagData();
            tagData.setName(tagName);
            tagDataDao.persist(tagData);
        }
        createLink(task, tagData.getName(), tagData.getRemoteId());
    }

    private void createLink(Task task, String tagName, String tagUuid) {
        Tag link = new Tag(task.getId(), task.getUuid(), tagName, tagUuid);
        tagDao.insert(link);
    }

    /**
     * Parse quick add markup for the given task
     * @param tags an empty array to apply tags to
     */
    void parseQuickAddMarkup(Task task, ArrayList<String> tags) {
        TitleParser.parse(tagService, task, tags);
    }
}
