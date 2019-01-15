package org.tasks.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.UUIDHelper;
import java.util.List;

@Dao
public abstract class UserActivityDao {

  @Insert
  public abstract void insert(UserActivity userActivity);

  @Update
  public abstract void update(UserActivity userActivity);

  @Delete
  public abstract void delete(UserActivity userActivity);

  @Query("SELECT * FROM userActivity WHERE target_id = :taskUuid ORDER BY created_at DESC ")
  public abstract List<UserActivity> getCommentsForTask(String taskUuid);

  @Query(
      "SELECT userActivity.* FROM userActivity INNER JOIN tasks ON tasks._id = :task WHERE target_id = tasks.remoteId")
  public abstract List<UserActivity> getComments(long task);

  @Query("SELECT * FROM userActivity")
  public abstract List<UserActivity> getComments();

  public void createNew(UserActivity item) {
    if (item.getCreated() == null || item.getCreated() == 0L) {
      item.setCreated(DateUtilities.now());
    }
    if (Task.isUuidEmpty(item.getRemoteId())) {
      item.setRemoteId(UUIDHelper.newUUID());
    }
    insert(item);
  }
}
