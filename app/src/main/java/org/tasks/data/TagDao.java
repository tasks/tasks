package org.tasks.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface TagDao {
    @Query("UPDATE tags SET name = :name WHERE tag_uid = :tagUid")
    void rename(String tagUid, String name);

    @Query("DELETE FROM tags WHERE tag_uid = :tagUid")
    void deleteTag(String tagUid);

    @Insert
    void insert(Tag tag);

    @Insert
    void insert(List<Tag> tags);

    @Query("DELETE FROM tags WHERE task = :taskId AND tag_uid = :tagUid")
    void deleteTag(long taskId, String tagUid);

    @Query("SELECT name FROM tags WHERE task = :taskId ORDER BY UPPER(name) ASC")
    List<String> getTagNames(long taskId);

    @Query("SELECT tag_uid FROM tags WHERE task = :taskId")
    List<String> getTagUids(long taskId);

    @Query("SELECT tag_uid FROM tags WHERE task_uid = :taskUid")
    List<String> getTagUids(String taskUid);

    @Query("SELECT * FROM tags WHERE tag_uid = :tagUid")
    List<Tag> getByTagUid(String tagUid);

    @Query("SELECT * FROM tags WHERE task = :taskId")
    List<Tag> getTagsForTask(long taskId);

    @Query("SELECT * FROM tags WHERE task = :taskId AND tag_uid = :tagUid")
    Tag getTagByTaskAndTagUid(long taskId, String tagUid);

    @Query("DELETE FROM tags WHERE task = :taskId")
    void deleteByTaskId(long taskId);

    @Query("DELETE FROM tags WHERE _id = :id")
    void deleteById(long id);
}
