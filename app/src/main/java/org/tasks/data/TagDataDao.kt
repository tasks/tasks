package org.tasks.data

import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import androidx.room.*
import com.todoroo.astrid.api.FilterListItem.NO_ORDER
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.helper.UUIDHelper
import org.tasks.db.DbUtils
import org.tasks.filters.AlphanumComparator
import org.tasks.filters.TagFilters
import org.tasks.time.DateTimeUtils.currentTimeMillis
import java.util.*
import kotlin.collections.HashSet

@Dao
abstract class TagDataDao {
    @Query("SELECT * FROM tagdata")
    abstract fun subscribeToTags(): LiveData<List<TagData>>

    @Query("SELECT * FROM tagdata WHERE name = :name COLLATE NOCASE LIMIT 1")
    abstract fun getTagByName(name: String): TagData?

    /**
     * If a tag already exists in the database that case insensitively matches the given tag, return
     * that. Otherwise, return the argument
     */
    fun getTagWithCase(tag: String): String? {
        return getTagByName(tag)?.name ?: tag
    }

    fun searchTags(query: String): List<TagData> {
        return searchTagsInternal("%$query%")
                .sortedWith(AlphanumComparator(TagData::name))
                .toMutableList()
    }

    @Query("SELECT * FROM tagdata WHERE name LIKE :query AND name NOT NULL AND name != ''")
    protected abstract fun searchTagsInternal(query: String): List<TagData>

    @Query("SELECT * FROM tagdata")
    abstract fun getAll(): List<TagData>

    @Query("SELECT * FROM tagdata WHERE remoteId = :uuid LIMIT 1")
    abstract fun getByUuid(uuid: String): TagData?

    @Query("SELECT * FROM tagdata WHERE remoteId IN (:uuids)")
    abstract fun getByUuid(uuids: Collection<String>): List<TagData>

    @Query("SELECT * FROM tagdata WHERE name IS NOT NULL AND name != '' ORDER BY UPPER(name) ASC")
    abstract fun tagDataOrderedByName(): List<TagData>

    @Delete
    abstract fun deleteTagData(tagData: TagData)

    @Query("DELETE FROM tags WHERE tag_uid = :tagUid")
    abstract fun deleteTags(tagUid: String)

    @Query("SELECT * FROM tags WHERE task IN (:tasks) AND tag_uid NOT IN (:tagsToKeep)")
    abstract fun tagsToDelete(tasks: List<Long>, tagsToKeep: List<String>): List<Tag>
    
    fun getTagSelections(tasks: List<Long>): Pair<Set<String>, Set<String>> {
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
    abstract fun getAllTags(tasks: List<Long>): List<String>

    @Transaction
    open fun applyTags(
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
    open fun delete(tagData: TagData) {
        deleteTags(tagData.remoteId!!)
        deleteTagData(tagData)
    }

    @Delete
    abstract fun delete(tagData: List<TagData>)

    @Delete
    abstract fun deleteTags(tags: List<Tag>)

    @Query("SELECT tagdata.* FROM tagdata "
            + "INNER JOIN tags ON tags.tag_uid = tagdata.remoteId "
            + "WHERE tags.task = :id "
            + "ORDER BY UPPER(tagdata.name) ASC")
    abstract fun getTagDataForTask(id: Long): List<TagData>

    @Query("SELECT * FROM tagdata WHERE name IN (:names)")
    abstract fun getTags(names: List<String>): List<TagData>

    @Update
    abstract fun update(tagData: TagData)

    @Insert
    abstract fun insert(tag: TagData): Long

    @Insert
    abstract fun insert(tags: Iterable<Tag>)

    fun createNew(tag: TagData) {
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
    abstract fun getTagFilters(now: Long = currentTimeMillis()): List<TagFilters>

    @Query("UPDATE tagdata SET td_order = $NO_ORDER")
    abstract fun resetOrders()

    @Query("UPDATE tagdata SET td_order = :order WHERE _id = :id")
    abstract fun setOrder(id: Long, order: Int)
}