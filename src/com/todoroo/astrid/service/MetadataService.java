package com.todoroo.astrid.service;

import com.todoroo.android.data.Property;
import com.todoroo.android.data.TodorooCursor;
import com.todoroo.android.data.Property.IntegerProperty;
import com.todoroo.android.service.Autowired;
import com.todoroo.android.service.DependencyInjectionService;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.model.Metadata;

/**
 * Service layer for {@link Metadata}-centered activities.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class MetadataService {

    @Autowired
    private Database database;

    @Autowired
    private MetadataDao metadataDao;

    public MetadataService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    // --- property list

    /**
     * @return property list containing just task id's
     */
    public static Property<?>[] idProperty() {
        return new Property<?>[] { Metadata.ID };
    }

    /**
     * @return property list containing just task id's
     */
    public static Property<?>[] valueProperty() {
        return new Property<?>[] { Metadata.VALUE };
    }

    // --- service layer

    /**
     * Clean up metadata. Typically called on startup
     */
    public void cleanup() {
        TodorooCursor<Metadata> cursor = metadataDao.fetchDangling(database, idProperty());
        try {
            if(cursor.getCount() == 0)
                return;

            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long id = cursor.getLong(0);
                metadataDao.delete(database, id);
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Retrieve count of all metadata grouped by value
     * @param where SQL where clause
     * @param onlyCountsGreaterThanZero only include items where count > 0
     */
    public TodorooCursor<Metadata> fetchWithCount(String where, String sort,
            boolean onlyCountsGreaterThanZero) {
        IntegerProperty count = Property.countProperty();
        String having = null;
        if(onlyCountsGreaterThanZero)
            having = count.name + " > 0"; //$NON-NLS-1$
        TodorooCursor<Metadata> cursor = metadataDao.fetch(database, new Property<?>[] {
                Metadata.VALUE, count },
                where,
                Metadata.VALUE.name, having, sort, null);
        return cursor;
    }

    /**
     * Delete from metadata table where rows match a certain condition
     * @param where
     */
    public void deleteWhere(String where) {
        metadataDao.deleteWhere(database, where);
    }
}
