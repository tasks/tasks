/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import java.util.ArrayList;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.sync.GtasksTaskContainer;
import com.todoroo.astrid.sync.SyncMetadataService;
import com.todoroo.astrid.sync.SyncProviderUtilities;

/**
 * Service for working with GTasks metadata
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class GtasksMetadataService extends SyncMetadataService<GtasksTaskContainer> {

    public GtasksMetadataService() {
        super(ContextManager.getContext());
    }

    @Override
    public GtasksTaskContainer createContainerFromLocalTask(Task task,
            ArrayList<Metadata> metadata) {
        return new GtasksTaskContainer(task, metadata);
    }

    @Override
    public Criterion getLocalMatchCriteria(GtasksTaskContainer remoteTask) {
        return GtasksMetadata.ID.eq(remoteTask.gtaskMetadata.getValue(GtasksMetadata.ID));
    }

    @Override
    public Criterion getMetadataCriteria() {
        return MetadataCriteria.withKey(getMetadataKey());
    }

    @Override
    public String getMetadataKey() {
        return GtasksMetadata.METADATA_KEY;
    }

    @Override
    public SyncProviderUtilities getUtilities() {
        return GtasksUtilities.INSTANCE;
    }

}
