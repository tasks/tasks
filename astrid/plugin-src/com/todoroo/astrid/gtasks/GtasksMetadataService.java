/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TaskService;

/**
 * Service for working with GTasks metadata
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class GtasksMetadataService {

    // --- constants

    /** Utility for joining tasks with metadata */
    public static final Join METADATA_JOIN = Join.left(Metadata.TABLE, Task.ID.eq(Metadata.TASK));

    // --- instance variables

    @Autowired
    private TaskService taskService;

    @Autowired
    private MetadataService metadataService;

    public GtasksMetadataService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    // --- task and metadata methods

    /**
     * Clears metadata information. Used when user logs out of service
     */
    public void clearMetadata() {
        metadataService.deleteWhere(Metadata.KEY.eq(GtasksMetadata.METADATA_KEY));
        PluginServices.getTaskService().clearDetails(Task.ID.in(Query.select(Metadata.TASK).from(Metadata.TABLE).
                where(MetadataCriteria.withKey(GtasksMetadata.METADATA_KEY))));
    }

    /**
     * Gets tasks that were created since last sync
     * @param properties
     * @return
     */
    public TodorooCursor<Task> getLocallyCreated(Property<?>[] properties) {
        return
            taskService.query(Query.select(properties).join(GtasksMetadataService.METADATA_JOIN).where(Criterion.and(
                    Criterion.not(Task.ID.in(Query.select(Metadata.TASK).from(Metadata.TABLE).
                            where(Criterion.and(MetadataCriteria.withKey(GtasksMetadata.METADATA_KEY), GtasksMetadata.ID.gt(0))))),
                    TaskCriteria.isActive())).groupBy(Task.ID));
    }

    /**
     * Gets tasks that were modified since last sync
     * @param properties
     * @return null if never sync'd
     */
    public TodorooCursor<Task> getLocallyUpdated(Property<?>[] properties) {
        long lastSyncDate = GtasksUtilities.INSTANCE.getLastSyncDate();
        if(lastSyncDate == 0)
            return taskService.query(Query.select(Task.ID).where(Criterion.none));
        return
            taskService.query(Query.select(properties).join(GtasksMetadataService.METADATA_JOIN).
                    where(Criterion.and(MetadataCriteria.withKey(GtasksMetadata.METADATA_KEY),
                            Task.MODIFICATION_DATE.gt(lastSyncDate))).groupBy(Task.ID));
    }

    /**
     * Reads metadata out of a task
     * @return null if no metadata found
     */
    public Metadata getTaskMetadata(long taskId) {
        TodorooCursor<Metadata> cursor = metadataService.query(Query.select(
                Metadata.PROPERTIES).where(
                MetadataCriteria.byTaskAndwithKey(taskId, GtasksMetadata.METADATA_KEY)));
        try {
            if(cursor.getCount() == 0)
                return null;
            cursor.moveToFirst();
            return new Metadata(cursor);
        } finally {
            cursor.close();
        }
    }
}
