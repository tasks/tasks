package org.tasks.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public abstract class TaskListMetadataDao {

  @Query("SELECT * from task_list_metadata where tag_uuid = :tagUuid OR filter = :tagUuid LIMIT 1")
  public abstract TaskListMetadata fetchByTagOrFilter(String tagUuid);

  @Update
  public abstract void update(TaskListMetadata taskListMetadata);

  @Insert
  abstract long insert(TaskListMetadata taskListMetadata);

  public void createNew(TaskListMetadata taskListMetadata) {
    taskListMetadata.setId(insert(taskListMetadata));
  }
}
