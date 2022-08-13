package org.tasks.activities

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.todoroo.astrid.api.Filter
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.compose.pickers.ListPicker
import org.tasks.dialogs.DialogBuilder
import javax.inject.Inject

@AndroidEntryPoint
class ListPicker : DialogFragment() {
    @Inject lateinit var dialogBuilder: DialogBuilder

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return dialogBuilder
            .newDialog()
            .setNegativeButton(R.string.cancel, null)
            .setContent {
                ListPicker(
                    selected = arguments?.getParcelable(EXTRA_SELECTED_FILTER),
                    onSelected = {
                        targetFragment!!.onActivityResult(
                            targetRequestCode,
                            Activity.RESULT_OK,
                            Intent().putExtra(EXTRA_SELECTED_FILTER, it)
                        )
                        dismiss()
                    },
                )
            }
            .show()
    }

    companion object {
        const val EXTRA_SELECTED_FILTER = "extra_selected_filter"
        fun newListPicker(
                selected: Filter?, targetFragment: Fragment?, requestCode: Int): ListPicker {
            val dialog = ListPicker()
            val arguments = Bundle()
            arguments.putParcelable(EXTRA_SELECTED_FILTER, selected)
            dialog.arguments = arguments
            dialog.setTargetFragment(targetFragment, requestCode)
            return dialog
        }
    }
}