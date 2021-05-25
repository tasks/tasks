package org.tasks.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.todoroo.astrid.adapter.NavigationDrawerAdapter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.FilterListItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.billing.PurchaseActivity
import org.tasks.data.TaskDao
import org.tasks.dialogs.NewFilterDialog.Companion.newFilterDialog
import org.tasks.extensions.Context.openUri
import org.tasks.extensions.View.lightStatusBar
import org.tasks.filters.FilterProvider
import org.tasks.filters.NavigationDrawerAction
import org.tasks.intents.TaskIntents
import javax.inject.Inject

@AndroidEntryPoint
class NavigationDrawerFragment : Fragment() {
    private val refreshReceiver = RefreshReceiver()

    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var adapter: NavigationDrawerAdapter
    @Inject lateinit var filterProvider: FilterProvider
    @Inject lateinit var taskDao: TaskDao

    private lateinit var recyclerView: RecyclerView
    private lateinit var mDrawerLayout: DrawerLayout
    private var mFragmentContainerView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            adapter.restore(savedInstanceState)
        }
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
        (layout.findViewById<View>(R.id.scrim_layout) as ScrimInsetsFrameLayout)
                .setOnInsetsCallback { insets: Rect -> recyclerView.setPadding(0, insets.top, 0, 0) }
        return layout
    }

    private fun setUpList() {
        adapter.setOnClick { item: FilterListItem? -> onFilterItemSelected(item) }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun onFilterItemSelected(item: FilterListItem?) {
        mDrawerLayout.addDrawerListener(
                object : SimpleDrawerListener() {
                    override fun onDrawerClosed(drawerView: View) {
                        mDrawerLayout.removeDrawerListener(this)
                        if (item is Filter) {
                            activity?.startActivity(TaskIntents.getTaskListIntent(activity, item))
                        } else if (item is NavigationDrawerAction) {
                            when (item.requestCode) {
                                REQUEST_PURCHASE ->
                                    startActivity(Intent(context, PurchaseActivity::class.java))
                                REQUEST_DONATE -> context?.openUri(R.string.url_donate)
                                REQUEST_NEW_FILTER -> newFilterDialog().show(parentFragmentManager, FRAG_TAG_NEW_FILTER)
                                else -> activity?.startActivityForResult(item.intent, item.requestCode)
                            }
                        }
                    }
                })
        close()
    }

    val isDrawerOpen: Boolean
        get() = mDrawerLayout.isDrawerOpen(mFragmentContainerView!!)

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    fun setUp(drawerLayout: DrawerLayout) {
        mFragmentContainerView = requireActivity().findViewById(FRAGMENT_NAVIGATION_DRAWER)
        mDrawerLayout = drawerLayout
        mDrawerLayout.addDrawerListener(object : SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                setStatusBarColors()
            }
        })
    }

    fun setSelected(selected: Filter?) = adapter.setSelected(selected)

    override fun onPause() {
        super.onPause()
        localBroadcastManager.unregisterReceiver(refreshReceiver)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        adapter.save(outState)
    }

    fun closeDrawer() {
        mDrawerLayout.setDrawerListener(null)
        close()
    }

    private fun close() = mDrawerLayout.closeDrawer(mFragmentContainerView!!)

    fun openDrawer() {
        setStatusBarColors()
        mDrawerLayout.openDrawer(mFragmentContainerView!!)
    }

    private fun setStatusBarColors() = mDrawerLayout.lightStatusBar(false)

    override fun onResume() {
        super.onResume()
        localBroadcastManager.registerRefreshListReceiver(refreshReceiver)
        updateFilters()
        if (isDrawerOpen) {
            setStatusBarColors()
        }
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
        const val FRAGMENT_NAVIGATION_DRAWER = R.id.navigation_drawer
        const val REQUEST_NEW_LIST = 10100
        const val REQUEST_SETTINGS = 10101
        const val REQUEST_PURCHASE = 10102
        const val REQUEST_DONATE = 10103
        const val REQUEST_NEW_PLACE = 10104
        const val REQUEST_NEW_FILTER = 101015
        private const val FRAG_TAG_NEW_FILTER = "frag_tag_new_filter"
    }
}