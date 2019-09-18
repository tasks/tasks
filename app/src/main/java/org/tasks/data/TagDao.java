package org.tasks.data;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.newHashSet;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import com.todoroo.astrid.data.Task;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Dao
public abstract class TagDao {

  @Query("UPDATE tags SET name = :name WHERE tag_uid = :tagUid")
  public abstract void rename(String tagUid, String name);

  @Query("DELETE FROM tags WHERE tag_uid = :tagUid")
  public abstract void deleteTag(String tagUid);

  @Insert
  public abstract void insert(Tag tag);

  @Insert
  public abstract void insert(Iterable<Tag> tags);

  @Query("DELETE FROM tags WHERE task = :taskId AND tag_uid in (:tagUids)")
  public abstract void deleteTags(long taskId, List<String> tagUids);

  @Query("SELECT name FROM tags WHERE task = :taskId ORDER BY UPPER(name) ASC")
  public abstract List<String> getTagNames(long taskId);

  @Query("SELECT * FROM tags WHERE tag_uid = :tagUid")
  public abstract List<Tag> getByTagUid(String tagUid);

  @Query("SELECT * FROM tags WHERE task = :taskId")
  public abstract List<Tag> getTagsForTask(long taskId);

  @Query("SELECT * FROM tags WHERE task = :taskId AND tag_uid = :tagUid")
  public abstract Tag getTagByTaskAndTagUid(long taskId, String tagUid);

  @Query("DELETE FROM tags WHERE _id = :id")
  public abstract void deleteById(long id);

  @Transaction
  public boolean applyTags(Task task, TagDataDao tagDataDao,List<TagData> current) {
    long taskId = task.getId();
    Set<TagData> existing = newHashSet(tagDataDao.getTagDataForTask(taskId));
    Set<TagData> selected = newHashSet(current);
    Set<TagData> added = difference(selected, existing);
    Set<TagData> removed = difference(existing, selected);
    deleteTags(taskId, newArrayList(transform(removed, TagData::getRemoteId)));
    insert(task, added);
    return !(removed.isEmpty() && added.isEmpty());
  }

  public void insert(Task task, Collection<TagData> tags) {
    if (!tags.isEmpty()) {
      insert(transform(tags, td -> new Tag(task, td)));
    }
  }
}
