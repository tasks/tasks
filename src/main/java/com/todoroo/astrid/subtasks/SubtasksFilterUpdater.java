package com.todoroo.astrid.subtasks;

import android.text.TextUtils;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskListMetadataDao;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskListMetadata;

import javax.inject.Inject;

public class SubtasksFilterUpdater extends AstridOrderedListUpdater {

    public static final String ACTIVE_TASKS_ORDER = "active_tasks_order"; //$NON-NLS-1$
    public static final String TODAY_TASKS_ORDER = "today_tasks_order"; //$NON-NLS-1$

    private final TaskListMetadataDao taskListMetadataDao;

    @Inject
    public SubtasksFilterUpdater(TaskListMetadataDao taskListMetadataDao, TaskDao taskDao) {
        super(taskDao);

        this.taskListMetadataDao = taskListMetadataDao;
    }

    @Override
    protected String getSerializedTree(TaskListMetadata list) {
        if (list == null) {
            return "[]"; //$NON-NLS-1$
        }
        String order = list.getTaskIDs();
        if (TextUtils.isEmpty(order) || "null".equals(order)) //$NON-NLS-1$
        {
            order = "[]"; //$NON-NLS-1$
        }

        return order;
    }

    @Override
    protected void writeSerialization(TaskListMetadata list, String serialized, boolean shouldQueueSync) {
        if (list != null) {
            list.setTaskIDs(serialized);
            if (!shouldQueueSync) {
                list.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
            }
            taskListMetadataDao.saveExisting(list);
        }
    }

    @Override
    public void initialize(TaskListMetadata list, Filter filter) {
        super.initialize(list, filter);
        applyToFilter(filter);
    }

    @Override
    protected void applyToFilter(Filter filter) {
        String query = filter.getSqlQuery();

        query = query.replaceAll("ORDER BY .*", "");
        query = query + String.format(" ORDER BY %s, %s, %s",
                Task.DELETION_DATE, getOrderString(), Task.CREATION_DATE);
        query = query.replace(TaskDao.TaskCriteria.isVisible().toString(),
                Criterion.all.toString());

        filter.setFilterQueryOverride(query);
    }
}
