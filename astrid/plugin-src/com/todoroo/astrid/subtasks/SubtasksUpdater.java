package com.todoroo.astrid.subtasks;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TaskService;

public class SubtasksUpdater extends OrderedListUpdater<Long> {

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
    protected Metadata getTaskMetadata(Long list, long taskId) {
        TodorooCursor<Metadata> cursor = metadataService.query(Query.select(Metadata.PROPERTIES).where(
                Criterion.and(
                        Metadata.TASK.eq(taskId),
                        Metadata.KEY.eq(SubtasksMetadata.METADATA_KEY),
                        SubtasksMetadata.LIST_ID.eq(list))));
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
    protected Metadata createEmptyMetadata(Long list, long taskId) {
        Metadata m = new Metadata();
        m.setValue(Metadata.TASK, taskId);
        m.setValue(Metadata.KEY, SubtasksMetadata.METADATA_KEY);
        m.setValue(SubtasksMetadata.LIST_ID, list);
        return m;
    }

    @Override
    protected void iterateThroughList(Filter filter, Long list, OrderedListIterator iterator) {
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
    public void applySubtasksToFilter(Filter filter) {
        String query = filter.sqlQuery;

        String subtaskJoin = String.format("LEFT JOIN %s ON (%s = %s AND %s = '%s') ",
                Metadata.TABLE, Task.ID, Metadata.TASK,
                Metadata.KEY, SubtasksMetadata.METADATA_KEY);
        if(!query.contains(subtaskJoin)) {
            query = subtaskJoin + query;
            query = query.replaceAll("ORDER BY .*", "");
            query = query + String.format(" ORDER BY CAST(%s AS LONG) ASC, %s ASC",
                    SubtasksMetadata.ORDER, Task.ID);

            filter.sqlQuery = query;
        }
    }
}
