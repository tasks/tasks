package com.todoroo.astrid.subtasks;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TaskService;

public class SubtasksUpdater extends OrderedListUpdater<String> {

    private static final String METADATA_ID = "mdi"; //$NON-NLS-1$

    @Autowired MetadataService metadataService;
    @Autowired TaskService taskService;

    @Override
    protected IntegerProperty indentProperty() {
        return SubtasksMetadata.INDENT;
    }

    @Override
    protected LongProperty orderProperty() {
        return SubtasksMetadata.ORDER;
    }
    @Override
    protected LongProperty parentProperty() {
        return null;
    }

    @Override
    protected Metadata getTaskMetadata(String list, long taskId) {
        TodorooCursor<Metadata> cursor = metadataService.query(Query.select(Metadata.PROPERTIES).where(
                Criterion.and(
                        Metadata.TASK.eq(taskId),
                        Metadata.KEY.eq(SubtasksMetadata.METADATA_KEY),
                        SubtasksMetadata.TAG.eq(list))));
        try {
            cursor.moveToFirst();
            if(cursor.isAfterLast())
                return null;
            return new Metadata(cursor);
        } finally {
            cursor.close();
        }
    }

    @Override
    protected Metadata createEmptyMetadata(String list, long taskId) {
        Metadata m = new Metadata();
        m.setValue(Metadata.TASK, taskId);
        m.setValue(Metadata.KEY, SubtasksMetadata.METADATA_KEY);
        m.setValue(SubtasksMetadata.TAG, list);
        return m;
    }

    @Override
    protected void iterateThroughList(Filter filter, String list, OrderedListIterator iterator) {
        TodorooCursor<Task> cursor = taskService.query(Query.select(Task.ID,
                Metadata.ID.as(METADATA_ID), Metadata.TASK, Metadata.KEY, SubtasksMetadata.INDENT,
                SubtasksMetadata.ORDER).withQueryTemplate(filter.sqlQuery));
        TodorooCursor<Metadata> metadataCursor = new TodorooCursor<Metadata>(cursor.getCursor(),
                cursor.getProperties());
        Metadata metadata = new Metadata();
        try {
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                metadata.readFromCursor(metadataCursor);
                metadata.setId(cursor.getLong(cursor.getColumnIndex(METADATA_ID)));
                iterator.processTask(cursor.get(Task.ID), metadata);
            }
        } finally {
            cursor.close();
        }
    }

    @SuppressWarnings("nls")
    public void applySubtasksToFilter(Filter filter, String tagName) {
        String query = filter.sqlQuery;

        if(tagName == null)
            tagName = SubtasksMetadata.LIST_ACTIVE_TASKS;
        String subtaskJoin = String.format("LEFT JOIN %s ON (%s = %s AND %s = '%s' AND %s = '%s') ",
                Metadata.TABLE, Task.ID, Metadata.TASK,
                Metadata.KEY, SubtasksMetadata.METADATA_KEY,
                SubtasksMetadata.TAG, tagName);

        if(!query.contains(subtaskJoin)) {
            query = subtaskJoin + query;
            query = query.replaceAll("ORDER BY .*", "");
            query = query + String.format(" ORDER BY %s, %s, IFNULL(CAST(%s AS LONG), %s)",
                    Task.DELETION_DATE, Task.COMPLETION_DATE,
                    SubtasksMetadata.ORDER, Task.CREATION_DATE);
            query = query.replace(TaskCriteria.isVisible().toString(),
                    Criterion.all.toString());

            filter.sqlQuery = query;
        }
    }

    public void sanitizeTaskList(Filter filter, String list) {
        final AtomicInteger previousIndent = new AtomicInteger(-1);
        final AtomicLong previousOrder = new AtomicLong(-1);
        final HashSet<Long> taskIds = new HashSet<Long>();

        iterateThroughList(filter, list, new OrderedListIterator() {
            @Override
            public void processTask(long taskId, Metadata metadata) {
                if(!metadata.isSaved())
                    return;

                if(taskIds.contains(taskId)) {
                    metadataService.delete(metadata);
                    return;
                }

                long order = metadata.getValue(SubtasksMetadata.ORDER);
                if(order <= previousOrder.get()) // bad
                    order = previousOrder.get() + 1;

                int indent = metadata.getValue(SubtasksMetadata.INDENT);
                if(indent < 0 || indent > previousIndent.get() + 1) // bad
                    indent = Math.max(0, previousIndent.get());

                metadata.setValue(SubtasksMetadata.ORDER, order);
                metadata.setValue(SubtasksMetadata.INDENT, indent);
                saveAndUpdateModifiedDate(metadata, taskId);

                previousIndent.set(indent);
                previousOrder.set(order);
                taskIds.add(taskId);
            }
        });
    }
}
