package org.tasks.compose.edit

import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.EPr_edit_section_description
import tasks.kmp.generated.resources.EPr_edit_section_priority
import tasks.kmp.generated.resources.EPr_edit_section_repeat
import tasks.kmp.generated.resources.EPr_edit_section_reminders
import tasks.kmp.generated.resources.EPr_edit_section_tags
import tasks.kmp.generated.resources.due_date
import tasks.kmp.generated.resources.sort_list
import tasks.kmp.generated.resources.start_date
import tasks.kmp.generated.resources.subtasks

/**
 * The customisable blocks of the desktop task edit screen, in their default
 * (canonical) order. The title and the save/discard action bar are pinned and
 * never part of this list.
 *
 * [name] doubles as the persisted id, so a row's identity is stable across
 * versions: when a future change adds a section, new installs and existing
 * users both pick it up through [EditScreenSections.merge] without migration.
 */
enum class EditScreenSection(
    val titleRes: StringResource,
) {
    LIST(Res.string.sort_list),
    START_DATE(Res.string.start_date),
    DUE_DATE(Res.string.due_date),
    REMINDERS(Res.string.EPr_edit_section_reminders),
    REPEATS(Res.string.EPr_edit_section_repeat),
    TAGS(Res.string.EPr_edit_section_tags),
    PRIORITY(Res.string.EPr_edit_section_priority),
    DESCRIPTION(Res.string.EPr_edit_section_description),
    SUBTASKS(Res.string.subtasks),
}

/**
 * One persisted row of the "Customize edit screen" list.
 */
@Serializable
data class EditScreenSectionPref(
    val id: String,
    val enabled: Boolean = true,
)

/**
 * One row of the "Customize edit screen" screen: a section plus whether it
 * currently renders on the edit screen.
 */
data class EditScreenSectionItem(
    val section: EditScreenSection,
    val enabled: Boolean,
) {
    val id: String get() = section.name
}

object EditScreenSections {

    /** Every section the current build knows about, in default order. */
    val CANONICAL: List<EditScreenSection> = EditScreenSection.entries

    /**
     * Merge a stored (possibly stale or partial) customization with the
     * current canonical list:
     *
     * - ids the build no longer knows about are dropped;
     * - sections added since the value was stored are appended, enabled;
     * - an absent/empty stored value yields the canonical list, all enabled.
     */
    fun merge(stored: List<EditScreenSectionPref>): List<EditScreenSectionItem> {
        val items = stored.mapNotNull { pref ->
            EditScreenSection.entries.firstOrNull { it.name == pref.id }
                ?.let { EditScreenSectionItem(it, pref.enabled) }
        }
        val known = items.mapTo(mutableSetOf()) { it.section }
        return items + CANONICAL.filter { it !in known }.map { EditScreenSectionItem(it, true) }
    }
}
