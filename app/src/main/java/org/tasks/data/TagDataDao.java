package org.tasks.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.UUIDHelper;
import java.util.List;
import org.tasks.filters.TagFilters;

@Dao
public abstract class TagDataDao {

  @Query("SELECT * FROM tagdata")
  public abstract LiveData<List<TagData>> subscribeToTags();

  @Query("SELECT * FROM tagdata WHERE name = :name COLLATE NOCASE LIMIT 1")
  public abstract TagData getTagByName(String name);

  /**
   * If a tag already exists in the database that case insensitively matches the given tag, return
   * that. Otherwise, return the argument
   */
  public String getTagWithCase(String tag) {
    TagData tagData = getTagByName(tag);
    return tagData != null ? tagData.getName() : tag;
  }

  @Query("SELECT * FROM tagdata")
  public abstract List<TagData> getAll();

  @Query("SELECT * FROM tagdata WHERE remoteId = :uuid LIMIT 1")
  public abstract TagData getByUuid(String uuid);

  @Query("SELECT * FROM tagdata WHERE name IS NOT NULL AND name != '' ORDER BY UPPER(name) ASC")
  public abstract List<TagData> tagDataOrderedByName();

  @Delete
  abstract void deleteTagData(TagData tagData);

  @Query("DELETE FROM tags WHERE tag_uid = :tagUid")
  abstract void deleteTags(String tagUid);

  @Transaction
  public void delete(TagData tagData) {
    deleteTags(tagData.getRemoteId());
    deleteTagData(tagData);
  }

  @Delete
  public abstract void delete(List<TagData> tagData);

  @Query("SELECT tagdata.* FROM tagdata "
      + "INNER JOIN tags ON tags.tag_uid = tagdata.remoteId "
      + "WHERE tags.task = :id "
      + "ORDER BY UPPER(tagdata.name) ASC")
  public abstract List<TagData> getTagDataForTask(long id);

  @Query("SELECT * FROM tagdata WHERE name IN (:names)")
  public abstract List<TagData> getTags(List<String> names);

  @Update
  public abstract void update(TagData tagData);

  @Insert
  abstract long insert(TagData tag);

  public void createNew(TagData tag) {
    if (Task.isUuidEmpty(tag.getRemoteId())) {
      tag.setRemoteId(UUIDHelper.newUUID());
    }
    tag.setId(insert(tag));
  }

  @Query(
      "SELECT tagdata.*, COUNT(tasks._id) AS count"
          + " FROM tagdata"
          + " LEFT JOIN tags ON tags.tag_uid = tagdata.remoteId"
          + " LEFT JOIN tasks ON tags.task = tasks._id AND tasks.deleted = 0 AND tasks.completed = 0 AND tasks.hideUntil < :now"
          + " WHERE tagdata.name IS NOT NULL AND tagdata.name != ''"
          + " GROUP BY tagdata.remoteId"
          + " ORDER BY tagdata.name COLLATE NOCASE")
  public abstract List<TagFilters> getTagFilters(long now);
}
