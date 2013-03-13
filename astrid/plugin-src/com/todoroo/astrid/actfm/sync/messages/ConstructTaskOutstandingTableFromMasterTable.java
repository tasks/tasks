package com.todoroo.astrid.actfm.sync.messages;

import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.OutstandingEntryDao;
import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskOutstanding;
import com.todoroo.astrid.tags.TaskToTagMetadata;

public class ConstructTaskOutstandingTableFromMasterTable extends ConstructOutstandingTableFromMasterTable<Task, TaskOutstanding> {

    private final MetadataDao metadataDao;

    public ConstructTaskOutstandingTableFromMasterTable(String table, RemoteModelDao<Task> dao, OutstandingEntryDao<TaskOutstanding> outstandingDao, MetadataDao metadataDao, LongProperty createdAtProperty) {
        super(table, dao, outstandingDao, createdAtProperty);
        this.metadataDao = metadataDao;
    }

    @Override
    protected void extras(long itemId, long createdAt) {
        super.extras(itemId, createdAt);
        TodorooCursor<Metadata> tagMetadata = metadataDao.query(Query.select(Metadata.PROPERTIES)
                .where(Criterion.and(MetadataCriteria.byTaskAndwithKey(itemId, TaskToTagMetadata.KEY), Metadata.DELETION_DATE.eq(0))));
        Metadata m = new Metadata();
        try {
            for (tagMetadata.moveToFirst(); !tagMetadata.isAfterLast(); tagMetadata.moveToNext()) {
                m.clear();
                m.readFromCursor(tagMetadata);

                if (m.containsNonNullValue(TaskToTagMetadata.TAG_UUID)) {
                    TaskOutstanding oe = new TaskOutstanding();
                    oe.setValue(TaskOutstanding.ENTITY_ID_PROPERTY, itemId);
                    oe.setValue(TaskOutstanding.COLUMN_STRING, NameMaps.TAG_ADDED_COLUMN);
                    oe.setValue(TaskOutstanding.VALUE_STRING, m.getValue(TaskToTagMetadata.TAG_UUID));
                    oe.setValue(TaskOutstanding.CREATED_AT, createdAt);
                    outstandingDao.createNew(oe);
                }
            }
        } finally {
            tagMetadata.close();
        }
    }

}
