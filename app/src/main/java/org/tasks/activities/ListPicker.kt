package org.tasks.activities

import android.app.Activity
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.adapter.FilterAdapter
import com.todoroo.astrid.api.CaldavFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.GtasksFilter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.dialogs.DialogBuilder
import org.tasks.filters.FilterProvider
import javax.inject.Inject

@AndroidEntryPoint
class ListPicker : DialogFragment() {
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var filterAdapter: FilterAdapter
    @Inject lateinit var filterProvider: FilterProvider
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    private val refreshReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refresh()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (savedInstanceState != null) {
            filterAdapter.restore(savedInstanceState)
        }
        return createDialog(filterAdapter, dialogBuilder, this::selectedList)
    }

    override fun onResume() {
        super.onResume()
        localBroadcastManager.registerRefreshListReceiver(refreshReceiver)
        refresh()
    }

    override fun onPause() {
        super.onPause()
        localBroadcastManager.unregisterReceiver(refreshReceiver)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        filterAdapter.save(outState)
    }

    private fun selectedList(list: Filter) {
        targetFragment!!.onActivityResult(
                targetRequestCode,
                Activity.RESULT_OK,
                Intent().putExtra(EXTRA_SELECTED_FILTER, list))
    }

    private fun refresh() {
        val noSelection = requireArguments().getBoolean(EXTRA_NO_SELECTION, false)
        val selected: Filter? = if (noSelection) null else arguments?.getParcelable(EXTRA_SELECTED_FILTER)
        lifecycleScope.launch {
            val items = filterProvider.listPickerItems()
            filterAdapter.setData(items, selected)
        }
    }

    companion object {
        const val EXTRA_SELECTED_FILTER = "extra_selected_filter"
        private const val EXTRA_NO_SELECTION = "extra_no_selection"
        fun newListPicker(
                selected: Filter?, targetFragment: Fragment?, requestCode: Int): ListPicker {
            val dialog = ListPicker()
            val arguments = Bundle()
            arguments.putParcelable(EXTRA_SELECTED_FILTER, selected)
            dialog.arguments = arguments
            dialog.setTargetFragment(targetFragment, requestCode)
            return dialog
        }

        fun newListPicker(targetFragment: Fragment?, requestCode: Int): ListPicker {
            val dialog = ListPicker()
            val arguments = Bundle()
            arguments.putBoolean(EXTRA_NO_SELECTION, true)
            dialog.arguments = arguments
            dialog.setTargetFragment(targetFragment, requestCode)
            return dialog
        }

        private fun createDialog(
                filterAdapter: FilterAdapter,
                dialogBuilder: DialogBuilder,
                handler: (Filter) -> Unit): AlertDialog {
            val builder = dialogBuilder
                    .newDialog()
                    .setNegativeButton(R.string.cancel, null)
                    .setSingleChoiceItems(filterAdapter,-1) { dialog: DialogInterface, which: Int ->
                        val item = filterAdapter.getItem(which)
                        if (item is GtasksFilter || item is CaldavFilter) {
                            handler(item as Filter)
                        }
                        dialog.dismiss()
                    }
            return builder.show()
        }
    }
}