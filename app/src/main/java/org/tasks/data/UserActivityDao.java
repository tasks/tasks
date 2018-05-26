package org.tasks.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.UUIDHelper;
import java.util.List;

@Dao
public abstract class UserActivityDao {

  @Insert
  public abstract void insert(UserActivity userActivity);

  @Query("SELECT * FROM userActivity WHERE target_id = :taskUuid ORDER BY created_at DESC ")
  public abstract List<UserActivity> getCommentsForTask(String taskUuid);

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
