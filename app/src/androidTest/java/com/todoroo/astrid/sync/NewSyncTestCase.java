package com.todoroo.astrid.sync;

import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.Task.Priority;
import javax.inject.Inject;
import org.tasks.data.TagData;
import org.tasks.data.TagDataDao;
import org.tasks.injection.InjectingTestCase;
import org.tasks.injection.TestComponent;

public class NewSyncTestCase extends InjectingTestCase {

  private static final String SYNC_TASK_TITLE = "new title";
  private static final int SYNC_TASK_IMPORTANCE = Priority.MEDIUM;
  @Inject TaskDao taskDao;
  @Inject TagDataDao tagDataDao;

  private Task createTask(String title) {
    Task task = new Task();
    task.setTitle(title);
    task.setPriority(SYNC_TASK_IMPORTANCE);

    taskDao.createNew(task);
    return task;
  }

  Task createTask() {
    return createTask(SYNC_TASK_TITLE);
  }

  private TagData createTagData(String name) {
    TagData tag = new TagData();
    tag.setName(name);

    tagDataDao.createNew(tag);
    return tag;
  }

  TagData createTagData() {
    return createTagData("new tag");
  }

  @Override
  protected void inject(TestComponent component) {
    component.inject(this);
  }
}
