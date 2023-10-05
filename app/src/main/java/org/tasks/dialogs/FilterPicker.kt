package org.tasks.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.LifecycleOwner
import com.todoroo.astrid.api.Filter
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.compose.pickers.FilterPicker
import javax.inject.Inject

@AndroidEntryPoint
class FilterPicker : DialogFragment() {
    @Inject lateinit var dialogBuilder: DialogBuilder

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return dialogBuilder
            .newDialog()
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
            .setContent {
                FilterPicker(
                    selected = arguments?.getParcelable(EXTRA_FILTER),
                    onSelected = { filter ->
                        val data = Bundle()
                        data.putParcelable(EXTRA_FILTER, filter)
                        filter.valuesForNewTasks?.let { data.putString(EXTRA_FILTER_VALUES, it) }
                        setFragmentResult(SELECT_FILTER, data)
                        dismiss()
                    }
                )
            }
            .show()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        setFragmentResult(SELECT_FILTER, Bundle())
    }

    companion object {
        const val SELECT_FILTER = "select_filter"
        const val EXTRA_FILTER = "extra_filter"
        const val EXTRA_FILTER_VALUES = "extra_filter_values"
        const val EXTRA_LISTS_ONLY = "extra_lists_only"

        fun newFilterPicker(
            selected: Filter?,
            listsOnly: Boolean = false,
        ): FilterPicker {
            val dialog = FilterPicker()
            val arguments = Bundle()
            arguments.putParcelable(EXTRA_FILTER, selected)
            arguments.putBoolean(EXTRA_LISTS_ONLY, listsOnly)
            dialog.arguments = arguments
            return dialog
        }

        fun FragmentManager.setFilterPickerResultListener(
            lifecycleOwner: LifecycleOwner,
            callback: (Filter) -> Unit
        ) {
            setFragmentResultListener(SELECT_FILTER, lifecycleOwner) { _, data ->
                data.getParcelable<Filter>(EXTRA_FILTER)?.let(callback)
            }
        }
    }
}