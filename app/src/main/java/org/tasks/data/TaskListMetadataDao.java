package org.tasks.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.helper.UUIDHelper;

@Dao
public abstract class TaskListMetadataDao {

    @Query("SELECT * from task_list_metadata where tag_uuid = :tagUuid OR filter = :tagUuid LIMIT 1")
    public abstract TaskListMetadata fetchByTagId(String tagUuid);

    @Update
    public abstract void update(TaskListMetadata taskListMetadata);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insert(TaskListMetadata taskListMetadata);

    public void createNew(TaskListMetadata taskListMetadata) {
        if (RemoteModel.isUuidEmpty(taskListMetadata.getRemoteId())) {
            taskListMetadata.setRemoteId(UUIDHelper.newUUID());
        }
        insert(taskListMetadata);
    }
}

