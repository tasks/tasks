package org.tasks.activities

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.astrid.api.Filter
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.compose.pickers.FilterPicker
import org.tasks.dialogs.DialogBuilder
import javax.inject.Inject

@AndroidEntryPoint
class FilterPicker : DialogFragment() {
    @Inject lateinit var dialogBuilder: DialogBuilder

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return dialogBuilder
            .newDialog()
            .setNegativeButton(R.string.cancel, null)
            .setContent {
                FilterPicker(
                    selected = arguments?.getParcelable(EXTRA_FILTER),
                    onSelected = { filter ->
                        val data = Bundle()
                        data.putParcelable(EXTRA_FILTER, filter)
                        if (filter.valuesForNewTasks != null) {
                            data.putString(
                                EXTRA_FILTER_VALUES,
                                AndroidUtilities.mapToSerializedString(filter.valuesForNewTasks))
                        }
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

        fun newFilterPicker(
            selected: Filter?,
        ): FilterPicker {
            val dialog = FilterPicker()
            val arguments = Bundle()
            arguments.putParcelable(EXTRA_FILTER, selected)
            dialog.arguments = arguments
            return dialog
        }
    }
}