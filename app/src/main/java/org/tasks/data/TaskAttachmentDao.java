package org.tasks.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
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
