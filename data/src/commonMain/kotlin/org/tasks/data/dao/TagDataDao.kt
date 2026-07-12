package org.tasks.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import org.tasks.data.NO_ORDER
import org.tasks.data.TagFilters
import org.tasks.data.db.Database
import org.tasks.data.db.DbUtils
import org.tasks.data.db.SuspendDbUtils.eachChunk
import org.tasks.data.entity.MetadataTombstone
import org.tasks.data.entity.Tag
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.data.entity.isSyncable
import org.tasks.data.entity.normalizeColor
import org.tasks.data.entity.normalizeIcon
import org.tasks.time.DateTimeUtils2.currentTimeMillis

const val METADATA_CATEGORY_TAG = "tag"

data class TagFieldUpdate(val id: Long, val remoteId: String?, val name: String?, val color: Int?, val icon: String?)

data class RemoteTagEntry(val key: String, val name: String?, val color: Int?, val icon: String?)

data class TagWithState(
    @Embedded val tag: TagData,
    val reaped: Boolean = false,
    val dirty: Boolean = false,
    val dirtyVersion: Long = 0,
)

fun orderedNormalizedNames(tags: List<TagWithState>): List<String> =
    tags.filter { it.tag.isSyncable() && !it.reaped && it.tag.order != NO_ORDER }
        .sortedBy { it.tag.order }
        .map { it.tag.normalizedName }

fun <T> threeWay(local: T?, base: T?, remote: T?): T? = if (local != base) local else remote

@Dao
abstract class TagDataDao(private val database: Database) {
    @Query("SELECT * FROM tagdata")
    abstract fun subscribeToTags(): Flow<List<TagData>>

    /**
     * If a tag already exists in the database that case insensitively matches the given tag, return
     * that. Otherwise, return the argument
     */
    suspend fun getTagWithCase(tag: String): String = getTagByName(tag)?.name ?: tag

    @Query("SELECT * FROM tagdata WHERE name LIKE :query AND name NOT NULL AND name != ''")
    abstract suspend fun searchTagsInternal(query: String): List<TagData>

    @Query("SELECT * FROM tagdata")
    abstract suspend fun getAll(): List<TagData>

    @Query("SELECT * FROM tagdata WHERE remoteId = :uuid LIMIT 1")
    abstract suspend fun getByUuid(uuid: String): TagData?

    @Query("SELECT * FROM tagdata WHERE remoteId IN (:uuids)")
    abstract suspend fun getByUuid(uuids: Collection<String>): List<TagData>

    @Query("SELECT * FROM tagdata WHERE normalized_name = :normalized LIMIT 1")
    internal abstract suspend fun getByNormalizedName(normalized: String): TagData?

    @Query("SELECT * FROM tagdata WHERE normalized_name IN (:normalized)")
    internal abstract suspend fun getByNormalizedNames(normalized: List<String>): List<TagData>

    suspend fun getTagByName(name: String): TagData? = getByNormalizedName(TagData.normalize(name))

    suspend fun getOrCreateTag(name: String): TagData {
        getTagByName(name)?.let { return it }
        val tag = TagData(name = name)
        val id = insertOrIgnore(tag)
        return if (id == -1L) getTagByName(name)!! else tag.copy(id = id)
    }

    suspend fun getOrCreateTags(names: List<String>): List<TagData> {
        if (names.isEmpty()) {
            return emptyList()
        }
        val byNormalized = LinkedHashMap<String, String>()
        for (name in names) {
            byNormalized.getOrPut(TagData.normalize(name)) { name }
        }
        val existing = getByNormalizedNames(byNormalized.keys.toList()).associateBy { it.normalizedName }
        return byNormalized.map { (normalized, name) -> existing[normalized] ?: getOrCreateTag(name) }
    }

    suspend fun insertIfAbsent(tag: TagData): TagData? {
        val id = insertOrIgnore(tag)
        return if (id == -1L) null else tag.copy(id = id)
    }

    @Query("SELECT * FROM tagdata WHERE name IS NOT NULL AND name != '' ORDER BY UPPER(name) ASC")
    abstract suspend fun tagDataOrderedByName(): List<TagData>

    @Delete
    internal abstract suspend fun deleteTagData(tagData: TagData)

    @Query("DELETE FROM tags WHERE tag_uid = :tagUid")
    internal abstract suspend fun deleteTags(tagUid: String)

    @Query("SELECT * FROM tags WHERE task IN (:tasks)")
    internal abstract suspend fun getTagsForTasks(tasks: List<Long>): List<Tag>

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
    internal abstract suspend fun getAllTags(tasks: List<Long>): List<String?>

    @Transaction
    open suspend fun applyTags(
        tasks: List<Task>,
        partiallySelected: List<TagData>,
        selected: List<TagData>
    ): List<Long> {
        Logger.d("TagDataDao") { "applyTags tasks=$tasks partiallySelected=$partiallySelected selected=$selected" }
        val modified = HashSet<Long>()
        val canonical = rescueReaped(partiallySelected + selected).associateBy { it.normalizedName }
        val keep = canonical.values.mapNotNull { it.remoteId }.toHashSet()
        val selectedResolved = selected.mapNotNull { canonical[it.normalizedName] }
        for (sublist in tasks.chunked(DbUtils.MAX_SQLITE_ARGS)) {
            val tags = getTagsForTasks(sublist.map(Task::id)).filter { it.tagUid !in keep }
            deleteTags(tags)
            modified.addAll(tags.map(Tag::task))
        }
        for (task in tasks) {
            val currentIds = getTagDataForTask(task.id).mapNotNull { it.remoteId }.toHashSet()
            val added = selectedResolved.filter { it.remoteId !in currentIds }
            if (added.isNotEmpty()) {
                modified.add(task.id)
                insert(
                    added.map {
                        Tag(
                            task = task.id,
                            taskUid = task.uuid,
                            name = it.name,
                            tagUid = it.remoteId
                        )
                    }
                )
            }
        }
        val result = ArrayList(modified)
        if (result.isNotEmpty()) {
            database.taskDao().touch(result)
            database.dirtyDao().setDirtyForTags(result)
        }
        return result
    }

    @Transaction
    open suspend fun updateTag(tagData: TagData): Boolean {
        val clash = getByNormalizedName(tagData.normalizedName)
        if (clash != null && clash.id != tagData.id) {
            return false
        }
        val existing = getByUuid(tagData.remoteId!!)
        if (existing == null) {
            return insertOrIgnore(tagData) != -1L
        }
        val nameChanged = existing.name != tagData.name
        update(tagData)
        if (nameChanged) {
            val tagDao = database.tagDao()
            tagDao.rename(tagData.remoteId, tagData.name!!)
            val affectedTaskIds = tagDao.getByTagUid(tagData.remoteId).map { it.task }
            if (affectedTaskIds.isNotEmpty()) {
                database.dirtyDao().setDirtyForTags(affectedTaskIds)
            }
        }
        return true
    }

    @Transaction
    open suspend fun createDirty(tagData: TagData): TagData? {
        val inserted = insertIfAbsent(tagData) ?: return null
        markDirty(listOf(inserted.id!!))
        deleteTombstones(listOf(inserted.normalizedName))
        return inserted
    }

    private fun mergeEdit(
        current: TagData?,
        remoteId: String,
        name: String,
        color: Int,
        icon: String?,
        order: Int,
        nameChanged: Boolean,
        colorChanged: Boolean,
        iconChanged: Boolean,
    ): TagData =
        (current ?: TagData(remoteId = remoteId, name = name, color = color, icon = icon, order = order)).copy(
            name = if (current == null || nameChanged) name else current.name,
            color = if (current == null || colorChanged) color else current.color,
            icon = if (current == null || iconChanged) icon else current.icon,
        )

    @Transaction
    open suspend fun editTag(
        remoteId: String,
        name: String,
        color: Int,
        icon: String?,
        nameChanged: Boolean,
        colorChanged: Boolean,
        iconChanged: Boolean,
        order: Int = NO_ORDER,
    ): Boolean {
        val current = getByUuid(remoteId)
        val updated = mergeEdit(current, remoteId, name, color, icon, order, nameChanged, colorChanged, iconChanged)
        if (!updateTag(updated)) return false
        val row = getByUuid(remoteId)
        if (row != null) {
            markDirty(listOf(row.id!!))
            deleteTombstones(listOf(row.normalizedName))
        }
        return true
    }

    @Transaction
    open suspend fun renameTag(
        remoteId: String,
        name: String,
        color: Int,
        icon: String?,
        colorChanged: Boolean,
        iconChanged: Boolean,
        order: Int = NO_ORDER,
        queueTombstone: Boolean = true,
    ): TagData? {
        val current = getByUuid(remoteId)
        val oldKey = current?.normalizedName
        val updated = mergeEdit(current, remoteId, name, color, icon, order, nameChanged = true, colorChanged, iconChanged)
        if (!updateTag(updated)) return null
        val row = getByUuid(remoteId) ?: return null
        if (queueTombstone && oldKey != null && oldKey != row.normalizedName) {
            insertTombstone(oldKey, currentTimeMillis())
        }
        markDirty(listOf(row.id!!))
        deleteTombstones(listOf(row.normalizedName))
        return row
    }

    @Transaction
    open suspend fun rescueReaped(picked: Collection<TagData>): List<TagData> {
        if (picked.isEmpty()) return emptyList()
        val keys = picked.map { it.normalizedName }
        val presentBefore = HashSet<String>()
        keys.eachChunk { chunk -> presentBefore.addAll(getByNormalizedNames(chunk).map { it.normalizedName }) }
        insertOrIgnore(picked.map { it.copy(id = null) })
        val resolved = mutableListOf<TagData>()
        keys.eachChunk { chunk -> resolved.addAll(getByNormalizedNames(chunk)) }
        resolved.filter { it.normalizedName !in presentBefore }.mapNotNull { it.id }
            .eachChunk { markDirty(it) }
        resolved.mapNotNull { it.id }.eachChunk { unreap(it) }
        deleteTombstones(keys)
        return resolved
    }

    @Transaction
    open suspend fun delete(tagData: TagData) {
        Logger.d("TagDataDao") { "deleting $tagData" }
        val tagDao = database.tagDao()
        val affectedTaskIds = tagDao.getByTagUid(tagData.remoteId!!).map { it.task }
        deleteTags(tagData.remoteId)
        deleteTagData(tagData)
        if (affectedTaskIds.isNotEmpty()) {
            database.dirtyDao().setDirtyForTags(affectedTaskIds)
        }
    }

    @Transaction
    open suspend fun deleteWithTombstone(tagData: TagData) {
        insertTombstone(tagData.normalizedName, currentTimeMillis())
        delete(tagData)
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

    @Update
    abstract suspend fun update(tagData: TagData)

    @Insert
    abstract suspend fun insert(tag: TagData): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    internal abstract suspend fun insertOrIgnore(tag: TagData): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    internal abstract suspend fun insertOrIgnore(tags: List<TagData>)

    @Insert
    abstract suspend fun insert(tags: Iterable<Tag>)

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


    @Query(
        "SELECT tagdata.*, " +
        "COALESCE(s.reaped, 0) AS reaped, " +
        "(COALESCE(s.dirty_version, 0) > COALESCE(s.synced_version, 0)) AS dirty, " +
        "COALESCE(s.dirty_version, 0) AS dirtyVersion " +
        "FROM tagdata LEFT JOIN metadata_sync_state s ON s.category = '" + METADATA_CATEGORY_TAG + "' AND s.local_id = tagdata._id"
    )
    abstract suspend fun getTagsWithState(): List<TagWithState>

    @Query(
        "SELECT tagdata.*, COALESCE(s.reaped, 0) AS reaped, 1 AS dirty, s.dirty_version AS dirtyVersion " +
        "FROM tagdata INNER JOIN metadata_sync_state s ON s.category = '" + METADATA_CATEGORY_TAG + "' AND s.local_id = tagdata._id " +
        "WHERE s.dirty_version > s.synced_version"
    )
    abstract suspend fun getDirty(): List<TagWithState>

    private suspend fun markDirty(ids: List<Long>) = database.metadataDao().markDirty(METADATA_CATEGORY_TAG, ids)
    private suspend fun healDirty(ids: List<Long>) = database.metadataDao().healDirty(METADATA_CATEGORY_TAG, ids)
    private suspend fun unreap(ids: List<Long>) = database.metadataDao().unreap(METADATA_CATEGORY_TAG, ids)
    private suspend fun setReaped(ids: List<Long>, reaped: Boolean) =
        database.metadataDao().setReaped(METADATA_CATEGORY_TAG, ids, reaped)
    private suspend fun insertTombstone(key: String, ts: Long) =
        database.metadataDao().insertTombstone(MetadataTombstone(METADATA_CATEGORY_TAG, key, ts))
    private suspend fun hasReaped(): Boolean = database.metadataDao().hasReaped(METADATA_CATEGORY_TAG)

    suspend fun hasDirty(): Boolean = database.metadataDao().hasDirty(METADATA_CATEGORY_TAG)
    suspend fun hasTombstones(): Boolean = database.metadataDao().hasTombstones(METADATA_CATEGORY_TAG)
    suspend fun getTombstones(): List<MetadataTombstone> = database.metadataDao().getTombstones(METADATA_CATEGORY_TAG)
    suspend fun getTombstoneKeys(): List<String> = database.metadataDao().getTombstoneKeys(METADATA_CATEGORY_TAG)
    suspend fun deleteTombstones(keys: List<String>) =
        keys.eachChunk { database.metadataDao().deleteTombstones(METADATA_CATEGORY_TAG, it) }
    suspend fun clearAllReaped() = database.metadataDao().clearAllReaped(METADATA_CATEGORY_TAG)
    suspend fun clearAllDirty() = database.metadataDao().clearAllDirty(METADATA_CATEGORY_TAG)
    suspend fun clearAllTombstones() = database.metadataDao().clearAllTombstones(METADATA_CATEGORY_TAG)

    suspend fun clearDirty(ids: List<Long>) =
        ids.eachChunk { database.metadataDao().clearDirty(METADATA_CATEGORY_TAG, it) }

    suspend fun markSynced(id: Long, version: Long) = database.metadataDao().markSynced(METADATA_CATEGORY_TAG, id, version)

    @Query("UPDATE tagdata SET name = COALESCE(:name, name), color = :color, td_icon = :icon WHERE _id = :id")
    internal abstract suspend fun updateDecoration(id: Long, name: String?, color: Int?, icon: String?)

    @Query("UPDATE tagdata SET td_order = :order WHERE normalized_name = :normalized")
    internal abstract suspend fun setOrderByNormalized(normalized: String, order: Int)

    @Query(
        "SELECT tagdata.* FROM tagdata INNER JOIN metadata_sync_state s ON s.category = '" + METADATA_CATEGORY_TAG + "' AND s.local_id = tagdata._id " +
        "WHERE s.reaped = 1 AND NOT EXISTS (SELECT 1 FROM tags WHERE tags.tag_uid = tagdata.remoteId)"
    )
    internal abstract suspend fun getOrphanedTombstones(): List<TagData>

    @Transaction
    open suspend fun reapOrphanedTombstones(): List<TagData> {
        if (!hasReaped()) return emptyList()
        val orphaned = getOrphanedTombstones()
        if (orphaned.isNotEmpty()) delete(orphaned)
        return orphaned
    }

    @Transaction
    open suspend fun applyMetadata(
        live: List<RemoteTagEntry>,
        tombstones: List<String>,
        order: List<String>?,
        base: () -> List<RemoteTagEntry>,
        healMissing: Boolean,
    ) {
        val withState = getTagsWithState()
        val pendingTombstones = getTombstoneKeys().toHashSet()
        val localByKey = withState.filter { it.tag.isSyncable() }.associateBy { it.tag.normalizedName }
        val baseByKey = if (localByKey.values.any { it.dirty }) base().associateBy { it.key } else emptyMap()
        val creates = mutableListOf<TagData>()
        val updates = mutableListOf<TagFieldUpdate>()
        val markReaped = mutableListOf<Long>()
        val clearReaped = mutableListOf<Long>()
        for (e in live) {
            if (e.key in pendingTombstones) continue
            val localState = localByKey[e.key]
            val local = localState?.tag
            when {
                local == null -> {
                    val name = e.name?.takeIf { TagData.normalize(it) == e.key } ?: e.key
                    creates.add(TagData(name = name, color = e.color ?: 0, icon = e.icon))
                }
                else -> {
                    if (localState.reaped) clearReaped.add(local.id!!)
                    val b = if (localState.dirty) baseByKey[e.key] else null
                    val resolvedName = if (localState.dirty) threeWay(local.name, b?.name, e.name) else e.name
                    val resolvedColor =
                        if (localState.dirty) threeWay(normalizeColor(local.color), b?.color, e.color) else e.color
                    val resolvedIcon =
                        if (localState.dirty) threeWay(normalizeIcon(local.icon), b?.icon, e.icon) else e.icon
                    val newName = resolvedName?.takeIf { it != local.name && TagData.normalize(it) == e.key }
                    val localColor = normalizeColor(local.color)
                    val localIcon = normalizeIcon(local.icon)
                    if (newName != null || localColor != resolvedColor || localIcon != resolvedIcon) {
                        updates.add(TagFieldUpdate(local.id!!, local.remoteId, newName, resolvedColor ?: 0, resolvedIcon))
                    }
                }
            }
        }
        for (key in tombstones) {
            localByKey[key]?.takeIf { !it.dirty }?.tag?.id?.let { markReaped.add(it) }
        }
        val redirty = if (healMissing) {
            val present = HashSet<String>(live.size + tombstones.size).apply {
                live.forEach { add(it.key) }
                addAll(tombstones)
            }
            localByKey.values
                .filter { !it.reaped && !it.dirty && it.tag.normalizedName !in present && it.tag.normalizedName !in pendingTombstones }
                .mapNotNull { it.tag.id }
        } else {
            emptyList()
        }
        val orderWrites: List<Pair<String, Int>>? = order?.let { desired ->
            val currentOrder = orderedNormalizedNames(withState)
            if (desired == currentOrder) {
                null
            } else {
                val desiredSet = desired.toHashSet()
                currentOrder.filter { it !in desiredSet }.map { it to NO_ORDER } +
                        desired.mapIndexed { index, key -> key to index }
            }
        }
        applyMetadataMerge(creates, updates, markReaped, clearReaped, orderWrites, redirty)
    }

    @Transaction
    open suspend fun applyMetadataMerge(
        create: List<TagData>,
        update: List<TagFieldUpdate>,
        markReaped: List<Long>,
        clearReaped: List<Long>,
        orderWrites: List<Pair<String, Int>>?,
        markDirty: List<Long> = emptyList(),
    ) {
        if (create.isNotEmpty()) insertOrIgnore(create)
        update.forEach { u ->
            updateDecoration(u.id, u.name, u.color, u.icon)
            if (u.name != null && u.remoteId != null) database.tagDao().rename(u.remoteId, u.name)
        }
        markReaped.eachChunk { setReaped(it, true) }
        clearReaped.eachChunk { setReaped(it, false) }
        markDirty.eachChunk { healDirty(it) }
        orderWrites?.forEach { (normalized, order) -> setOrderByNormalized(normalized, order) }
    }

    suspend fun getTags(task: Task) = ArrayList(if (task.isNew) {
        task.tags.mapNotNull { getTagByName(it) }
    } else {
        getTagDataForTask(task.id)
    })
}
