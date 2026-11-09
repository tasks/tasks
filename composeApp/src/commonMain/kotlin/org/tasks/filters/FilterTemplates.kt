package org.tasks.filters

import org.jetbrains.compose.resources.StringResource
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.filter_after_today
import tasks.kmp.generated.resources.filter_any_due_date
import tasks.kmp.generated.resources.filter_any_start_date
import tasks.kmp.generated.resources.filter_eisenhower_box_1
import tasks.kmp.generated.resources.filter_eisenhower_box_2
import tasks.kmp.generated.resources.filter_eisenhower_box_3
import tasks.kmp.generated.resources.filter_eisenhower_box_4
import tasks.kmp.generated.resources.filter_high_priority
import tasks.kmp.generated.resources.filter_low_priority
import tasks.kmp.generated.resources.filter_medium_priority
import tasks.kmp.generated.resources.filter_no_priority
import tasks.kmp.generated.resources.filter_no_tags
import tasks.kmp.generated.resources.filter_overdue
import tasks.kmp.generated.resources.filter_today_only
import tasks.kmp.generated.resources.no_due_date
import tasks.kmp.generated.resources.no_start_date
import tasks.kmp.generated.resources.repeat_option_custom
import tasks.kmp.generated.resources.tomorrow

/**
 * The canned filter templates shown when the user starts a new custom filter
 * (desktop port of `org.tasks.dialogs.NewFilterDialog` from the Android `app`
 * module).
 *
 * The template order and the criterion composition for each option are a
 * copy of the Android original, so a "Tomorrow" filter looks and behaves the
 * same on both platforms. [criteria] returns the rows WITHOUT the leading
 * universe row; [withUniverse] prepends it, mirroring the Android flow where
 * the editor always shows the "My Tasks" universe as the locked first row.
 */
object FilterTemplates {

    // Option order matches NewFilterDialog.options.
    private const val CUSTOM = 0
    private const val OVERDUE = 1
    private const val TODAY_ONLY = 2
    private const val TOMORROW = 3
    private const val AFTER_TODAY = 4
    private const val ANY_START_DATE = 5
    private const val NO_START_DATE = 6
    private const val ANY_DUE_DATE = 7
    private const val NO_DUE_DATE = 8
    private const val NO_TAGS = 9
    private const val HIGH_PRIORITY = 10
    private const val MEDIUM_PRIORITY = 11
    private const val LOW_PRIORITY = 12
    private const val NO_PRIORITY = 13
    private const val EISENHOWER_BOX_1 = 14
    private const val EISENHOWER_BOX_2 = 15
    private const val EISENHOWER_BOX_3 = 16
    private const val EISENHOWER_BOX_4 = 17

    /** Display names, in [count] order. */
    val names: List<StringResource> = listOf(
        Res.string.repeat_option_custom,
        Res.string.filter_overdue,
        Res.string.filter_today_only,
        Res.string.tomorrow,
        Res.string.filter_after_today,
        Res.string.filter_any_start_date,
        Res.string.no_start_date,
        Res.string.filter_any_due_date,
        Res.string.no_due_date,
        Res.string.filter_no_tags,
        Res.string.filter_high_priority,
        Res.string.filter_medium_priority,
        Res.string.filter_low_priority,
        Res.string.filter_no_priority,
        Res.string.filter_eisenhower_box_1,
        Res.string.filter_eisenhower_box_2,
        Res.string.filter_eisenhower_box_3,
        Res.string.filter_eisenhower_box_4,
    )

    val count: Int get() = names.size

    /**
     * The criteria rows for the chosen [which] template (index into [names]);
     * empty for the "Custom" option. Matches `NewFilterDialog.newCustomFilter`.
     */
    suspend fun criteria(which: Int, provider: FilterCriteriaProvider): List<CriterionInstance> {
        val list = mutableListOf<CriterionInstance>()
        when (which) {
            OVERDUE ->
                list.add(multiSelect(provider.dueDateFilter(), 7, CriterionInstance.TYPE_INTERSECT))
            TODAY_ONLY -> {
                list.add(multiSelect(provider.dueDateFilter(), 1, CriterionInstance.TYPE_SUBTRACT))
                list.add(multiSelect(provider.dueDateFilter(), 2, CriterionInstance.TYPE_INTERSECT))
            }
            TOMORROW -> {
                list.add(multiSelect(provider.dueDateFilter(), 2, CriterionInstance.TYPE_SUBTRACT))
                list.add(multiSelect(provider.dueDateFilter(), 3, CriterionInstance.TYPE_INTERSECT))
            }
            AFTER_TODAY -> {
                list.add(multiSelect(provider.dueDateFilter(), 0, CriterionInstance.TYPE_SUBTRACT))
                list.add(multiSelect(provider.dueDateFilter(), 2, CriterionInstance.TYPE_SUBTRACT))
            }
            NO_START_DATE ->
                list.add(multiSelect(provider.startDateFilter(), 0, CriterionInstance.TYPE_INTERSECT))
            ANY_START_DATE ->
                list.add(multiSelect(provider.startDateFilter(), 0, CriterionInstance.TYPE_SUBTRACT))
            NO_DUE_DATE ->
                list.add(multiSelect(provider.dueDateFilter(), 0, CriterionInstance.TYPE_INTERSECT))
            ANY_DUE_DATE ->
                list.add(multiSelect(provider.dueDateFilter(), 0, CriterionInstance.TYPE_SUBTRACT))
            NO_TAGS ->
                list.add(text(provider.tagNameContainsFilter(), "", CriterionInstance.TYPE_SUBTRACT))
            HIGH_PRIORITY ->
                list.add(multiSelect(provider.priorityFilter(), 0, CriterionInstance.TYPE_INTERSECT))
            MEDIUM_PRIORITY -> {
                list.add(multiSelect(provider.priorityFilter(), 1, CriterionInstance.TYPE_INTERSECT))
                list.add(multiSelect(provider.priorityFilter(), 0, CriterionInstance.TYPE_SUBTRACT))
            }
            LOW_PRIORITY -> {
                list.add(multiSelect(provider.priorityFilter(), 2, CriterionInstance.TYPE_INTERSECT))
                list.add(multiSelect(provider.priorityFilter(), 1, CriterionInstance.TYPE_SUBTRACT))
            }
            NO_PRIORITY ->
                list.add(multiSelect(provider.priorityFilter(), 2, CriterionInstance.TYPE_SUBTRACT))
            EISENHOWER_BOX_1 -> {
                list.add(multiSelect(provider.priorityFilter(), 2, CriterionInstance.TYPE_INTERSECT))
                list.add(multiSelect(provider.dueDateFilter(), 0, CriterionInstance.TYPE_SUBTRACT))
            }
            EISENHOWER_BOX_2 -> {
                list.add(multiSelect(provider.priorityFilter(), 2, CriterionInstance.TYPE_INTERSECT))
                list.add(multiSelect(provider.dueDateFilter(), 0, CriterionInstance.TYPE_INTERSECT))
            }
            EISENHOWER_BOX_3 -> {
                list.add(multiSelect(provider.priorityFilter(), 2, CriterionInstance.TYPE_SUBTRACT))
                list.add(multiSelect(provider.dueDateFilter(), 0, CriterionInstance.TYPE_SUBTRACT))
            }
            EISENHOWER_BOX_4 -> {
                list.add(multiSelect(provider.priorityFilter(), 2, CriterionInstance.TYPE_SUBTRACT))
                list.add(multiSelect(provider.dueDateFilter(), 0, CriterionInstance.TYPE_INTERSECT))
            }
            // CUSTOM (0) and any unlisted index build no rows; all other
            // templates are handled above. The `when` is a statement, not an
            // expression, so it need not be exhaustive.
            CUSTOM -> Unit
        }
        return list
    }

    /**
     * Prepends the locked "My Tasks" universe row, as the Android editor always
     * does before showing a new filter's criteria. Returns the input list
     * unchanged when it is empty (the "Custom" template).
     */
    suspend fun withUniverse(
        which: Int,
        provider: FilterCriteriaProvider,
    ): List<CriterionInstance> {
        val list = criteria(which, provider).toMutableList()
        if (list.isNotEmpty()) {
            list.add(0, multiSelect(provider.startingUniverse(), -1, CriterionInstance.TYPE_UNIVERSE))
        }
        return list
    }

    private fun multiSelect(
        criterion: CustomFilterCriterion,
        index: Int,
        type: Int,
    ): CriterionInstance =
        CriterionInstance().apply {
            this.criterion = criterion
            selectedIndex = index
            this.type = type
        }

    private fun text(
        criterion: CustomFilterCriterion,
        text: String,
        type: Int,
    ): CriterionInstance =
        CriterionInstance().apply {
            this.criterion = criterion
            selectedText = text
            this.type = type
        }
}
