/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.view.ActionMode
import androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.astrid.activity.TaskEditFragment.TaskEditFragmentCallbackHandler
import com.todoroo.astrid.activity.TaskListFragment.TaskListFragmentCallbackHandler
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.service.TaskCreator
import com.todoroo.astrid.timers.TimerControlSet.TimerControlSetCallback
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.tasks.BuildConfig
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.activities.TagSettingsActivity
import org.tasks.billing.Inventory
import org.tasks.data.Place
import org.tasks.databinding.TaskListActivityBinding
import org.tasks.dialogs.SortDialog.SortDialogCallback
import org.tasks.dialogs.WhatsNewDialog
import org.tasks.filters.PlaceFilter
import org.tasks.fragments.CommentBarFragment.CommentBarFragmentCallback
import org.tasks.gtasks.PlayServices
import org.tasks.injection.ActivityComponent
import org.tasks.injection.InjectingAppCompatActivity
import org.tasks.intents.TaskIntents
import org.tasks.location.LocationPickerActivity
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.receivers.RepeatConfirmationReceiver
import org.tasks.tasklist.ActionUtils
import org.tasks.themes.ColorProvider
import org.tasks.themes.Theme
import org.tasks.themes.ThemeColor
import org.tasks.ui.DeadlineControlSet.DueDateChangeListener
import org.tasks.ui.EmptyTaskEditFragment.Companion.newEmptyTaskEditFragment
import org.tasks.ui.ListFragment.OnListChanged
import org.tasks.ui.NavigationDrawerFragment
import org.tasks.ui.TaskListViewModel
import javax.inject.Inject

class MainActivity : InjectingAppCompatActivity(), TaskListFragmentCallbackHandler, OnListChanged, TimerControlSetCallback, DueDateChangeListener, TaskEditFragmentCallbackHandler, CommentBarFragmentCallback, SortDialogCallback {
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var repeatConfirmationReceiver: RepeatConfirmationReceiver
    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var theme: Theme
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var taskCreator: TaskCreator
    @Inject lateinit var playServices: PlayServices
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var colorProvider: ColorProvider
    private var disposables: CompositeDisposable? = null
    private lateinit var navigationDrawer: NavigationDrawerFragment
    private var currentNightMode = 0
    private var currentPro = false
    private var filter: Filter? = null
    private var actionMode: ActionMode? = null
    private lateinit var binding: TaskListActivityBinding

    /** @see android.app.Activity.onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = ViewModelProvider(this).get(TaskListViewModel::class.java)
        component.inject(viewModel)
        currentNightMode = nightMode
        currentPro = inventory.hasPro()
        binding = TaskListActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (savedInstanceState != null) {
            filter = savedInstanceState.getParcelable(EXTRA_FILTER)
            applyTheme()
        }
        navigationDrawer = navigationDrawerFragment
        navigationDrawer.setUp(binding.drawerLayout)
        binding.drawerLayout.addDrawerListener(
                object : SimpleDrawerListener() {
                    override fun onDrawerStateChanged(newState: Int) {
                        finishActionMode()
                    }
                })
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == NavigationDrawerFragment.REQUEST_SETTINGS) {
            if (AndroidUtilities.atLeastNougat()) {
                recreate()
            } else {
                finish()
                startActivity(TaskIntents.getTaskListIntent(this, filter))
            }
        } else if (requestCode == NavigationDrawerFragment.REQUEST_NEW_LIST) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val filter: Filter? = data.getParcelableExtra(OPEN_FILTER)
                if (filter != null) {
                    startActivity(TaskIntents.getTaskListIntent(this, filter))
                }
            }
        } else if (requestCode == NavigationDrawerFragment.REQUEST_NEW_PLACE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val place: Place? = data.getParcelableExtra(LocationPickerActivity.EXTRA_PLACE)
                if (place != null) {
                    startActivity(TaskIntents.getTaskListIntent(this, PlaceFilter(place)))
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(EXTRA_FILTER, filter)
    }

    private fun clearUi() {
        finishActionMode()
        navigationDrawer.closeDrawer()
    }

    private fun getTaskToLoad(filter: Filter?): Task? {
        val intent = intent
        if (intent.hasExtra(CREATE_TASK)) {
            intent.removeExtra(CREATE_TASK)
            return taskCreator.createWithValues(filter, "")
        }
        if (intent.hasExtra(OPEN_TASK)) {
            val task: Task? = intent.getParcelableExtra(OPEN_TASK)
            intent.removeExtra(OPEN_TASK)
            return task
        }
        return null
    }

    private fun openTask(filter: Filter?) {
        val task = getTaskToLoad(filter)
        when {
            task != null -> onTaskListItemClicked(task)
            taskEditFragment == null -> hideDetailFragment()
            else -> showDetailFragment()
        }
    }

    private fun handleIntent() {
        val intent = intent
        val openFilter = intent.hasExtra(OPEN_FILTER)
        val loadFilter = intent.hasExtra(LOAD_FILTER)
        val tef = taskEditFragment
        if (tef != null && (openFilter || loadFilter)) {
            tef.save()
        }
        if (loadFilter || !openFilter && filter == null) {
            disposables!!.add(
                    Single.fromCallable {
                        val filter = intent.getStringExtra(LOAD_FILTER)
                        intent.removeExtra(LOAD_FILTER)
                        if (filter.isNullOrBlank()) {
                            defaultFilterProvider.startupFilter
                        } else {
                            defaultFilterProvider.getFilterFromPreference(filter)
                        }
                    }
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe { filter: Filter? ->
                                clearUi()
                                openTaskListFragment(filter)
                                openTask(filter)
                            })
        } else if (openFilter) {
            val filter: Filter? = intent.getParcelableExtra(OPEN_FILTER)
            intent.removeExtra(OPEN_FILTER)
            clearUi()
            openTaskListFragment(filter)
            openTask(filter)
        } else {
            val existing = taskListFragment
            openTaskListFragment(
                    if (existing == null || existing.getFilter() !== filter) TaskListFragment.newTaskListFragment(applicationContext, filter) else existing,
                    false)
            openTask(filter)
        }
        if (intent.hasExtra(TOKEN_CREATE_NEW_LIST_NAME)) {
            val listName = intent.getStringExtra(TOKEN_CREATE_NEW_LIST_NAME)
            intent.removeExtra(TOKEN_CREATE_NEW_LIST_NAME)
            val activityIntent = Intent(this@MainActivity, TagSettingsActivity::class.java)
            activityIntent.putExtra(TagSettingsActivity.TOKEN_AUTOPOPULATE_NAME, listName)
            startActivityForResult(activityIntent, NavigationDrawerFragment.REQUEST_NEW_LIST)
        }
    }

    private fun showDetailFragment() {
        if (isSinglePaneLayout) {
            binding.detail.visibility = View.VISIBLE
            binding.master.visibility = View.GONE
        }
    }

    private fun hideDetailFragment() {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.detail, newEmptyTaskEditFragment(filter!!))
                .commit()
        if (isSinglePaneLayout) {
            binding.master.visibility = View.VISIBLE
            binding.detail.visibility = View.GONE
        }
    }

    private fun openTaskListFragment(filter: Filter?, force: Boolean = false) {
        openTaskListFragment(TaskListFragment.newTaskListFragment(applicationContext, filter), force)
    }

    private fun openTaskListFragment(taskListFragment: TaskListFragment, force: Boolean) {
        AndroidUtilities.assertMainThread()
        val newFilter = taskListFragment.getFilter()
        if (filter != null && !force
                && filter!!.areItemsTheSame(newFilter)
                && filter!!.areContentsTheSame(newFilter)) {
            return
        }
        filter = newFilter
        navigationDrawer.setSelected(filter)
        defaultFilterProvider.lastViewedFilter = newFilter
        applyTheme()
        val fragmentManager = supportFragmentManager
        fragmentManager
                .beginTransaction()
                .replace(R.id.master, taskListFragment, FRAG_TAG_TASK_LIST)
                .commit()
        fragmentManager.executePendingTransactions()
    }

    private fun applyTheme() {
        val filterColor = filterColor
        filterColor.setStatusBarColor(binding.drawerLayout)
        filterColor.applyToNavigationBar(this)
        filterColor.applyTaskDescription(this, filter?.listingTitle ?: getString(R.string.app_name))
        theme.withThemeColor(filterColor).applyToContext(this)
    }

    private val filterColor: ThemeColor
        get() = if (filter != null && filter!!.tint != 0) colorProvider.getThemeColor(filter!!.tint, true) else theme.themeColor

    private val navigationDrawerFragment: NavigationDrawerFragment
        get() = supportFragmentManager
                .findFragmentById(NavigationDrawerFragment.FRAGMENT_NAVIGATION_DRAWER) as NavigationDrawerFragment

    override fun onResume() {
        super.onResume()
        if (currentNightMode != nightMode || currentPro != inventory.hasPro()) {
            recreate()
            return
        }
        localBroadcastManager.registerRepeatReceiver(repeatConfirmationReceiver)
        check(!(BuildConfig.DEBUG && disposables != null && !disposables!!.isDisposed))
        disposables = CompositeDisposable(playServices.check(this))
        if (preferences.getBoolean(R.string.p_just_updated, false)) {
            if (preferences.getBoolean(R.string.p_show_whats_new, true)) {
                val fragmentManager = supportFragmentManager
                if (fragmentManager.findFragmentByTag(FRAG_TAG_WHATS_NEW) == null) {
                    WhatsNewDialog().show(fragmentManager, FRAG_TAG_WHATS_NEW)
                }
            }
            preferences.setBoolean(R.string.p_just_updated, false)
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        handleIntent()
    }

    private val nightMode: Int
        get() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

    override fun inject(component: ActivityComponent) {
        component.inject(this)
        theme.applyTheme(this)
    }

    override fun onPause() {
        super.onPause()
        localBroadcastManager.unregisterReceiver(repeatConfirmationReceiver)
        disposables?.dispose()
    }

    override fun onTaskListItemClicked(task: Task?) {
        AndroidUtilities.assertMainThread()
        if (task == null) {
            return
        }
        val taskEditFragment = taskEditFragment
        taskEditFragment?.save()
        clearUi()
        if (task.isNew) {
            openTask(task)
        } else {
            disposables!!.add(
                    Single.fromCallable { taskDao.fetch(task.id) }
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe { t: Task? -> this.openTask(t) })
        }
    }

    private fun openTask(task: Task?) {
        supportFragmentManager
                .beginTransaction()
                .replace(
                        R.id.detail,
                        TaskEditFragment.newTaskEditFragment(task, filterColor),
                        TaskEditFragment.TAG_TASKEDIT_FRAGMENT)
                .addToBackStack(TaskEditFragment.TAG_TASKEDIT_FRAGMENT)
                .commit()
        showDetailFragment()
    }

    override fun onNavigationIconClicked() {
        hideKeyboard()
        navigationDrawer.openDrawer()
    }

    override fun onBackPressed() {
        if (navigationDrawer.isDrawerOpen) {
            navigationDrawer.closeDrawer()
            return
        }
        val taskEditFragment = taskEditFragment
        if (taskEditFragment != null) {
            if (preferences.backButtonSavesTask()) {
                taskEditFragment.save()
            } else {
                taskEditFragment.discardButtonClick()
            }
            return
        }
        if (taskListFragment?.collapseSearchView() == true) {
            return
        }
        finish()
    }

    val taskListFragment: TaskListFragment?
        get() = supportFragmentManager.findFragmentByTag(FRAG_TAG_TASK_LIST) as TaskListFragment?

    val taskEditFragment: TaskEditFragment?
        get() = supportFragmentManager.findFragmentByTag(TaskEditFragment.TAG_TASKEDIT_FRAGMENT) as TaskEditFragment?

    override fun stopTimer(): Task {
        return taskEditFragment!!.stopTimer()
    }

    override fun startTimer(): Task {
        return taskEditFragment!!.startTimer()
    }

    private val isSinglePaneLayout: Boolean
        get() = !resources.getBoolean(R.bool.two_pane_layout)

    override fun removeTaskEditFragment() {
        supportFragmentManager
                .popBackStackImmediate(
                        TaskEditFragment.TAG_TASKEDIT_FRAGMENT, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        hideDetailFragment()
        hideKeyboard()
        taskListFragment?.loadTaskListContent()
    }

    private fun hideKeyboard() {
        val view = currentFocus
        if (view != null) {
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun addComment(message: String?, picture: Uri?) {
        val taskEditFragment = taskEditFragment
        taskEditFragment?.addComment(message, picture)
    }

    override fun sortChanged(reload: Boolean) {
        taskListFragment?.clearCollapsed()
        localBroadcastManager.broadcastRefresh()
        if (reload) {
            openTaskListFragment(filter, true)
        }
    }

    override fun onSupportActionModeStarted(mode: ActionMode) {
        super.onSupportActionModeStarted(mode)
        actionMode = mode
        val filterColor = filterColor
        ActionUtils.applySupportActionModeColor(filterColor, mode)
        filterColor.setStatusBarColor(this)
    }

    @SuppressLint("NewApi")
    override fun onSupportActionModeFinished(mode: ActionMode) {
        super.onSupportActionModeFinished(mode)
        window.statusBarColor = 0
    }

    private fun finishActionMode() {
        actionMode?.finish()
        actionMode = null
    }

    override fun dueDateChanged(dateTime: Long) {
        taskEditFragment!!.onDueDateChanged(dateTime)
    }

    override fun onListChanged(filter: Filter?) {
        taskEditFragment!!.onRemoteListChanged(filter)
    }

    companion object {
        /** For indicating the new list screen should be launched at fragment setup time  */
        const val TOKEN_CREATE_NEW_LIST_NAME = "newListName" // $NON-NLS-1$
        const val OPEN_FILTER = "open_filter" // $NON-NLS-1$
        const val LOAD_FILTER = "load_filter"
        const val CREATE_TASK = "open_task" // $NON-NLS-1$
        const val OPEN_TASK = "open_new_task" // $NON-NLS-1$
        private const val FRAG_TAG_TASK_LIST = "frag_tag_task_list"
        private const val FRAG_TAG_WHATS_NEW = "frag_tag_whats_new"
        private const val EXTRA_FILTER = "extra_filter"
    }
}