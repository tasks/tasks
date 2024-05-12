package org.tasks.data.dao

import androidx.room.*
import org.tasks.data.entity.Task
import org.tasks.data.entity.Tag
import org.tasks.data.entity.TagData

@Dao
abstract class TagDao {
    @Query("UPDATE tags SET name = :name WHERE tag_uid = :tagUid")
    abstract suspend fun rename(tagUid: String, name: String)

    @Insert
    abstract suspend fun insert(tag: Tag)

    @Insert
    abstract suspend fun insert(tags: Iterable<Tag>)

    @Query("DELETE FROM tags WHERE task = :taskId AND tag_uid in (:tagUids)")
    internal abstract suspend fun deleteTags(taskId: Long, tagUids: List<String>)

    @Query("SELECT * FROM tags WHERE tag_uid = :tagUid")
    abstract suspend fun getByTagUid(tagUid: String): List<Tag>

    @Query("SELECT * FROM tags WHERE task = :taskId")
    abstract suspend fun getTagsForTask(taskId: Long): List<Tag>

    @Query("SELECT * FROM tags WHERE task = :taskId AND tag_uid = :tagUid")
    abstract suspend fun getTagByTaskAndTagUid(taskId: Long, tagUid: String): Tag?

    @Delete
    abstract suspend fun delete(tags: List<Tag>)

    @Transaction
    open suspend fun applyTags(task: Task, tagDataDao: TagDataDao, current: List<TagData>) {
        val taskId = task.id
        val existing = HashSet(tagDataDao.getTagDataForTask(taskId))
        val selected = HashSet<TagData>(current)
        val added = selected subtract existing
        val removed = existing subtract selected
        deleteTags(taskId, removed.map { td -> td.remoteId!! })
        insert(task, added)
    }

    suspend fun insert(task: Task, tags: Collection<TagData>) {
        if (!tags.isEmpty()) {
            insert(tags.map { Tag(task, it) })
        }
    }
}