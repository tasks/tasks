/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.weloveastrid.rmilk.data;

import java.util.ArrayList;

import org.weloveastrid.rmilk.MilkDependencyInjector;
import org.weloveastrid.rmilk.MilkUtilities;
import org.weloveastrid.rmilk.sync.MilkTaskContainer;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.MetadataApiDao.MetadataCriteria;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.notes.NoteMetadata;
import com.todoroo.astrid.sync.SyncMetadataService;
import com.todoroo.astrid.sync.SyncProviderUtilities;

public final class MilkMetadataService extends SyncMetadataService<MilkTaskContainer>{

    static {
        MilkDependencyInjector.initialize();
    }

    public MilkMetadataService() {
        super(ContextManager.getContext());
    }

    @Override
    public MilkTaskContainer createContainerFromLocalTask(Task task,
            ArrayList<Metadata> metadata) {
        return new MilkTaskContainer(task, metadata);
    }

    @Override
    public Criterion getLocalMatchCriteria(MilkTaskContainer remoteTask) {
        return Criterion.and(MilkTaskFields.TASK_SERIES_ID.eq(remoteTask.taskSeriesId),
                        MilkTaskFields.TASK_ID.eq(remoteTask.taskId));
    }

    @Override
    public Criterion getMetadataCriteria() {
        return Criterion.or(MetadataCriteria.withKey(TAG_KEY),
                MetadataCriteria.withKey(MilkTaskFields.METADATA_KEY),
                Criterion.and(MetadataCriteria.withKey(NoteMetadata.METADATA_KEY),
                        NoteMetadata.EXT_PROVIDER.eq(MilkNoteHelper.PROVIDER)));
    }

    @Override
    public String getMetadataKey() {
        return MilkTaskFields.METADATA_KEY;
    }

    @Override
    public SyncProviderUtilities getUtilities() {
        return MilkUtilities.INSTANCE;
    }

    /**
     * Reads task notes out of a task
     */
    public TodorooCursor<Metadata> getTaskNotesCursor(long taskId) {
        TodorooCursor<Metadata> cursor = metadataDao.query(Query.select(Metadata.PROPERTIES).
                where(MetadataCriteria.byTaskAndwithKey(taskId, NoteMetadata.METADATA_KEY)));
        return cursor;
    }

    @Override
    public Criterion getMetadataWithRemoteId() {
        return MilkTaskFields.TASK_ID.gt(0);
    }

}
