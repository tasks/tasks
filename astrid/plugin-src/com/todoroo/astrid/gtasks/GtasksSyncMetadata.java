package com.todoroo.astrid.gtasks;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.data.Metadata;

/**
 * Metadata entries for synchronizing a GTasks Task
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class GtasksSyncMetadata {

    /** metadata key */
    public static final String METADATA_KEY = "gtasks-sync"; //$NON-NLS-1$

    /** last sync date*/
    public static final LongProperty LAST_SYNC = new LongProperty(Metadata.TABLE,
            Metadata.VALUE1.name);

    /**
     * Helper to set value
     * @param metadataDao
     * @param id
     * @param property
     * @param now
     */
    public static <T> void set(MetadataDao metadataDao, long taskId,
            Property<T> property, T value) {
        TodorooCursor<Metadata> cursor = metadataDao.query(Query.select(Metadata.PROPERTIES).
                where(MetadataCriteria.byTaskAndwithKey(taskId, METADATA_KEY)));
        Metadata metadata = new Metadata();
        if(cursor.getCount() == 0) {
            metadata.setValue(Metadata.TASK, taskId);
            metadata.setValue(Metadata.KEY, METADATA_KEY);
        } else {
            cursor.moveToFirst();
            metadata.readFromCursor(cursor);
        }

        metadata.setValue(property, value);
        metadataDao.persist(metadata);
    }

}
