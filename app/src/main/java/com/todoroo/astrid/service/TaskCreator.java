package com.todoroo.astrid.service;

import static com.todoroo.andlib.utility.DateUtilities.now;

import android.content.ContentValues;
import android.net.Uri;
import android.text.TextUtils;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.Task.Priority;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.helper.UUIDHelper;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.utility.TitleParser;
import java.util.ArrayList;
import java.util.Map;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.data.CaldavDao;
import org.tasks.data.CaldavTask;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.Tag;
import org.tasks.data.TagDao;
import org.tasks.data.TagData;
import org.tasks.data.TagDataDao;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

public class TaskCreator {

  private final GCalHelper gcalHelper;
  private final Preferences preferences;
  private final TagDao tagDao;
  private final GoogleTaskDao googleTaskDao;
  private final Tracker tracker;
  private final DefaultFilterProvider defaultFilterProvider;
  private final CaldavDao caldavDao;
  private final TagDataDao tagDataDao;
  private final TaskDao taskDao;
  private final TagService tagService;

  @Inject
  public TaskCreator(
      GCalHelper gcalHelper,
      Preferences preferences,
      TagDataDao tagDataDao,
      TaskDao taskDao,
      TagService tagService,
      TagDao tagDao,
      GoogleTaskDao googleTaskDao,
      Tracker tracker,
      DefaultFilterProvider defaultFilterProvider,
      CaldavDao caldavDao) {
    this.gcalHelper = gcalHelper;
    this.preferences = preferences;
    this.tagDataDao = tagDataDao;
    this.taskDao = taskDao;
    this.tagService = tagService;
    this.tagDao = tagDao;
    this.googleTaskDao = googleTaskDao;
    this.tracker = tracker;
    this.defaultFilterProvider = defaultFilterProvider;
    this.caldavDao = caldavDao;
  }

  private static void setDefaultReminders(Preferences preferences, Task task) {
    task.setReminderPeriod(
        DateUtilities.ONE_HOUR
            * preferences.getIntegerFromString(R.string.p_rmd_default_random_hours, 0));
    task.setReminderFlags(preferences.getDefaultReminders() | preferences.getDefaultRingMode());
  }

  public Task basicQuickAddTask(String title) {
    title = title.trim();

    Task task = createWithValues(null, title);
    taskDao.createNew(task);

    boolean gcalCreateEventEnabled =
        preferences.isDefaultCalendarSet() && task.hasDueDate(); // $NON-NLS-1$
    if (!TextUtils.isEmpty(task.getTitle())
        && gcalCreateEventEnabled
        && TextUtils.isEmpty(task.getCalendarURI())) {
      Uri calendarUri = gcalHelper.createTaskEvent(task, new ContentValues());
      task.setCalendarUri(calendarUri.toString());
    }

    createTags(task);

    if (task.hasTransitory(GoogleTask.KEY)) {
      googleTaskDao.insert(new GoogleTask(task.getId(), task.getTransitory(GoogleTask.KEY)));
    } else if (task.hasTransitory(CaldavTask.KEY)) {
      caldavDao.insert(
          new CaldavTask(task.getId(), task.getTransitory(CaldavTask.KEY), UUIDHelper.newUUID()));
    } else {
      Filter remoteList = defaultFilterProvider.getDefaultRemoteList();
      if (remoteList != null && remoteList instanceof GtasksFilter) {
        googleTaskDao.insert(
            new GoogleTask(task.getId(), ((GtasksFilter) remoteList).getRemoteId()));
      }
    }

    taskDao.save(task, null);
    return task;
  }

  /**
   * Create task from the given content values, saving it. This version doesn't need to start with a
   * base task model.
   */
  public Task createWithValues(Map<String, Object> values, String title) {
    Task task = new Task();
    task.setCreationDate(now());
    task.setModificationDate(now());
    if (title != null) {
      task.setTitle(title.trim());
    }

    task.setUuid(UUIDHelper.newUUID());

    task.setPriority(
        preferences.getIntegerFromString(R.string.p_default_importance_key, Priority.LOW));
    task.setDueDate(
        Task.createDueDate(
            preferences.getIntegerFromString(R.string.p_default_urgency_key, Task.URGENCY_NONE),
            0));
    int setting =
        preferences.getIntegerFromString(R.string.p_default_hideUntil_key, Task.HIDE_UNTIL_NONE);
    task.setHideUntil(task.createHideUntil(setting, 0));
    setDefaultReminders(preferences, task);

    ArrayList<String> tags = new ArrayList<>();

    if (values != null && values.size() > 0) {
      for (Map.Entry<String, Object> item : values.entrySet()) {
        String key = item.getKey();
        Object value = item.getValue();
        switch (key) {
          case Tag.KEY:
            tags.add((String) value);
            break;
          case GoogleTask.KEY:
            task.putTransitory(key, value);
            break;
          case CaldavTask.KEY:
            task.putTransitory(key, value);
            break;
          default:
            if (value instanceof String) {
              value = PermaSql.replacePlaceholdersForNewTask((String) value);
            }

            switch (key) {
              case "dueDate":
                task.setDueDate(Long.valueOf((String) value));
                break;
              case "importance":
                task.setPriority(Integer.valueOf((String) value));
                break;
              default:
                tracker.reportEvent(Tracking.Events.TASK_CREATION_FAILED, "Unhandled key: " + key);
                break;
            }
            break;
        }
      }
    }

    try {
      TitleParser.parse(tagService, task, tags);
    } catch (Throwable e) {
      Timber.e(e);
    }

    task.setTags(tags);

    return task;
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
