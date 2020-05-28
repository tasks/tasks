package com.todoroo.astrid.adapter;

import static org.tasks.Strings.isNullOrEmpty;

import android.content.Context;
import com.todoroo.astrid.api.CaldavFilter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.api.TagFilter;
import com.todoroo.astrid.core.BuiltInFilterExposer;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.subtasks.SubtasksFilterUpdater;
import com.todoroo.astrid.subtasks.SubtasksHelper;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.data.CaldavDao;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.TagData;
import org.tasks.data.TaskListMetadata;
import org.tasks.data.TaskListMetadataDao;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;

public class TaskAdapterProvider {

  private final Context context;
  private final Preferences preferences;
  private final TaskListMetadataDao taskListMetadataDao;
  private final TaskDao taskDao;
  private final GoogleTaskDao googleTaskDao;
  private final CaldavDao caldavDao;
  private final SubtasksHelper subtasksHelper;
  private final LocalBroadcastManager localBroadcastManager;

  @Inject
  public TaskAdapterProvider(
      @ForApplication Context context,
      Preferences preferences,
      TaskListMetadataDao taskListMetadataDao,
      TaskDao taskDao,
      GoogleTaskDao googleTaskDao,
      CaldavDao caldavDao,
      SubtasksHelper subtasksHelper,
      LocalBroadcastManager localBroadcastManager) {
    this.context = context;
    this.preferences = preferences;
    this.taskListMetadataDao = taskListMetadataDao;
    this.taskDao = taskDao;
    this.googleTaskDao = googleTaskDao;
    this.caldavDao = caldavDao;
    this.subtasksHelper = subtasksHelper;
    this.localBroadcastManager = localBroadcastManager;
  }

  public TaskAdapter createTaskAdapter(Filter filter) {
    if (preferences.isManualSort()) {
      if (filter instanceof TagFilter) {
        return createManualTagTaskAdapter((TagFilter) filter);
      } else if (filter instanceof GtasksFilter) {
        return new GoogleTaskManualSortAdapter(googleTaskDao, caldavDao, taskDao, localBroadcastManager);
      } else if (filter instanceof CaldavFilter) {
        return new CaldavManualSortTaskAdapter(googleTaskDao, caldavDao, taskDao, localBroadcastManager);
      } else if (subtasksHelper.shouldUseSubtasksFragmentForFilter(filter)) {
        return createManualFilterTaskAdapter(filter);
      }
    }
    return new TaskAdapter(preferences.addTasksToTop(), googleTaskDao, caldavDao, taskDao, localBroadcastManager);
  }

  private TaskAdapter createManualTagTaskAdapter(TagFilter filter) {
    TagData tagData = filter.getTagData();
    String tdId = tagData.getRemoteId();
    TaskListMetadata list = taskListMetadataDao.fetchByTagOrFilter(tagData.getRemoteId());
    if (list == null && !Task.isUuidEmpty(tdId)) {
      list = new TaskListMetadata();
      list.setTagUuid(tdId);
      taskListMetadataDao.createNew(list);
    }
    SubtasksFilterUpdater updater = new SubtasksFilterUpdater(taskListMetadataDao, taskDao);
    updater.initialize(list, filter);
    return new AstridTaskAdapter(list, filter, updater, googleTaskDao, caldavDao, taskDao, localBroadcastManager);
  }

  private TaskAdapter createManualFilterTaskAdapter(Filter filter) {
    String filterId = null;
    String prefId = null;
    if (BuiltInFilterExposer.isInbox(context, filter)) {
      filterId = TaskListMetadata.FILTER_ID_ALL;
      prefId = SubtasksFilterUpdater.ACTIVE_TASKS_ORDER;
    } else if (BuiltInFilterExposer.isTodayFilter(context, filter)) {
      filterId = TaskListMetadata.FILTER_ID_TODAY;
      prefId = SubtasksFilterUpdater.TODAY_TASKS_ORDER;
    }
    if (isNullOrEmpty(filterId)) {
      return null;
    }
    TaskListMetadata list = taskListMetadataDao.fetchByTagOrFilter(filterId);
    if (list == null) {
      String defaultOrder = preferences.getStringValue(prefId);
      if (isNullOrEmpty(defaultOrder)) {
        defaultOrder = "[]"; // $NON-NLS-1$
      }
      defaultOrder = SubtasksHelper.convertTreeToRemoteIds(taskDao, defaultOrder);
      list = new TaskListMetadata();
      list.setFilter(filterId);
      list.setTaskIds(defaultOrder);
      taskListMetadataDao.createNew(list);
    }
    SubtasksFilterUpdater updater = new SubtasksFilterUpdater(taskListMetadataDao, taskDao);
    updater.initialize(list, filter);
    return new AstridTaskAdapter(list, filter, updater, googleTaskDao, caldavDao, taskDao, localBroadcastManager);
  }
}
