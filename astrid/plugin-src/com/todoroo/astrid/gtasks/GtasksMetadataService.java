/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import android.text.TextUtils;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
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

    @Autowired private GtasksPreferenceService gtasksPreferenceService;

    public GtasksMetadataService() {
        super(ContextManager.getContext());
        DependencyInjectionService.getInstance().inject(this);
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
        return gtasksPreferenceService;
    }

    @Override
    public Criterion getMetadataWithRemoteId() {
        return GtasksMetadata.ID.neq(""); //$NON-NLS-1$
    }

    // --- list iterating helpers


    public interface ListIterator {
        public void processTask(long taskId, Metadata metadata);
    }

    public void iterateThroughList(StoreObject list, ListIterator iterator) {
        String listId = list.getValue(GtasksList.REMOTE_ID);
        iterateThroughList(listId, iterator, 0, false);
    }

    @SuppressWarnings("nls")
    public void iterateThroughList(String listId, ListIterator iterator, long startAtOrder, boolean reverse) {
        Field orderField = Functions.cast(GtasksMetadata.ORDER, "LONG");
        Order order = reverse ? Order.desc(orderField) : Order.asc(orderField);
        Criterion startAtCriterion = reverse ?  Functions.cast(GtasksMetadata.ORDER, "LONG").lt(startAtOrder) :
            Functions.cast(GtasksMetadata.ORDER, "LONG").gt(startAtOrder - 1);

        Query query = Query.select(Metadata.PROPERTIES).where(Criterion.and(
                        MetadataCriteria.withKey(GtasksMetadata.METADATA_KEY),
                        GtasksMetadata.LIST_ID.eq(listId),
                        startAtCriterion)).
                        orderBy(order);
        TodorooCursor<Metadata> cursor = PluginServices.getMetadataService().query(query);
        try {
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long taskId = cursor.get(Metadata.TASK);
                Metadata metadata = getTaskMetadata(taskId);
                if(metadata == null)
                    continue;
                iterator.processTask(taskId, metadata);
            }

        } finally {
            cursor.close();
        }
    }

    /**
     * Gets the remote id string of the parent task
     * @param gtasksMetadata
     * @return
     */
    public String getRemoteParentId(Metadata gtasksMetadata) {
        String parent = null;
        if (gtasksMetadata.containsNonNullValue(GtasksMetadata.PARENT_TASK)) {
            long parentId = gtasksMetadata.getValue(GtasksMetadata.PARENT_TASK);
            Metadata parentMetadata = getTaskMetadata(parentId);
            if (parentMetadata != null && parentMetadata.containsNonNullValue(GtasksMetadata.ID)) {
                parent = parentMetadata.getValue(GtasksMetadata.ID);
                if (TextUtils.isEmpty(parent)) {
                    parent = null;
                }
            }
        }
        return parent;
    }

    /**
     * Gets the remote id string of the previous sibling task
     * @param listId
     * @param gtasksMetadata
     * @return
     */
    public String getRemoteSiblingId(String listId, Metadata gtasksMetadata) {
        final AtomicInteger indentToMatch = new AtomicInteger(gtasksMetadata.getValue(GtasksMetadata.INDENT).intValue());
        final AtomicLong parentToMatch = new AtomicLong(gtasksMetadata.getValue(GtasksMetadata.PARENT_TASK).longValue());
        final AtomicReference<String> sibling = new AtomicReference<String>();

        ListIterator iterator = new ListIterator() {
            @Override
            public void processTask(long taskId, Metadata metadata) {
                Task t = taskDao.fetch(taskId, Task.TITLE, Task.DELETION_DATE);
                if (t.isDeleted()) return;
                int currIndent = metadata.getValue(GtasksMetadata.INDENT).intValue();
                long currParent = metadata.getValue(GtasksMetadata.PARENT_TASK);

                if (currIndent == indentToMatch.get() && currParent == parentToMatch.get()) {
                    if (sibling.get() == null) {
                        sibling.set(metadata.getValue(GtasksMetadata.ID));
                    }
                }
            }
        };

        this.iterateThroughList(listId, iterator, gtasksMetadata.getValue(GtasksMetadata.ORDER), true);
        return sibling.get();
    }

}
