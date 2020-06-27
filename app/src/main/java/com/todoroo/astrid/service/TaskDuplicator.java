package com.todoroo.astrid.service;

import static com.google.common.collect.Lists.transform;
import static com.todoroo.andlib.utility.DateUtilities.now;
import static com.todoroo.astrid.data.Task.NO_UUID;

import com.todoroo.astrid.dao.TaskDaoBlocking;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.data.Alarm;
import org.tasks.data.AlarmDaoBlocking;
import org.tasks.data.CaldavDaoBlocking;
import org.tasks.data.CaldavTask;
import org.tasks.data.Geofence;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDaoBlocking;
import org.tasks.data.LocationDaoBlocking;
import org.tasks.data.Tag;
import org.tasks.data.TagDaoBlocking;
import org.tasks.data.TagData;
import org.tasks.data.TagDataDaoBlocking;
import org.tasks.preferences.Preferences;

public class TaskDuplicator {

  private final GCalHelper gcalHelper;
  private final TaskDaoBlocking taskDao;
  private final TagDaoBlocking tagDao;
  private final TagDataDaoBlocking tagDataDao;
  private final GoogleTaskDaoBlocking googleTaskDao;
  private final CaldavDaoBlocking caldavDao;
  private final LocationDaoBlocking locationDao;
  private final AlarmDaoBlocking alarmDao;
  private final Preferences preferences;
  private final LocalBroadcastManager localBroadcastManager;

  @Inject
  public TaskDuplicator(
      GCalHelper gcalHelper,
      TaskDaoBlocking taskDao,
      LocalBroadcastManager localBroadcastManager,
      TagDaoBlocking tagDao,
      TagDataDaoBlocking tagDataDao,
      GoogleTaskDaoBlocking googleTaskDao,
      CaldavDaoBlocking caldavDao,
      LocationDaoBlocking locationDao,
      AlarmDaoBlocking alarmDao,
      Preferences preferences) {
    this.gcalHelper = gcalHelper;
    this.taskDao = taskDao;
    this.localBroadcastManager = localBroadcastManager;
    this.tagDao = tagDao;
    this.tagDataDao = tagDataDao;
    this.googleTaskDao = googleTaskDao;
    this.caldavDao = caldavDao;
    this.locationDao = locationDao;
    this.alarmDao = alarmDao;
    this.preferences = preferences;
  }

  public List<Task> duplicate(List<Long> taskIds) {
    List<Task> result = new ArrayList<>();
    for (Task task : taskDao.fetch(taskIds)) {
      result.add(clone(task));
    }
    localBroadcastManager.broadcastRefresh();
    return result;
  }

  private Task clone(Task clone) {
    long originalId = clone.getId();
    clone.setCreationDate(now());
    clone.setModificationDate(now());
    clone.setCompletionDate(0L);
    clone.setCalendarURI("");
    clone.setUuid(NO_UUID);
    clone.suppressSync();
    clone.suppressRefresh();
    taskDao.createNew(clone);

    List<TagData> tags = tagDataDao.getTagDataForTask(originalId);
    if (!tags.isEmpty()) {
      tagDao.insert(transform(tags, td -> new Tag(clone, td)));
    }

    GoogleTask googleTask = googleTaskDao.getByTaskId(originalId);
    boolean addToTop = preferences.addTasksToTop();
    if (googleTask != null) {
      googleTaskDao.insertAndShift(new GoogleTask(clone.getId(), googleTask.getListId()), addToTop);
    }

    CaldavTask caldavTask = caldavDao.getTask(originalId);
    if (caldavTask != null) {
      caldavDao.insert(clone, new CaldavTask(clone.getId(), caldavTask.getCalendar()), addToTop);
    }

    for (Geofence g : locationDao.getGeofencesForTask(originalId)) {
      locationDao.insert(
          new Geofence(clone.getId(), g.getPlace(), g.isArrival(), g.isDeparture(), g.getRadius()));
    }

    List<Alarm> alarms = alarmDao.getAlarms(originalId);
    if (!alarms.isEmpty()) {
      alarmDao.insert(transform(alarms, a -> new Alarm(clone.getId(), a.getTime())));
    }

    gcalHelper.createTaskEventIfEnabled(clone);

    taskDao.save(clone, null); // TODO: delete me

    return clone;
  }
}
