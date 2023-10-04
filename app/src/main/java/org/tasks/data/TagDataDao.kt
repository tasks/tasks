package org.tasks.data

import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.todoroo.astrid.api.Filter.Companion.NO_ORDER
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.helper.UUIDHelper
import org.tasks.db.DbUtils
import org.tasks.filters.AlphanumComparator
import org.tasks.filters.TagFilters
import org.tasks.time.DateTimeUtils.currentTimeMillis

@Dao
abstract class TagDataDao {
    @Query("SELECT * FROM tagdata")
    abstract fun subscribeToTags(): LiveData<List<TagData>>

    @Query("SELECT * FROM tagdata WHERE name = :name COLLATE NOCASE LIMIT 1")
    abstract suspend fun getTagByName(name: String): TagData?

    /**
     * If a tag already exists in the database that case insensitively matches the given tag, return
     * that. Otherwise, return the argument
     */
    suspend fun getTagWithCase(tag: String): String = getTagByName(tag)?.name ?: tag

    suspend fun searchTags(query: String): List<TagData> = searchTagsInternal("%$query%").sort()

    @Query("SELECT * FROM tagdata WHERE name LIKE :query AND name NOT NULL AND name != ''")
    protected abstract suspend fun searchTagsInternal(query: String): List<TagData>

    @Query("SELECT * FROM tagdata")
    abstract suspend fun getAll(): List<TagData>

    @Query("SELECT * FROM tagdata WHERE remoteId = :uuid LIMIT 1")
    abstract suspend fun getByUuid(uuid: String): TagData?

    @Query("SELECT * FROM tagdata WHERE remoteId IN (:uuids)")
    abstract suspend fun getByUuid(uuids: Collection<String>): List<TagData>

    @Query("SELECT * FROM tagdata WHERE name IS NOT NULL AND name != '' ORDER BY UPPER(name) ASC")
    abstract suspend fun tagDataOrderedByName(): List<TagData>

    @Delete
    internal abstract suspend fun deleteTagData(tagData: TagData)

    @Query("DELETE FROM tags WHERE tag_uid = :tagUid")
    internal abstract suspend fun deleteTags(tagUid: String)

    @Query("SELECT * FROM tags WHERE task IN (:tasks) AND tag_uid NOT IN (:tagsToKeep)")
    internal abstract suspend fun tagsToDelete(tasks: List<Long>, tagsToKeep: List<String>): List<Tag>
    
    suspend fun getTagSelections(tasks: List<Long>): Pair<Set<String>, Set<String>> {
        val allTags = getAllTags(tasks)
        val tags = allTags.map { t: String? -> HashSet<String>(t?.split(",") ?: emptySet()) }
        val partialTags = tags.flatten().toMutableSet()
        var commonTags: MutableSet<String>? = null
        if (tags.isEmpty()) {
            commonTags = HashSet()
        } else {
            for (s in tags) {
                if (commonTags == null) {
                    commonTags = s.toMutableSet()
                } else {
                    commonTags.retainAll(s)
                }
            }
        }
        partialTags.removeAll(commonTags!!)
        return Pair(partialTags, commonTags)
    }

    @Query("SELECT GROUP_CONCAT(DISTINCT(tag_uid)) FROM tasks"
            + " LEFT JOIN tags ON tags.task = tasks._id"
            + " WHERE tasks._id IN (:tasks)"
            + " GROUP BY tasks._id")
    internal abstract suspend fun getAllTags(tasks: List<Long>): List<String>

    @Transaction
    open suspend fun applyTags(
            tasks: List<Task>, partiallySelected: List<TagData>, selected: List<TagData>): List<Long> {
        val modified = HashSet<Long>()
        val keep = partiallySelected.plus(selected).map { it.remoteId!! }
        for (sublist in tasks.chunked(DbUtils.MAX_SQLITE_ARGS - keep.size)) {
            val tags = tagsToDelete(sublist.map(Task::id), keep)
            deleteTags(tags)
            modified.addAll(tags.map(Tag::task))
        }
        for (task in tasks) {
            val added = selected subtract getTagDataForTask(task.id)
            if (added.isNotEmpty()) {
                modified.add(task.id)
                insert(added.map { Tag(task, it) })
            }
        }
        return ArrayList(modified)
    }

    @Transaction
    open suspend fun delete(tagData: TagData) {
        deleteTags(tagData.remoteId!!)
        deleteTagData(tagData)
    }

    @Delete
    abstract suspend fun delete(tagData: List<TagData>)

    @Delete
    internal abstract suspend fun deleteTags(tags: List<Tag>)

    @Query("SELECT tagdata.* FROM tagdata "
            + "INNER JOIN tags ON tags.tag_uid = tagdata.remoteId "
            + "WHERE tags.task = :id "
            + "ORDER BY UPPER(tagdata.name) ASC")
    abstract suspend fun getTagDataForTask(id: Long): List<TagData>

    @Query("SELECT * FROM tagdata WHERE name IN (:names)")
    abstract suspend fun getTags(names: List<String>): List<TagData>

    @Update
    abstract suspend fun update(tagData: TagData)

    @Insert
    abstract suspend fun insert(tag: TagData): Long

    @Insert
    abstract suspend fun insert(tags: Iterable<Tag>)

    suspend fun createNew(tag: TagData) {
        if (Task.isUuidEmpty(tag.remoteId)) {
            tag.remoteId = UUIDHelper.newUUID()
        }
        tag.id = insert(tag)
    }

    @Query("SELECT tagdata.*, COUNT(tasks._id) AS count"
            + " FROM tagdata"
            + " LEFT JOIN tags ON tags.tag_uid = tagdata.remoteId"
            + " LEFT JOIN tasks ON tags.task = tasks._id AND tasks.deleted = 0 AND tasks.completed = 0 AND tasks.hideUntil < :now"
            + " WHERE tagdata.name IS NOT NULL AND tagdata.name != ''"
            + " GROUP BY tagdata.remoteId")
    abstract suspend fun getTagFilters(now: Long = currentTimeMillis()): List<TagFilters>

    @Query("UPDATE tagdata SET td_order = $NO_ORDER")
    abstract suspend fun resetOrders()

    @Query("UPDATE tagdata SET td_order = :order WHERE _id = :id")
    abstract suspend fun setOrder(id: Long, order: Int)

    suspend fun getTags(task: Task) = ArrayList(if (task.isNew) {
        task.tags.mapNotNull { getTagByName(it) }
    } else {
        getTagDataForTask(task.id)
    })

    companion object {
        private val COMPARATOR = Comparator<TagData> { f1, f2 ->
            when {
                f1.order == NO_ORDER && f2.order == NO_ORDER -> f1.id!!.compareTo(f2.id!!)
                f1.order == NO_ORDER -> 1
                f2.order == NO_ORDER -> -1
                f1.order < f2.order -> -1
                f1.order > f2.order -> 1
                else -> AlphanumComparator.TAGDATA.compare(f1, f2)
            }
        }

        private fun List<TagData>.sort(): List<TagData> =
                if (all { it.order == NO_ORDER }) {
                    sortedWith(AlphanumComparator.TAGDATA)
                } else {
                    sortedWith(COMPARATOR)
                }
    }
}