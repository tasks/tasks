package org.tasks.ui

import android.app.Dialog
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.todoroo.astrid.adapter.NavigationDrawerAdapter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.FilterListItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.tasks.R
import org.tasks.billing.PurchaseActivity
import org.tasks.extensions.Context.openUri
import org.tasks.filters.NavigationDrawerAction
import org.tasks.intents.TaskIntents
import org.tasks.preferences.Preferences
import javax.inject.Inject

@AndroidEntryPoint
class NavigationDrawerFragment : BottomSheetDialogFragment() {
    @Inject lateinit var adapter: NavigationDrawerAdapter
    @Inject lateinit var preferences: Preferences

    override fun getTheme() = R.style.CustomBottomSheetDialog

    private lateinit var recyclerView: RecyclerView
    private val viewModel: NavigationDrawerViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getParcelable<Filter>(EXTRA_SELECTED)?.let {
            viewModel.setSelected(it)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet =
                dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            val behavior = BottomSheetBehavior.from(bottomSheet!!)
            behavior.skipCollapsed = true
            if (preferences.isTopAppBar) {
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val layout = inflater.inflate(R.layout.fragment_navigation_drawer, container, false)
        recyclerView = layout.findViewById(R.id.recycler_view)
        (recyclerView.itemAnimator as DefaultItemAnimator).supportsChangeAnimations = false
        adapter.setOnClick(this::onFilterItemSelected)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
        viewModel
            .viewState
            .flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
            .onEach {
                adapter.setSelected(it.selected)
                adapter.submitList(it.filters)
            }
            .launchIn(lifecycleScope)
        return layout
    }

    private fun onFilterItemSelected(item: FilterListItem?) {
        if (item is Filter) {
            viewModel.setSelected(item)
            activity?.startActivity(TaskIntents.getTaskListIntent(activity, item))
        } else if (item is NavigationDrawerAction) {
            when (item.requestCode) {
                REQUEST_PURCHASE ->
                    startActivity(Intent(context, PurchaseActivity::class.java))
                REQUEST_DONATE -> context?.openUri(R.string.url_donate)
                else -> activity?.startActivityForResult(item.intent, item.requestCode)
            }
        }
        dismiss()
    }

    companion object {
        const val REQUEST_NEW_LIST = 10100
        const val REQUEST_SETTINGS = 10101
        const val REQUEST_PURCHASE = 10102
        const val REQUEST_DONATE = 10103
        const val REQUEST_NEW_PLACE = 10104
        const val REQUEST_NEW_FILTER = 101015
        private const val EXTRA_SELECTED = "extra_selected"

        fun newNavigationDrawer(selected: Filter?): NavigationDrawerFragment {
            val fragment = NavigationDrawerFragment()
            fragment.arguments = Bundle().apply {
                putParcelable(EXTRA_SELECTED, selected)
            }
            return fragment
        }
    }
}