package org.tasks.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.todoroo.astrid.gtasks.GtasksList;

import java.util.List;

@Dao
public abstract class StoreObjectDao {

    @Query("SELECT * FROM store WHERE _id = :id LIMIT 1")
    abstract StoreObject getById(long id);

    public GtasksList getGtasksList(long id) {
        StoreObject result = getById(id);
        if (result == null) {
            throw new RuntimeException(String.format("No store object found [id=%s]", id));
        } else if (!result.getType().equals(GtasksList.TYPE)) {
            throw new RuntimeException("Not a google task list");
        }
        return new GtasksList(result);
    }

    @Query("SELECT * FROM store WHERE deleted = 0 AND type = 'gtasks-list' ORDER BY value ASC")
    public abstract List<StoreObject> getGtasksLists();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract long insert(StoreObject storeObject);

    public boolean persist(StoreObject storeObject) {
        long id = insert(storeObject);
        if (id >= 0) {
            storeObject.setId(id);
            return true;
        }
        return false;
    }

    public void persist(GtasksList list) {
        persist(list.getStoreObject());
    }

    @Update
    public abstract void update(StoreObject storeObject);

    @Query("DELETE FROM store WHERE _id = :id")
    public abstract void delete(long id);
}

