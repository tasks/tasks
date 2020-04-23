package org.tasks.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import com.todoroo.astrid.api.CustomFilterCriterion
import com.todoroo.astrid.core.CriterionInstance
import com.todoroo.astrid.core.CriterionInstance.*
import org.tasks.R
import org.tasks.activities.FilterSettingsActivity
import org.tasks.filters.FilterCriteriaProvider
import org.tasks.injection.DialogFragmentComponent
import org.tasks.injection.InjectingDialogFragment
import org.tasks.ui.NavigationDrawerFragment
import javax.inject.Inject

class NewFilterDialog : InjectingDialogFragment() {

    @Inject
    lateinit var dialogBuilder: DialogBuilder
    @Inject
    lateinit var provider: FilterCriteriaProvider

    companion object {
        fun newFilterDialog(): NewFilterDialog = NewFilterDialog()

        private val options = arrayOf(
                R.string.repeat_option_custom,
                R.string.filter_overdue,
                R.string.filter_today_only,
                R.string.tomorrow,
                R.string.filter_after_today,
                R.string.filter_any_due_date,
                R.string.no_due_date,
                R.string.filter_no_tags,
                R.string.filter_high_priority,
                R.string.filter_medium_priority,
                R.string.filter_low_priority,
                R.string.filter_no_priority,
                R.string.filter_eisenhower_box_1,
                R.string.filter_eisenhower_box_2,
                R.string.filter_eisenhower_box_3,
                R.string.filter_eisenhower_box_4
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return dialogBuilder.newDialog()
                .setItems(options.map { getString(it) }) { _, which ->
                    newCustomFilter(options[which])
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    private fun newCustomFilter(title: Int) {
        val list = ArrayList<CriterionInstance>()
        when (title) {
            R.string.filter_overdue ->
                list.add(newMultiSelect(provider.dueDateFilter, 1, TYPE_INTERSECT))
            R.string.filter_today_only -> {
                list.add(newMultiSelect(provider.dueDateFilter, 1, TYPE_SUBTRACT))
                list.add(newMultiSelect(provider.dueDateFilter, 2, TYPE_INTERSECT))
            }
            R.string.tomorrow -> {
                list.add(newMultiSelect(provider.dueDateFilter, 2, TYPE_SUBTRACT))
                list.add(newMultiSelect(provider.dueDateFilter, 3, TYPE_INTERSECT))
            }
            R.string.filter_after_today -> {
                list.add(newMultiSelect(provider.dueDateFilter, 0, TYPE_SUBTRACT))
                list.add(newMultiSelect(provider.dueDateFilter, 2, TYPE_SUBTRACT))
            }
            R.string.no_due_date ->
                list.add(newMultiSelect(provider.dueDateFilter, 0, TYPE_INTERSECT))
            R.string.filter_any_due_date -> {
                list.add(newMultiSelect(provider.dueDateFilter, 0, TYPE_SUBTRACT))
            }
            R.string.filter_no_tags ->
                list.add(newText(provider.tagNameContainsFilter, "", TYPE_SUBTRACT))
            R.string.filter_high_priority ->
                list.add(newMultiSelect(provider.priorityFilter, 0, TYPE_INTERSECT))
            R.string.filter_medium_priority -> {
                list.add(newMultiSelect(provider.priorityFilter, 1, TYPE_INTERSECT))
                list.add(newMultiSelect(provider.priorityFilter, 0, TYPE_SUBTRACT))
            }
            R.string.filter_low_priority -> {
                list.add(newMultiSelect(provider.priorityFilter, 2, TYPE_INTERSECT))
                list.add(newMultiSelect(provider.priorityFilter, 1, TYPE_SUBTRACT))
            }
            R.string.filter_no_priority ->
                list.add(newMultiSelect(provider.priorityFilter, 2, TYPE_SUBTRACT))
            R.string.filter_eisenhower_box_1 -> {
                list.add(newMultiSelect(provider.priorityFilter, 2, TYPE_INTERSECT))
                list.add(newMultiSelect(provider.dueDateFilter, 0, TYPE_SUBTRACT))
            }
            R.string.filter_eisenhower_box_2 -> {
                list.add(newMultiSelect(provider.priorityFilter, 2, TYPE_INTERSECT))
                list.add(newMultiSelect(provider.dueDateFilter, 0, TYPE_INTERSECT))
            }
            R.string.filter_eisenhower_box_3 -> {
                list.add(newMultiSelect(provider.priorityFilter, 2, TYPE_SUBTRACT))
                list.add(newMultiSelect(provider.dueDateFilter, 0, TYPE_SUBTRACT))
            }
            R.string.filter_eisenhower_box_4 -> {
                list.add(newMultiSelect(provider.priorityFilter, 2, TYPE_SUBTRACT))
                list.add(newMultiSelect(provider.dueDateFilter, 0, TYPE_INTERSECT))
            }

        }
        val intent = Intent(requireContext(), FilterSettingsActivity::class.java)
        if (list.isNotEmpty()) {
            list.add(0, newMultiSelect(provider.startingUniverse, -1, TYPE_UNIVERSE))
            intent.putExtra(FilterSettingsActivity.EXTRA_TITLE, title)
            intent.putExtra(FilterSettingsActivity.EXTRA_CRITERIA, serialize(list))
        }
        startActivityForResult(intent, NavigationDrawerFragment.REQUEST_NEW_LIST)
        dismiss()
    }

    private fun newMultiSelect(criteria: CustomFilterCriterion, index: Int, type: Int): CriterionInstance {
        val criterion = CriterionInstance()
        criterion.criterion = criteria
        criterion.selectedIndex = index
        criterion.type = type
        return criterion
    }

    private fun newText(criteria: CustomFilterCriterion, text: String, type: Int): CriterionInstance {
        val criterion = CriterionInstance()
        criterion.criterion = criteria
        criterion.selectedText = text
        criterion.type = type
        return criterion
    }

    override fun inject(component: DialogFragmentComponent) = component.inject(this)
}