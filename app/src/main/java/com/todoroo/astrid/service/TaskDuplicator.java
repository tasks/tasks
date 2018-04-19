package com.todoroo.astrid.service;

import static com.google.common.collect.Lists.transform;
import static com.todoroo.andlib.utility.DateUtilities.now;

import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.helper.UUIDHelper;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.Tag;
import org.tasks.data.TagDao;

public class TaskDuplicator {

  private final GCalHelper gcalHelper;
  private final TaskDao taskDao;
  private final TagDao tagDao;
  private final GoogleTaskDao googleTaskDao;
  private final LocalBroadcastManager localBroadcastManager;

  @Inject
  public TaskDuplicator(
      GCalHelper gcalHelper,
      TaskDao taskDao,
      LocalBroadcastManager localBroadcastManager,
      TagDao tagDao,
      GoogleTaskDao googleTaskDao) {
    this.gcalHelper = gcalHelper;
    this.taskDao = taskDao;
    this.localBroadcastManager = localBroadcastManager;
    this.tagDao = tagDao;
    this.googleTaskDao = googleTaskDao;
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
    clone.setCreationDate(now());
    clone.setModificationDate(now());
    clone.setCompletionDate(0L);
    clone.setCalendarUri("");
    clone.setUuid(UUIDHelper.newUUID());

    List<Tag> tags = tagDao.getTagsForTask(clone.getId());
    GoogleTask googleTask = googleTaskDao.getByTaskId(clone.getId());
    if (googleTask != null) {
      clone.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
    }
    clone.putTransitory(TaskDao.TRANS_SUPPRESS_REFRESH, true);

    taskDao.createNew(clone);

    tagDao.insert(
        transform(
            tags, tag -> new Tag(clone.getId(), clone.getUuid(), tag.getName(), tag.getTagUid())));

    if (googleTask != null) {
      googleTaskDao.insert(new GoogleTask(clone.getId(), googleTask.getListId()));
    }

    gcalHelper.createTaskEventIfEnabled(clone);

    taskDao.save(clone, null); // TODO: delete me

    return clone;
  }
}
