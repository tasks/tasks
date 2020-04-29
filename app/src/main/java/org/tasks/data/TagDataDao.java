package org.tasks.data;

import static com.google.common.collect.FluentIterable.concat;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptySet;
import static org.tasks.db.DbUtils.MAX_SQLITE_ARGS;

import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.UUIDHelper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.tasks.filters.AlphanumComparator;
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

  public List<TagData> searchTags(String query) {
    List<TagData> results = searchTagsInternal("%" + query + "%");
    Collections.sort(results, new AlphanumComparator<>(TagData::getName));
    return results;
  }

  @Query("SELECT * FROM tagdata WHERE name LIKE :query AND name NOT NULL AND name != ''")
  protected abstract List<TagData> searchTagsInternal(String query);

  @Query("SELECT * FROM tagdata")
  public abstract List<TagData> getAll();

  @Query("SELECT * FROM tagdata WHERE remoteId = :uuid LIMIT 1")
  public abstract TagData getByUuid(String uuid);

  @Query("SELECT * FROM tagdata WHERE remoteId IN (:uuids)")
  public abstract List<TagData> getByUuid(Collection<String> uuids);

  @Query("SELECT * FROM tagdata WHERE name IS NOT NULL AND name != '' ORDER BY UPPER(name) ASC")
  public abstract List<TagData> tagDataOrderedByName();

  @Delete
  abstract void deleteTagData(TagData tagData);

  @Query("DELETE FROM tags WHERE tag_uid = :tagUid")
  abstract void deleteTags(String tagUid);

  @Query("SELECT * FROM tags WHERE task IN (:tasks) AND tag_uid NOT IN (:tagsToKeep)")
  abstract List<Tag> tagsToDelete(List<Long> tasks, List<String> tagsToKeep);

  public Pair<Set<String>, Set<String>> getTagSelections(List<Long> tasks) {
    List<String> allTags = getAllTags(tasks);
    Collection<Set<String>> tags =
        Collections2.transform(allTags, t -> t == null ? emptySet() : newHashSet(t.split(",")));
    Set<String> partialTags = newHashSet(concat(tags));
    Set<String> commonTags = null;
    if (tags.isEmpty()) {
      commonTags = emptySet();
    } else {
      for (Set<String> s : tags) {
        if (commonTags == null) {
          commonTags = s;
        } else {
          commonTags.retainAll(s);
        }
      }
    }
    partialTags.removeAll(commonTags);
    return Pair.create(partialTags, commonTags);
  }

  @Query(
      "SELECT GROUP_CONCAT(DISTINCT(tag_uid)) FROM tasks"
          + " LEFT JOIN tags ON tags.task = tasks._id"
          + " WHERE tasks._id IN (:tasks)"
          + " GROUP BY tasks._id")
  abstract List<String> getAllTags(List<Long> tasks);

  @Transaction
  public List<Long> applyTags(
      List<Task> tasks, List<TagData> partiallySelected, List<TagData> selected) {
    Set<Long> modified = newHashSet();
    List<String> keep =
        concat(partiallySelected, selected).transform(TagData::getRemoteId).toList();
    for (List<Task> sublist : Iterables.partition(tasks, MAX_SQLITE_ARGS - keep.size())) {
      List<Tag> tags = tagsToDelete(from(sublist).transform(Task::getId).toList(), keep);
      deleteTags(tags);
      modified.addAll(transform(tags, Tag::getTask));
    }
    for (Task task : tasks) {
      Set<TagData> added =
          difference(newHashSet(selected), newHashSet(getTagDataForTask(task.getId())));
      if (added.size() > 0) {
        modified.add(task.getId());
        insert(transform(added, td -> new Tag(task, td)));
      }
    }
    return new ArrayList<>(modified);
  }

  @Transaction
  public void delete(TagData tagData) {
    deleteTags(tagData.getRemoteId());
    deleteTagData(tagData);
  }

  @Delete
  public abstract void delete(List<TagData> tagData);

  @Delete
  abstract void deleteTags(List<Tag> tags);

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

  @Insert
  public abstract void insert(Iterable<Tag> tags);

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
          + " GROUP BY tagdata.remoteId")
  public abstract List<TagFilters> getTagFilters(long now);
}
