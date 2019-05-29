package org.tasks.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.RoomWarnings;
import androidx.room.Transaction;
import androidx.room.Update;
import java.util.List;
import timber.log.Timber;

@Dao
public abstract class GoogleTaskDao {

  @Insert
  public abstract void insert(GoogleTask task);

  @Transaction
  public void insertAndShift(GoogleTask task, boolean top) {
    if (top) {
      task.setOrder(0);
      shiftDown(task.getListId(), task.getParent(), 0);
    } else {
      task.setOrder(getBottom(task.getListId(), task.getParent()));
    }
    insert(task);
  }

  @Query(
      "UPDATE google_tasks SET gt_order = gt_order + 1 WHERE gt_list_id = :listId AND gt_parent = :parent AND gt_order >= :position")
  abstract void shiftDown(String listId, long parent, long position);

  @Query(
      "UPDATE google_tasks SET gt_order = gt_order - 1 WHERE gt_list_id = :listId AND gt_parent = :parent AND gt_order > :from AND gt_order <= :to")
  abstract void shiftUp(String listId, long parent, long from, long to);

  @Query(
      "UPDATE google_tasks SET gt_order = gt_order + 1 WHERE gt_list_id = :listId AND gt_parent = :parent AND gt_order < :from AND gt_order >= :to")
  abstract void shiftDown(String listId, long parent, long from, long to);

  @Query(
      "UPDATE google_tasks SET gt_order = gt_order - 1 WHERE gt_list_id = :listId AND gt_parent = :parent AND gt_order >= :position")
  abstract void shiftUp(String listId, long parent, long position);

  @Transaction
  public void move(GoogleTask task, long newParent, long newPosition) {
    long previousParent = task.getParent();
    long previousPosition = task.getOrder();

    if (newParent == previousParent) {
      if (previousPosition < newPosition) {
        shiftUp(task.getListId(), newParent, previousPosition, newPosition);
      } else {
        shiftDown(task.getListId(), newParent, previousPosition, newPosition);
      }
    } else {
      shiftUp(task.getListId(), previousParent, previousPosition);
      shiftDown(task.getListId(), newParent, newPosition);
    }
    task.setMoved(true);
    task.setParent(newParent);
    task.setOrder(newPosition);
    update(task);
  }

  @Query("SELECT * FROM google_tasks WHERE gt_task = :taskId AND gt_deleted = 0 LIMIT 1")
  public abstract GoogleTask getByTaskId(long taskId);

  @Update
  public abstract void update(GoogleTask googleTask);

  @Delete
  public abstract void delete(GoogleTask deleted);

  @Query("SELECT * FROM google_tasks WHERE gt_remote_id = :remoteId LIMIT 1")
  public abstract GoogleTask getByRemoteId(String remoteId);

  @Query("SELECT * FROM google_tasks WHERE gt_task = :taskId AND gt_deleted > 0")
  public abstract List<GoogleTask> getDeletedByTaskId(long taskId);

  @Query("SELECT * FROM google_tasks WHERE gt_task = :taskId")
  public abstract List<GoogleTask> getAllByTaskId(long taskId);

  @Query("SELECT DISTINCT gt_list_id FROM google_tasks WHERE gt_deleted = 0 AND gt_task IN (:tasks)")
  public abstract List<String> getLists(List<Long> tasks);

  @Query("SELECT gt_task FROM google_tasks WHERE gt_parent IN (:ids)")
  public abstract List<Long> getChildren(List<Long> ids);

  @Query(
      "SELECT IFNULL(MAX(gt_order), -1) + 1 FROM google_tasks WHERE gt_list_id = :listId AND gt_parent = :parent")
  public abstract long getBottom(String listId, long parent);

  @Query(
      "SELECT gt_remote_id FROM google_tasks JOIN tasks ON tasks._id = gt_task WHERE deleted = 0 AND gt_list_id = :listId AND gt_parent = :parent AND gt_order < :order AND gt_remote_id IS NOT NULL AND gt_remote_id != '' ORDER BY gt_order DESC")
  public abstract String getPrevious(String listId, long parent, long order);

  @Query("SELECT gt_remote_id FROM google_tasks WHERE gt_task = :task")
  public abstract String getRemoteId(long task);

  @Query("SELECT gt_task FROM google_tasks WHERE gt_remote_id = :remoteId")
  public abstract long getTask(String remoteId);

  @SuppressWarnings({RoomWarnings.CURSOR_MISMATCH, "AndroidUnresolvedRoomSqlReference"})
  @Query(
      "SELECT google_tasks.*, gt_order AS primary_sort, NULL AS secondary_sort FROM google_tasks JOIN tasks ON tasks._id = gt_task WHERE gt_parent = 0 AND gt_list_id = :listId AND tasks.deleted = 0 UNION SELECT c.*, p.gt_order AS primary_sort, c.gt_order AS secondary_sort FROM google_tasks AS c LEFT JOIN google_tasks AS p ON c.gt_parent = p.gt_task JOIN tasks ON tasks._id = c.gt_task WHERE c.gt_parent > 0 AND c.gt_list_id = :listId AND tasks.deleted = 0 ORDER BY primary_sort ASC, secondary_sort ASC")
  abstract List<GoogleTask> getByLocalOrder(String listId);

  @SuppressWarnings({RoomWarnings.CURSOR_MISMATCH, "AndroidUnresolvedRoomSqlReference"})
  @Query(
      "SELECT google_tasks.*, gt_remote_order AS primary_sort, NULL AS secondary_sort FROM google_tasks JOIN tasks ON tasks._id = gt_task WHERE gt_parent = 0 AND gt_list_id = :listId AND tasks.deleted = 0 UNION SELECT c.*, p.gt_remote_order AS primary_sort, c.gt_remote_order AS secondary_sort FROM google_tasks AS c LEFT JOIN google_tasks AS p ON c.gt_parent = p.gt_task JOIN tasks ON tasks._id = c.gt_task WHERE c.gt_parent > 0 AND c.gt_list_id = :listId AND tasks.deleted = 0 ORDER BY primary_sort ASC, secondary_sort ASC")
  abstract List<GoogleTask> getByRemoteOrder(String listId);

  @Query(
      "UPDATE google_tasks SET gt_parent = IFNULL((SELECT gt_task FROM google_tasks AS p WHERE p.gt_remote_id = google_tasks.gt_remote_parent), gt_parent) WHERE gt_list_id = :listId AND gt_moved = 0 AND gt_remote_parent IS NOT NULL AND gt_remote_parent != ''")
  abstract void updateParents(String listId);

  @Query(
      "UPDATE google_tasks SET gt_remote_parent = :parent, gt_remote_order = :position WHERE gt_remote_id = :id")
  public abstract void updatePosition(String id, String parent, String position);

  @Transaction
  public void reposition(String listId) {
    updateParents(listId);

    List<GoogleTask> orderedTasks = getByRemoteOrder(listId);
    int subtasks = 0;
    int parent = 0;
    for (int i = 0; i < orderedTasks.size(); i++) {
      GoogleTask task = orderedTasks.get(i);
      if (task.getParent() > 0) {
        if (task.getOrder() != subtasks && !task.isMoved()) {
          task.setOrder(subtasks);
          update(task);
        }
        subtasks++;
      } else {
        subtasks = 0;
        if (task.getOrder() != parent && !task.isMoved()) {
          task.setOrder(parent);
          update(task);
        }
        parent++;
      }
    }
  }

  public void validateSorting(String listId) {
    List<GoogleTask> orderedTasks = getByLocalOrder(listId);
    int subtasks = 0;
    int parent = 0;
    for (int i = 0; i < orderedTasks.size(); i++) {
      GoogleTask task = orderedTasks.get(i);
      if (task.getParent() > 0) {
        if (task.getOrder() != subtasks) {
          Timber.e("Subtask violation expected %s but was %s", subtasks, task.getOrder());
        }
        subtasks++;
      } else {
        subtasks = 0;
        if (task.getOrder() != parent) {
          Timber.e("Parent violation expected %s but was %s", parent, task.getOrder());
        }
        parent++;
      }
    }
  }
}
