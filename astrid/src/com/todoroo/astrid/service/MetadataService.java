package com.todoroo.astrid.service;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
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
    private MetadataDao metadataDao;

    public MetadataService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    // --- service layer

    /**
     * Clean up metadata. Typically called on startup
     */
    public void cleanup() {
        TodorooCursor<Metadata> cursor = metadataDao.fetchDangling(Metadata.ID);
        try {
            if(cursor.getCount() == 0)
                return;

            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long id = cursor.getLong(0);
                metadataDao.delete(id);
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Query underlying database
     * @param query
     * @return
     */
    public TodorooCursor<Metadata> query(Query query) {
        return metadataDao.query(query);
    }

    /**
     * Delete from metadata table where rows match a certain condition
     * @param where
     */
    public void deleteWhere(Criterion where) {
        metadataDao.deleteWhere(where);
    }

    /**
     * Save a single piece of metadata
     * @param metadata
     */
    public void save(Metadata metadata) {
        metadataDao.persist(metadata);
    }
}
