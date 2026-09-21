package org.tasks.caldav.metadata

import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.TagData
import org.tasks.data.NO_ORDER
import org.tasks.filters.TagFilter
import org.tasks.preferences.FilterPreferences.Companion.delete
import org.tasks.filters.key
import org.tasks.preferences.TasksPreferences

open class TagMetadataEditor(
    private val caldavDao: CaldavDao,
    private val tagDataDao: TagDataDao,
    protected val preferences: TasksPreferences,
) {
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

    suspend fun finalizeDeletedTags(tags: List<TagData>) {
        preferences.delete(tags.map { TagFilter(it).key() })
    }

    protected suspend fun preferredPrimaryId(): Long =
        preferences.get(TasksPreferences.metadataPrimaryAccount, 0L)
}
