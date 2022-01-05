package org.tasks.ui

import android.app.Activity
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.billing.PurchaseActivity
import org.tasks.data.TaskDao
import org.tasks.extensions.Context.openUri
import org.tasks.filters.FilterProvider
import org.tasks.filters.NavigationDrawerAction
import org.tasks.intents.TaskIntents
import javax.inject.Inject

@AndroidEntryPoint
class NavigationDrawerFragment : BottomSheetDialogFragment() {
    private val refreshReceiver = RefreshReceiver()

    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var adapter: NavigationDrawerAdapter
    @Inject lateinit var filterProvider: FilterProvider
    @Inject lateinit var taskDao: TaskDao

    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            adapter.restore(savedInstanceState)
        }
        arguments?.getParcelable<Filter>(EXTRA_SELECTED)?.let {
            adapter.setSelected(it)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            val behavior = BottomSheetBehavior.from(bottomSheet!!)
            behavior.skipCollapsed = true
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
            }
        }
        return dialog
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireActivity().setDefaultKeyMode(Activity.DEFAULT_KEYS_SEARCH_LOCAL)
        setUpList()
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layout = inflater.inflate(R.layout.fragment_navigation_drawer, container, false)
        recyclerView = layout.findViewById(R.id.recycler_view)
        (recyclerView.itemAnimator as DefaultItemAnimator).supportsChangeAnimations = false
        return layout
    }

    private fun setUpList() {
        adapter.setOnClick { item: FilterListItem? -> onFilterItemSelected(item) }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun onFilterItemSelected(item: FilterListItem?) {
        if (item is Filter) {
            activity?.startActivity(TaskIntents.getTaskListIntent(activity, item))
        } else if (item is NavigationDrawerAction) {
            when (item.requestCode) {
                REQUEST_PURCHASE ->
                    startActivity(Intent(context, PurchaseActivity::class.java))
                REQUEST_DONATE -> context?.openUri(R.string.url_donate)
                else -> activity?.startActivityForResult(item.intent, item.requestCode)
            }
        }
        closeDrawer()
    }

    override fun onPause() {
        super.onPause()
        localBroadcastManager.unregisterReceiver(refreshReceiver)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        adapter.save(outState)
    }

    fun closeDrawer() {
        dismiss()
    }

    override fun onResume() {
        super.onResume()
        localBroadcastManager.registerRefreshListReceiver(refreshReceiver)
        updateFilters()
    }

    private fun updateFilters() = lifecycleScope.launch {
        filterProvider
                .navDrawerItems()
                .onEach {
                    if (it is Filter && it.count == -1) {
                        it.count = taskDao.count(it)
                    }
                }
                .let { adapter.submitList(it) }
    }

    private inner class RefreshReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent == null) {
                return
            }
            val action = intent.action
            if (LocalBroadcastManager.REFRESH == action || LocalBroadcastManager.REFRESH_LIST == action) {
                updateFilters()
            }
        }
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