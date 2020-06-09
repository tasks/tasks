package org.tasks.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.astrid.adapter.NavigationDrawerAdapter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.FilterListItem
import com.todoroo.astrid.dao.TaskDao
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.billing.PurchaseActivity
import org.tasks.dialogs.NewFilterDialog.Companion.newFilterDialog
import org.tasks.filters.FilterProvider
import org.tasks.filters.NavigationDrawerAction
import org.tasks.injection.FragmentComponent
import org.tasks.injection.InjectingFragment
import org.tasks.intents.TaskIntents
import javax.inject.Inject

class NavigationDrawerFragment : InjectingFragment() {
    private val refreshReceiver = RefreshReceiver()

    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var adapter: NavigationDrawerAdapter
    @Inject lateinit var filterProvider: FilterProvider
    @Inject lateinit var taskDao: TaskDao

    private lateinit var recyclerView: RecyclerView
    private lateinit var mDrawerLayout: DrawerLayout
    private var disposables: CompositeDisposable? = null
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
                                REQUEST_PURCHASE -> startActivity(Intent(context, PurchaseActivity::class.java))
                                REQUEST_DONATE -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://tasks.org/donate")))
                                REQUEST_NEW_FILTER -> newFilterDialog().show(parentFragmentManager, FRAG_TAG_NEW_FILTER)
                                else -> activity?.startActivityForResult(item.intent, item.requestCode)
                            }
                        }
                    }
                })
        if (item is Filter) {
            ViewModelProvider(requireActivity()).get(TaskListViewModel::class.java).setFilter((item as Filter?)!!)
        }
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
    }

    fun setSelected(selected: Filter?) = adapter.setSelected(selected)

    override fun onPause() {
        super.onPause()
        localBroadcastManager.unregisterReceiver(refreshReceiver)
    }

    override fun onStart() {
        super.onStart()
        disposables = CompositeDisposable()
    }

    override fun onStop() {
        super.onStop()
        disposables?.dispose()
    }

    override fun inject(component: FragmentComponent) = component.inject(this)

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        adapter.save(outState)
    }

    fun closeDrawer() {
        mDrawerLayout.setDrawerListener(null)
        close()
    }

    private fun close() = mDrawerLayout.closeDrawer(mFragmentContainerView!!)

    fun openDrawer() = mDrawerLayout.openDrawer(mFragmentContainerView!!)

    override fun onResume() {
        super.onResume()
        localBroadcastManager.registerRefreshListReceiver(refreshReceiver)
        disposables?.add(updateFilters())
    }

    private fun updateFilters() =
            Single.fromCallable { filterProvider.navDrawerItems }
                .map { items: List<FilterListItem> -> refreshFilterCount(items) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(adapter::submitList)

    private fun refreshFilterCount(items: List<FilterListItem>): List<FilterListItem> {
        AndroidUtilities.assertNotMainThread()
        for (item in items) {
            if (item is Filter && item.count == -1) {
                item.count = taskDao.count(item)
            }
        }
        return items
    }

    private inner class RefreshReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent == null) {
                return
            }
            val action = intent.action
            if (LocalBroadcastManager.REFRESH == action || LocalBroadcastManager.REFRESH_LIST == action) {
                disposables?.add(updateFilters())
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