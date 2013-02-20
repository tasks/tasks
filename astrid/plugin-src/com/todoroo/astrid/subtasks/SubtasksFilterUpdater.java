package com.todoroo.astrid.subtasks;

import android.text.TextUtils;

import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.AstridNewSyncMigrator;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.TaskListMetadata;

public class SubtasksFilterUpdater extends SubtasksUpdater<TaskListMetadata> {

    private boolean migrationOccurred;

    public SubtasksFilterUpdater() {
        migrationOccurred = Preferences.getBoolean(AstridNewSyncMigrator.PREF_SYNC_MIGRATION, false);
    }

    @Override
    protected String getSerializedTree(TaskListMetadata list, Filter filter) {
        if (list == null)
            return "[]"; //$NON-NLS-1$
        String order = list.getValue(TaskListMetadata.TASK_IDS);
        if (TextUtils.isEmpty(order) || "null".equals(order)) //$NON-NLS-1$
            order = "[]"; //$NON-NLS-1$

        return order;
    }

    @Override
    protected void writeSerialization(TaskListMetadata list, String serialized, boolean shouldQueueSync) {
        if (list != null && syncMigrationOccurred()) {
            list.setValue(TaskListMetadata.TASK_IDS, serialized);
            if (!shouldQueueSync)
                list.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
            taskListMetadataDao.saveExisting(list);
        }
    }

    private boolean syncMigrationOccurred() {
        if (migrationOccurred)
            return true;
        migrationOccurred = Preferences.getBoolean(AstridNewSyncMigrator.PREF_SYNC_MIGRATION, false);
        return migrationOccurred;
    }

}
