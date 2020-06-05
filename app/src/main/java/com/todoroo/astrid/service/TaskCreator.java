package com.todoroo.astrid.service;

import static com.todoroo.andlib.utility.DateUtilities.now;
import static com.todoroo.astrid.helper.UUIDHelper.newUUID;
import static org.tasks.Strings.isNullOrEmpty;

import android.content.ContentValues;
import android.net.Uri;
import androidx.annotation.Nullable;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.CaldavFilter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.utility.TitleParser;
import java.util.ArrayList;
import java.util.Map;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.CaldavDao;
import org.tasks.data.CaldavTask;
import org.tasks.data.Geofence;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.LocationDao;
import org.tasks.data.Place;
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
  private final DefaultFilterProvider defaultFilterProvider;
  private final CaldavDao caldavDao;
  private final LocationDao locationDao;
  private final TagDataDao tagDataDao;
  private final TaskDao taskDao;

  @Inject
  public TaskCreator(
      GCalHelper gcalHelper,
      Preferences preferences,
      TagDataDao tagDataDao,
      TaskDao taskDao,
      TagDao tagDao,
      GoogleTaskDao googleTaskDao,
      DefaultFilterProvider defaultFilterProvider,
      CaldavDao caldavDao,
      LocationDao locationDao) {
    this.gcalHelper = gcalHelper;
    this.preferences = preferences;
    this.tagDataDao = tagDataDao;
    this.taskDao = taskDao;
    this.tagDao = tagDao;
    this.googleTaskDao = googleTaskDao;
    this.defaultFilterProvider = defaultFilterProvider;
    this.caldavDao = caldavDao;
    this.locationDao = locationDao;
  }

  private static void setDefaultReminders(Preferences preferences, Task task) {
    task.setReminderPeriod(
        DateUtilities.ONE_HOUR
            * preferences.getIntegerFromString(R.string.p_rmd_default_random_hours, 0));
    task.setReminderFlags(preferences.getDefaultReminders() | preferences.getDefaultRingMode());
  }

  public Task basicQuickAddTask(String title) {
    title = title.trim();

    Task task = createWithValues(title);
    taskDao.createNew(task);

    boolean gcalCreateEventEnabled =
        preferences.isDefaultCalendarSet() && task.hasDueDate(); // $NON-NLS-1$
    if (!isNullOrEmpty(task.getTitle())
        && gcalCreateEventEnabled
        && isNullOrEmpty(task.getCalendarURI())) {
      Uri calendarUri = gcalHelper.createTaskEvent(task, new ContentValues());
      task.setCalendarURI(calendarUri.toString());
    }

    createTags(task);

    boolean addToTop = preferences.addTasksToTop();
    if (task.hasTransitory(GoogleTask.KEY)) {
      googleTaskDao.insertAndShift(
          new GoogleTask(task.getId(), task.getTransitory(GoogleTask.KEY)), addToTop);
    } else if (task.hasTransitory(CaldavTask.KEY)) {
      caldavDao.insert(
          task, new CaldavTask(task.getId(), task.getTransitory(CaldavTask.KEY)), addToTop);
    } else {
      Filter remoteList = defaultFilterProvider.getDefaultList();
      if (remoteList instanceof GtasksFilter) {
        googleTaskDao.insertAndShift(
            new GoogleTask(task.getId(), ((GtasksFilter) remoteList).getRemoteId()), addToTop);
      } else if (remoteList instanceof CaldavFilter) {
        caldavDao.insert(
            task, new CaldavTask(task.getId(), ((CaldavFilter) remoteList).getUuid()), addToTop);
      }
    }

    if (task.hasTransitory(Place.KEY)) {
      Place place = locationDao.getPlace(task.getTransitory(Place.KEY));
      if (place != null) {
        locationDao.insert(new Geofence(place.getUid(), preferences));
      }
    }

    taskDao.save(task, null);
    return task;
  }

  public Task createWithValues(String title) {
    return create(null, title);
  }

  public Task createWithValues(Filter filter, String title) {
    return create(filter.valuesForNewTasks, title);
  }
  /**
   * Create task from the given content values, saving it. This version doesn't need to start with a
   * base task model.
   */
  private Task create(@Nullable Map<String, Object> values, String title) {
    Task task = new Task();
    task.setCreationDate(now());
    task.setModificationDate(now());
    if (title != null) {
      task.setTitle(title.trim());
    }

    task.setUuid(newUUID());

    task.setPriority(preferences.getDefaultPriority());
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
          case CaldavTask.KEY:
          case Place.KEY:
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
                break;
            }
            break;
        }
      }
    }

    try {
      TitleParser.parse(tagDataDao, task, tags);
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
      tagDao.insert(new Tag(task, tagData));
    }
  }
}
