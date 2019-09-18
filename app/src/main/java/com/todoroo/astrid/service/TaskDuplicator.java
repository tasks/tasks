package com.todoroo.astrid.service;

import static com.todoroo.andlib.utility.DateUtilities.now;

import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.data.CaldavDao;
import org.tasks.data.CaldavTask;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.Tag;
import org.tasks.data.TagDao;
import org.tasks.data.TagData;
import org.tasks.data.TagDataDao;
import org.tasks.preferences.Preferences;

public class TaskDuplicator {

  private final GCalHelper gcalHelper;
  private final TaskDao taskDao;
  private final TagDao tagDao;
  private final TagDataDao tagDataDao;
  private final GoogleTaskDao googleTaskDao;
  private final CaldavDao caldavDao;
  private final Preferences preferences;
  private final LocalBroadcastManager localBroadcastManager;

  @Inject
  public TaskDuplicator(
      GCalHelper gcalHelper,
      TaskDao taskDao,
      LocalBroadcastManager localBroadcastManager,
      TagDao tagDao,
      TagDataDao tagDataDao,
      GoogleTaskDao googleTaskDao,
      CaldavDao caldavDao,
      Preferences preferences) {
    this.gcalHelper = gcalHelper;
    this.taskDao = taskDao;
    this.localBroadcastManager = localBroadcastManager;
    this.tagDao = tagDao;
    this.tagDataDao = tagDataDao;
    this.googleTaskDao = googleTaskDao;
    this.caldavDao = caldavDao;
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
    clone.setCalendarUri("");
    clone.setUuid(null);
    clone.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
    clone.putTransitory(TaskDao.TRANS_SUPPRESS_REFRESH, true);
    taskDao.createNew(clone);

    for (TagData tagData : tagDataDao.getTagDataForTask(originalId)) {
      tagDao.insert(new Tag(clone, tagData));
    }

    GoogleTask googleTask = googleTaskDao.getByTaskId(originalId);
    if (googleTask != null) {
      googleTaskDao.insertAndShift(
          new GoogleTask(clone.getId(), googleTask.getListId()), preferences.addGoogleTasksToTop());
    }

    CaldavTask caldavTask = caldavDao.getTask(originalId);
    if (caldavTask != null) {
      caldavDao.insert(new CaldavTask(clone.getId(), caldavTask.getCalendar()));
    }

    gcalHelper.createTaskEventIfEnabled(clone);

    taskDao.save(clone, null); // TODO: delete me

    return clone;
  }
}
