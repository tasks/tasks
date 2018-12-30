package org.tasks.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.UUIDHelper;
import java.util.List;

@Dao
public abstract class TaskAttachmentDao {

  @Query("SELECT * FROM task_attachments WHERE task_id = :taskUuid")
  public abstract List<TaskAttachment> getAttachments(String taskUuid);

  @Delete
  public abstract void delete(TaskAttachment taskAttachment);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  public abstract void insert(TaskAttachment attachment);

  @Update
  public abstract void update(TaskAttachment attachment);

  public void createNew(TaskAttachment attachment) {
    if (Task.isUuidEmpty(attachment.getRemoteId())) {
      attachment.setRemoteId(UUIDHelper.newUUID());
    }
    insert(attachment);
  }
}
