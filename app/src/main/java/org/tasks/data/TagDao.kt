package org.tasks.data

import androidx.room.*
import com.todoroo.astrid.data.Task

@Dao
abstract class TagDao {
    @Query("UPDATE tags SET name = :name WHERE tag_uid = :tagUid")
    abstract fun rename(tagUid: String, name: String)

    @Insert
    abstract fun insert(tag: Tag)

    @Insert
    abstract fun insert(tags: Iterable<Tag>)

    @Query("DELETE FROM tags WHERE task = :taskId AND tag_uid in (:tagUids)")
    abstract fun deleteTags(taskId: Long, tagUids: List<String>)

    @Query("SELECT name FROM tags WHERE task = :taskId ORDER BY UPPER(name) ASC")
    abstract fun getTagNames(taskId: Long): List<String>

    @Query("SELECT * FROM tags WHERE tag_uid = :tagUid")
    abstract fun getByTagUid(tagUid: String): List<Tag>

    @Query("SELECT * FROM tags WHERE task = :taskId")
    abstract fun getTagsForTask(taskId: Long): List<Tag>

    @Query("SELECT * FROM tags WHERE task = :taskId AND tag_uid = :tagUid")
    abstract fun getTagByTaskAndTagUid(taskId: Long, tagUid: String): Tag?

    @Delete
    abstract fun delete(tags: List<Tag>)

    @Transaction
    open fun applyTags(task: Task, tagDataDao: TagDataDao, current: List<TagData>): Boolean {
        val taskId = task.id
        val existing = HashSet(tagDataDao.getTagDataForTask(taskId))
        val selected = HashSet<TagData>(current)
        val added = selected subtract existing
        val removed = existing subtract selected
        deleteTags(taskId, removed.map { td -> td.remoteId!! })
        insert(task, added)
        return removed.isNotEmpty() || added.isNotEmpty()
    }

    fun insert(task: Task, tags: Collection<TagData>) {
        if (!tags.isEmpty()) {
            insert(tags.map { Tag(task, it) })
        }
    }
}