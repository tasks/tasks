package org.tasks.caldav.metadata

import co.touchlab.kermit.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl
import org.tasks.caldav.CaldavClient
import org.tasks.caldav.CaldavClientProvider
import org.tasks.caldav.VtodoCache
import org.tasks.caldav.supportsDeadProperties
import org.tasks.data.UUIDHelper
import org.tasks.data.dao.CaldavDao
import org.tasks.data.NO_ORDER
import org.tasks.data.dao.RemoteTagEntry
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TagWithState
import org.tasks.data.dao.orderedNormalizedNames
import org.tasks.data.dao.threeWay
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.TagData
import org.tasks.data.entity.isSyncable
import org.tasks.data.entity.normalizeColor
import org.tasks.data.entity.normalizeIcon
import org.tasks.filters.TagFilter
import org.tasks.filters.key
import org.tasks.preferences.FilterPreferences.Companion.delete
import org.tasks.preferences.TasksPreferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis

private const val TAG = "TagMetadataSync"

private fun TagData.toEntry() = RemoteTagEntry(normalizedName, name, normalizeColor(color), normalizeIcon(icon))

class TagMetadataSync(
    private val caldavDao: CaldavDao,
    private val tagDataDao: TagDataDao,
    private val provider: CaldavClientProvider,
    private val vtodoCache: VtodoCache,
    private val preferences: TasksPreferences,
) {
    private val mutex = Mutex()

    private var principalCache: Triple<Long, String?, HttpUrl>? = null

    private suspend fun resolvePrincipal(account: CaldavAccount, client: CaldavClient): HttpUrl? {
        principalCache?.let { (id, url, p) -> if (id == account.id && url == account.url) return p }
        return client.principal()?.also { principalCache = Triple(account.id!!, account.url, it) }
    }

    suspend fun primaryAccount(): CaldavAccount? = caldavDao.getMetadataPrimary(preferredPrimaryId())

    suspend fun isPrimary(account: CaldavAccount): Boolean = primaryAccount()?.id == account.id

    data class ToggleState(
        val visible: Boolean = false,
        val checked: Boolean = false,
        val interactable: Boolean = true,
        val forcedByTasksOrg: Boolean = false,
        val otherPrimary: String? = null,
    )

    suspend fun toggleState(account: CaldavAccount): ToggleState {
        if (!account.isCaldavAccount) return ToggleState()
        val primary = primaryAccount()
        val forced = primary?.accountType == CaldavAccount.TYPE_TASKS
        return ToggleState(
            visible = true,
            checked = primary?.id == account.id,
            interactable = !forced,
            forcedByTasksOrg = forced,
            otherPrimary = primary
                ?.takeIf { it.id != account.id }
                ?.let { it.name ?: it.username ?: "" },
        )
    }

    suspend fun newAccountToggleState(): ToggleState {
        val primary = primaryAccount()
        val forced = primary?.accountType == CaldavAccount.TYPE_TASKS
        return ToggleState(
            visible = true,
            checked = false,
            interactable = !forced,
            forcedByTasksOrg = forced,
            otherPrimary = primary?.let { it.name ?: it.username ?: "" },
        )
    }

    class Pulled internal constructor(
        val applied: Boolean,
        internal val store: TagMetadataBlob?,
    )

    suspend fun sync(account: CaldavAccount, client: CaldavClient): Boolean {
        val pulled = pullMetadata(account, client) ?: return false
        return pushAndReap(account, client, pulled)
    }

    suspend fun pullMetadata(account: CaldavAccount, client: CaldavClient): Pulled? {
        val principal = resolvePrincipal(account, client) ?: run {
            Logger.w(tag = TAG) { "could not resolve principal for $account; skipping metadata this run" }
            return null
        }
        return pull(account, client, principal)
    }

    suspend fun pushAndReap(account: CaldavAccount, client: CaldavClient, pulled: Pulled): Boolean {
        val principal = resolvePrincipal(account, client) ?: return false
        pushDirty(account, client, principal, pulled.store)
        if (!pulled.applied) return false
        val reaped = reapOrphaned(account)
        finalizeDeletedTags(reaped)
        return reaped.isNotEmpty()
    }

    suspend fun finalizeDeletedTags(tags: List<TagData>) {
        preferences.delete(tags.map { TagFilter(it).key() })
    }

    suspend fun applyRemote(remoteJson: String?) {
        val blob = TagMetadataBlob.parse(remoteJson) ?: return
        applyBlob(blob, base = { emptyList() }, healMissing = false)
    }

    private suspend fun applyBlob(
        blob: TagMetadataBlob,
        base: () -> List<RemoteTagEntry>,
        healMissing: Boolean,
        applyOrder: Boolean = true,
    ) {
        val live = mutableListOf<RemoteTagEntry>()
        val tombstones = mutableListOf<String>()
        for (key in blob.keys) {
            when (val entry = blob.entryOf(key)) {
                is Entry.Live -> live.add(RemoteTagEntry(key, entry.name, entry.color, entry.icon))
                is Entry.Tomb -> tombstones.add(key)
                null -> {}
            }
        }
        tagDataDao.applyMetadata(live, tombstones, blob.order.takeIf { applyOrder }, base, healMissing)
    }

    private fun liveEntries(json: String?): List<RemoteTagEntry> {
        val blob = TagMetadataBlob.parse(json) ?: return emptyList()
        return blob.keys.mapNotNull { key ->
            (blob.entryOf(key) as? Entry.Live)?.let { RemoteTagEntry(key, it.name, it.color, it.icon) }
        }
    }

    suspend fun reapOrphaned(account: CaldavAccount): List<TagData> =
        withPrimaryLock(account, emptyList()) { tagDataDao.reapOrphanedTombstones() }

    suspend fun pull(account: CaldavAccount, client: CaldavClient, principal: HttpUrl): Pulled {
        if (!holdsStore(account)) {
            if (!withPrimaryLock(account, false) { adoptStore(account); true }) return Pulled(false, null)
        }
        val cachedRev = rev()
        val cheapVersion = client.tagMetadataVersion(principal)
        if (cheapVersion != null && cachedRev == cheapVersion) {
            if (!hasPendingPush()) return Pulled(true, null)
            return withPrimaryLock(account, Pulled(false, null)) {
                Pulled(true, TagMetadataBlob.parse(vtodoCache.getTagMetadata(account)))
            }
        }
        return withPrimaryLock(account, Pulled(false, null)) {
            val cached = vtodoCache.getTagMetadata(account)
            val payload = client.tagMetadata(principal)
            val remote = TagMetadataBlob.parse(payload)
            when {
                payload == null ->
                    if (cached == null) seed(client, principal, account).toPulled() else Pulled(false, null)
                remote == null -> Pulled(false, null)
                cached == null -> firstAdopt(client, principal, account, remote, payload).toPulled()
                else -> {
                    applyBlob(remote, base = { liveEntries(cached) }, healMissing = true)
                    cache(account, payload, remote.rev)
                    Pulled(true, remote)
                }
            }
        }
    }

    private fun TagMetadataBlob?.toPulled() = Pulled(this != null, this)

    suspend fun deleteTag(tag: TagData) {
        if (primaryAccount() != null) tagDataDao.deleteWithTombstone(tag) else tagDataDao.delete(tag)
        finalizeDeletedTags(listOf(tag))
    }

    suspend fun renameTag(
        remoteId: String,
        name: String,
        color: Int,
        icon: String?,
        colorChanged: Boolean,
        iconChanged: Boolean,
        order: Int = NO_ORDER,
    ): TagData? = tagDataDao.renameTag(
        remoteId, name, color, icon, colorChanged, iconChanged, order,
        queueTombstone = primaryAccount() != null,
    )

    suspend fun markOrderDirty() = preferences.set(TasksPreferences.metadataOrderDirty, true)

    suspend fun pushDirty(
        account: CaldavAccount,
        client: CaldavClient,
        principal: HttpUrl,
        store: TagMetadataBlob? = null,
    ) {
        val orderDirty = orderDirty()
        if (!orderDirty && !tagDataDao.hasDirty() && !tagDataDao.hasTombstones()) return
        withPrimaryLock(account, Unit) {
            ensureStoreHeld(account)
            val allDirty = tagDataDao.getDirty()
            val dirty = allDirty.filter { it.tag.isSyncable() }
            val unpushable = allDirty.filterNot { it.tag.isSyncable() }.mapNotNull { it.tag.id }
            if (unpushable.isNotEmpty()) tagDataDao.clearDirty(unpushable)
            val tombstones = tagDataDao.getTombstones()
            if (dirty.isEmpty() && tombstones.isEmpty() && !orderDirty) return@withPrimaryLock
            val liveVersion = client.tagMetadataVersion(principal)
            val current = store?.takeIf { it.rev != null && it.rev == liveVersion }
                ?: currentBlob(client, principal, account, liveVersion)
            val mergeBase =
                if (dirty.isEmpty()) emptyMap() else liveEntries(vtodoCache.getTagMetadata(account)).associateBy { it.key }
            val overlay = LinkedHashMap<String, JsonObject?>()
            dirty.forEach {
                val key = it.tag.normalizedName
                val cur = current.entryOf(key) as? Entry.Live
                overlay[key] = if (cur == null) {
                    current.liveEntry(key, it.tag.name, it.tag.color, it.tag.icon)
                } else {
                    val b = mergeBase[key]
                    current.liveEntry(
                        key,
                        threeWay(it.tag.name, b?.name, cur.name),
                        threeWay(normalizeColor(it.tag.color), b?.color, cur.color),
                        threeWay(normalizeIcon(it.tag.icon), b?.icon, cur.icon),
                    )
                }
            }
            tombstones.forEach { overlay[it.key] = TagMetadataBlob.tombstone(it.ts) }
            val order = orderedKeys(syncableTags())
            val pushed = pushPayload(client, principal, current.overlaid(overlay, order)) ?: return@withPrimaryLock
            applyBlob(pushed.blob, base = { dirty.map { it.tag.toEntry() } }, healMissing = false, applyOrder = false)
            dirty.forEach { tagDataDao.markSynced(it.tag.id!!, it.dirtyVersion) }
            if (tombstones.isNotEmpty()) tagDataDao.deleteTombstones(tombstones.map { it.key })
            if (orderDirty && orderedKeys(syncableTags()) == order) clearOrderDirty()
            cache(account, pushed.json, pushed.rev)
        }
    }

    suspend fun probeViability(url: String, username: String, password: String): Boolean {
        val client = provider.forUrl(url, username, password)
        val principal = client.principal() ?: return false
        if (TagMetadataBlob.parse(client.tagMetadata(principal)) != null) return true
        return client.supportsDeadProperties(principal)
    }

    suspend fun enablePrimary(account: CaldavAccount, skipProbe: Boolean = false): Boolean = mutex.withLock {
        val client = provider.forAccount(account)
        val principal = client.principal() ?: return@withLock false
        val existingPayload = client.tagMetadata(principal)
        val existing = TagMetadataBlob.parse(existingPayload)
        if (existing != null) {
            resetHeldStore(clearDirty = false)
            firstAdopt(client, principal, account, existing, existingPayload!!) ?: return@withLock false
            markPrimary(account)
            return@withLock true
        }
        if (!skipProbe && !client.supportsDeadProperties(principal)) return@withLock false
        resetHeldStore(clearDirty = false)
        seed(client, principal, account) ?: return@withLock false
        markPrimary(account)
        true
    }

    suspend fun disable() = mutex.withLock {
        resetHeldStore(clearDirty = true)
        preferences.set(TasksPreferences.metadataPrimaryAccount, 0L)
    }

    private suspend fun seed(client: CaldavClient, principal: HttpUrl, account: CaldavAccount): TagMetadataBlob? {
        val snapshot = syncableTags()
        val tombstoneKeys = tagDataDao.getTombstoneKeys()
        val payload = localPayload(snapshot, tombstoneKeys)
        val pushed = pushPayload(client, principal, payload) ?: return null
        cache(account, pushed.json, pushed.rev)
        clearDirtyReconciled(snapshot)
        if (tombstoneKeys.isNotEmpty()) tagDataDao.deleteTombstones(tombstoneKeys)
        if (orderedKeys(syncableTags()) == orderedKeys(snapshot)) clearOrderDirty()
        return pushed.blob
    }

    private suspend fun firstAdopt(
        client: CaldavClient,
        principal: HttpUrl,
        account: CaldavAccount,
        remote: TagMetadataBlob,
        remotePayload: String,
    ): TagMetadataBlob? {
        val snapshot = syncableTags()
        val tombstoneKeys = tagDataDao.getTombstoneKeys()
        val liveLocal = snapshot.filter { !it.reaped }.map { it.tag.toEntry() }
        val dirtyKeys = snapshot.filter { it.dirty }.map { it.tag.normalizedName }.toSet()
        val union = remote.overlayLocal(liveLocal, dirtyKeys)
        val tombMap: Map<String, JsonObject?> =
            tombstoneKeys.associateWith { TagMetadataBlob.tombstone(currentTimeMillis()) }
        val merged: JsonObject? = when {
            tombstoneKeys.isEmpty() -> union
            union == null -> remote.overlaid(tombMap)
            else -> TagMetadataBlob.of(union).overlaid(tombMap)
        }
        val base = { snapshot.filter { it.dirty }.map { it.tag.toEntry() } }
        val applied = if (merged == null) {
            applyBlob(remote, base, healMissing = false)
            cache(account, remotePayload, remote.rev)
            remote
        } else {
            val pushed = pushPayload(client, principal, merged) ?: return null
            applyBlob(pushed.blob, base, healMissing = false)
            cache(account, pushed.json, pushed.rev)
            pushed.blob
        }
        clearDirtyReconciled(snapshot)
        if (tombstoneKeys.isNotEmpty() && merged != null) tagDataDao.deleteTombstones(tombstoneKeys)
        return applied
    }

    private suspend fun clearDirtyReconciled(snapshot: List<TagWithState>) {
        snapshot.filter { it.dirty }.forEach { tagDataDao.markSynced(it.tag.id!!, it.dirtyVersion) }
    }

    private suspend fun buildLocalPayload(): JsonObject =
        localPayload(syncableTags(), tagDataDao.getTombstoneKeys())

    private fun localPayload(snapshot: List<TagWithState>, tombstoneKeys: List<String>): JsonObject {
        val entries = snapshot.filter { !it.reaped }.map { it.tag.toEntry() }
        return TagMetadataBlob.build(entries, orderedKeys(snapshot), tombstoneKeys, currentTimeMillis())
    }

    private suspend fun currentBlob(
        client: CaldavClient,
        principal: HttpUrl,
        account: CaldavAccount,
        remoteVersion: String?,
    ): TagMetadataBlob {
        if (remoteVersion != null && rev() == remoteVersion) {
            TagMetadataBlob.parse(vtodoCache.getTagMetadata(account))?.let { return it }
        }
        return TagMetadataBlob.parse(client.tagMetadata(principal)) ?: TagMetadataBlob.of(buildLocalPayload())
    }

    private class Pushed(val blob: TagMetadataBlob, val json: String, val rev: String)

    private suspend fun pushPayload(client: CaldavClient, principal: HttpUrl, json: JsonObject): Pushed? {
        val rev = UUIDHelper.newUUID()
        val stamped = stampRev(json, rev)
        val payload = stamped.toString()
        return if (client.pushTagMetadata(principal, payload, rev)) {
            Pushed(TagMetadataBlob.of(stamped), payload, rev)
        } else {
            null
        }
    }

    private suspend fun cache(account: CaldavAccount, payload: String, rev: String?) {
        vtodoCache.putTagMetadata(account, payload)
        preferences.set(TasksPreferences.metadataRev, rev ?: "")
    }

    private suspend fun resetHeldStore(clearDirty: Boolean) {
        val heldId = preferences.get(TasksPreferences.metadataStoreAccount, 0L)
        if (heldId != 0L) caldavDao.getAccount(heldId)?.let { vtodoCache.putTagMetadata(it, null) }
        tagDataDao.clearAllReaped()
        if (clearDirty) {
            tagDataDao.clearAllDirty()
            tagDataDao.clearAllTombstones()
        }
        preferences.set(TasksPreferences.metadataRev, "")
        preferences.set(TasksPreferences.metadataStoreAccount, 0L)
        clearOrderDirty()
    }

    private suspend fun adoptStore(account: CaldavAccount) {
        resetHeldStore(clearDirty = false)
        vtodoCache.putTagMetadata(account, null)
        preferences.set(TasksPreferences.metadataStoreAccount, account.id!!)
    }

    private suspend fun ensureStoreHeld(account: CaldavAccount) {
        if (!holdsStore(account)) adoptStore(account)
    }

    private suspend fun holdsStore(account: CaldavAccount): Boolean =
        preferences.get(TasksPreferences.metadataStoreAccount, 0L) == account.id

    private suspend fun markPrimary(account: CaldavAccount) {
        preferences.set(TasksPreferences.metadataPrimaryAccount, account.id!!)
        preferences.set(TasksPreferences.metadataStoreAccount, account.id!!)
    }

    private suspend fun preferredPrimaryId(): Long =
        preferences.get(TasksPreferences.metadataPrimaryAccount, 0L)

    private suspend fun rev(): String? =
        preferences.get(TasksPreferences.metadataRev, "").ifEmpty { null }

    private suspend fun orderDirty(): Boolean = preferences.get(TasksPreferences.metadataOrderDirty, false)

    private suspend fun clearOrderDirty() = preferences.set(TasksPreferences.metadataOrderDirty, false)

    private suspend fun hasPendingPush(): Boolean =
        orderDirty() || tagDataDao.hasDirty() || tagDataDao.hasTombstones()

    private suspend fun syncableTags(): List<TagWithState> =
        tagDataDao.getTagsWithState().filter { it.tag.isSyncable() }

    private fun orderedKeys(tags: List<TagWithState>): List<String> = orderedNormalizedNames(tags)

    private suspend fun <T> withPrimaryLock(account: CaldavAccount, onLost: T, block: suspend () -> T): T =
        mutex.withLock {
            if (primaryAccount()?.id != account.id) onLost else block()
        }
}
