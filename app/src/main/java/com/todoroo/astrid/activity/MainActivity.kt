/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.composethemeadapter.MdcTheme
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.astrid.activity.TaskEditFragment.Companion.newTaskEditFragment
import com.todoroo.astrid.activity.TaskListFragment.TaskListFragmentCallbackHandler
import com.todoroo.astrid.adapter.SubheaderClickHandler
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.service.TaskCreator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tasks.BuildConfig
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.Tasks.Companion.IS_GENERIC
import org.tasks.activities.NavigationDrawerCustomization
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseActivity
import org.tasks.compose.collectAsStateLifecycleAware
import org.tasks.compose.drawer.DrawerAction
import org.tasks.compose.drawer.DrawerItem
import org.tasks.compose.drawer.ModalBottomSheet
import org.tasks.compose.drawer.SheetState
import org.tasks.compose.drawer.SheetValue
import org.tasks.compose.drawer.TaskListDrawer
import org.tasks.data.AlarmDao
import org.tasks.data.LocationDao
import org.tasks.data.Place
import org.tasks.data.TagDataDao
import org.tasks.databinding.TaskListActivityBinding
import org.tasks.dialogs.NewFilterDialog
import org.tasks.dialogs.WhatsNewDialog
import org.tasks.extensions.Context.nightMode
import org.tasks.extensions.Context.openUri
import org.tasks.extensions.hideKeyboard
import org.tasks.filters.FilterProvider
import org.tasks.filters.PlaceFilter
import org.tasks.intents.TaskIntents.getTaskListIntent
import org.tasks.location.LocationPickerActivity
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.HelpAndFeedback
import org.tasks.preferences.MainPreferences
import org.tasks.preferences.Preferences
import org.tasks.themes.ColorProvider
import org.tasks.themes.Theme
import org.tasks.themes.ThemeColor
import org.tasks.ui.EmptyTaskEditFragment.Companion.newEmptyTaskEditFragment
import org.tasks.ui.MainActivityEvent
import org.tasks.ui.MainActivityEventBus
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), TaskListFragmentCallbackHandler {
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var theme: Theme
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var taskCreator: TaskCreator
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var colorProvider: ColorProvider
    @Inject lateinit var locationDao: LocationDao
    @Inject lateinit var tagDataDao: TagDataDao
    @Inject lateinit var alarmDao: AlarmDao
    @Inject lateinit var eventBus: MainActivityEventBus
    @Inject lateinit var firebase: Firebase

    private val viewModel: MainActivityViewModel by viewModels()
    private var currentNightMode = 0
    private var currentPro = false
    private var actionMode: ActionMode? = null
    private lateinit var binding: TaskListActivityBinding

    private val filter: Filter?
        get() = viewModel.state.value.filter

    private val settingsRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            recreate()
        }

    /** @see android.app.Activity.onCreate
     */
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        theme.applyTheme(this)
        currentNightMode = nightMode
        currentPro = inventory.hasPro
        binding = TaskListActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        handleIntent()

        binding.composeView.setContent {
            val state = viewModel.state.collectAsStateLifecycleAware().value
            if (state.drawerOpen) {
                MdcTheme {
                    var expanded by remember { mutableStateOf(false) }
                    val skipPartiallyExpanded = remember(expanded) {
                        expanded || preferences.isTopAppBar
                    }
                    val sheetState = rememberSaveable(
                        skipPartiallyExpanded,
                        saver = SheetState.Saver(
                            skipPartiallyExpanded = skipPartiallyExpanded,
                            confirmValueChange = { true },
                        )
                    ) {
                        SheetState(
                            skipPartiallyExpanded = skipPartiallyExpanded,
                            initialValue = if (skipPartiallyExpanded) SheetValue.Expanded else SheetValue.PartiallyExpanded,
                            confirmValueChange = { true },
                            skipHiddenState = false,
                        )
                    }
                    LaunchedEffect(sheetState.targetValue) {
                        if (sheetState.targetValue == SheetValue.Expanded) {
                            expanded = true
                        }
                    }
                    ModalBottomSheet(
                        sheetState = sheetState,
                        containerColor = MaterialTheme.colors.surface,
                        onDismissRequest = { viewModel.setDrawerOpen(false) }
                    ) {
                        val scope = rememberCoroutineScope()
                        TaskListDrawer(
                            begForMoney = state.begForMoney,
                            filters = state.drawerItems,
                            onClick = {
                                when (it) {
                                    is DrawerItem.Filter -> {
                                        openTaskListFragment(it.type())
                                        scope.launch(Dispatchers.Default) {
                                            sheetState.hide()
                                            viewModel.setDrawerOpen(false)
                                        }
                                    }
                                    is DrawerItem.Header -> {
                                        viewModel.toggleCollapsed(it.type())
                                    }
                                }
                            },
                            onAddClick = {
                                scope.launch(Dispatchers.Default) {
                                    sheetState.hide()
                                    viewModel.setDrawerOpen(false)
                                    val subheaderType = it.type()
                                    val rc = subheaderType.addIntentRc
                                    if (rc == FilterProvider.REQUEST_NEW_FILTER) {
                                        NewFilterDialog.newFilterDialog().show(
                                            supportFragmentManager,
                                            SubheaderClickHandler.FRAG_TAG_NEW_FILTER
                                        )
                                    } else {
                                        val intent = subheaderType.addIntent ?: return@launch
                                        startActivityForResult(intent, rc)
                                    }
                                }
                            },
                            onDrawerAction = {
                                viewModel.setDrawerOpen(false)
                                when (it) {
                                    DrawerAction.PURCHASE ->
                                        if (IS_GENERIC)
                                            openUri(R.string.url_donate)
                                        else
                                            startActivity(
                                                Intent(
                                                    this@MainActivity,
                                                    PurchaseActivity::class.java
                                                )
                                            )

                                    DrawerAction.CUSTOMIZE_DRAWER ->
                                        startActivity(
                                            Intent(
                                                this@MainActivity,
                                                NavigationDrawerCustomization::class.java
                                            )
                                        )

                                    DrawerAction.SETTINGS ->
                                        settingsRequest.launch(
                                            Intent(
                                                this@MainActivity,
                                                MainPreferences::class.java
                                            )
                                        )

                                    DrawerAction.HELP_AND_FEEDBACK ->
                                        startActivity(
                                            Intent(
                                                this@MainActivity,
                                                HelpAndFeedback::class.java
                                            )
                                        )
                                }
                            },
                            onErrorClick = {
                                startActivity(Intent(this@MainActivity, MainPreferences::class.java))
                            },
                        )
                    }
                }
            }
        }

        eventBus
            .onEach(this::process)
            .launchIn(lifecycleScope)

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                applyTheme()
            }
        }
    }

    private suspend fun process(event: MainActivityEvent) = when (event) {
        is MainActivityEvent.OpenTask ->
            onTaskListItemClicked(event.task)
        is MainActivityEvent.ClearTaskEditFragment ->
            removeTaskEditFragment()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_NEW_LIST ->
                if (resultCode == RESULT_OK) {
                    data
                            ?.getParcelableExtra<Filter>(OPEN_FILTER)
                            ?.let { startActivity(getTaskListIntent(this, it)) }
                }
            REQUEST_NEW_PLACE ->
                if (resultCode == RESULT_OK) {
                    data
                            ?.getParcelableExtra<Place>(LocationPickerActivity.EXTRA_PLACE)
                            ?.let { startActivity(getTaskListIntent(this, PlaceFilter(it))) }
                }
            else ->
                super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent()
    }

    private fun clearUi() {
        finishActionMode()
        viewModel.setDrawerOpen(false)
    }

    private suspend fun getTaskToLoad(filter: Filter?): Task? {
        val intent = intent
        if (intent.isFromHistory) {
            return null
        }
        if (intent.hasExtra(CREATE_TASK)) {
            val source = intent.getStringExtra(CREATE_SOURCE)
            firebase.addTask(source ?: "unknown")
            intent.removeExtra(CREATE_TASK)
            intent.removeExtra(CREATE_SOURCE)
            return taskCreator.createWithValues(filter, "")
        }
        if (intent.hasExtra(OPEN_TASK)) {
            val task: Task? = intent.getParcelableExtra(OPEN_TASK)
            intent.removeExtra(OPEN_TASK)
            return task
        }
        return null
    }

    private fun openTask(filter: Filter?) = lifecycleScope.launch {
        val task = getTaskToLoad(filter)
        when {
            task != null -> onTaskListItemClicked(task)
            taskEditFragment == null -> hideDetailFragment()
            else -> showDetailFragment()
        }
    }

    private fun handleIntent() {
        val intent = intent
        val openFilter = intent.getFilter
        val loadFilter = intent.getFilterString
        val openTask = !intent.isFromHistory
                && (intent.hasExtra(OPEN_TASK) || intent.hasExtra(CREATE_TASK))
        val tef = taskEditFragment
        if (BuildConfig.DEBUG) {
            Timber.d(
                """
            
            **********
            broughtToFront: ${intent.broughtToFront}
            isFromHistory: ${intent.isFromHistory}
            flags: ${intent.flagsToString}
            OPEN_FILTER: ${openFilter?.let { "${it.title}: $it" }}
            LOAD_FILTER: $loadFilter
            OPEN_TASK: ${intent.getParcelableExtra<Task>(OPEN_TASK)}
            CREATE_TASK: ${intent.hasExtra(CREATE_TASK)}
            taskListFragment: ${taskListFragment?.getFilter()?.let { "${it.title}: $it" }}
            taskEditFragment: ${taskEditFragment?.editViewModel?.task}
            **********"""
            )
        }
        if (!openTask && (openFilter != null || !loadFilter.isNullOrBlank())) {
            tef?.let {
                lifecycleScope.launch {
                    it.save()
                }
            }
        }
        if (!loadFilter.isNullOrBlank() || openFilter == null && filter == null) {
            lifecycleScope.launch {
                val filter = if (loadFilter.isNullOrBlank()) {
                    defaultFilterProvider.getStartupFilter()
                } else {
                    defaultFilterProvider.getFilterFromPreference(loadFilter)
                }
                clearUi()
                if (isSinglePaneLayout) {
                    if (openTask) {
                        setFilter(filter)
                        openTask(filter)
                    } else {
                        openTaskListFragment(filter, true)
                    }
                } else {
                    openTaskListFragment(filter, true)
                    openTask(filter)
                }
            }
        } else if (openFilter != null) {
            clearUi()
            if (isSinglePaneLayout) {
                if (openTask) {
                    setFilter(openFilter)
                    openTask(openFilter)
                } else {
                    openTaskListFragment(openFilter, true)
                }
            } else {
                openTaskListFragment(openFilter, true)
                openTask(openFilter)
            }
        } else {
            val existing = taskListFragment
            val target = if (existing == null || existing.getFilter() !== filter) {
                TaskListFragment.newTaskListFragment(applicationContext, filter)
            } else {
                existing
            }
            if (isSinglePaneLayout) {
                if (openTask || tef != null) {
                    openTask(filter)
                } else {
                    openTaskListFragment(filter, false)
                }
            } else {
                openTaskListFragment(target, false)
                openTask(filter)
            }
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
                .replace(R.id.detail, newEmptyTaskEditFragment())
                .runOnCommit {
                    if (isSinglePaneLayout) {
                        binding.master.visibility = View.VISIBLE
                        binding.detail.visibility = View.GONE
                    }
                }
                .commit()
    }

    private fun setFilter(newFilter: Filter?) {
        newFilter?.let {
            viewModel.setFilter(it)
            applyTheme()
        }
    }

    private fun openTaskListFragment(filter: Filter?, force: Boolean = false) {
        openTaskListFragment(TaskListFragment.newTaskListFragment(applicationContext, filter), force)
    }

    private fun openTaskListFragment(taskListFragment: TaskListFragment, force: Boolean) {
        AndroidUtilities.assertMainThread()
        if (supportFragmentManager.isDestroyed) {
            return
        }
        val newFilter = taskListFragment.getFilter()
        if (!force && filter == newFilter) {
            return
        }
        viewModel.setFilter(newFilter)
        applyTheme()
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.master, taskListFragment, FRAG_TAG_TASK_LIST)
                .commitNowAllowingStateLoss()
    }

    private fun applyTheme() {
        val filterColor = filterColor
        filterColor.applyToNavigationBar(this)
        filterColor.applyTaskDescription(this, filter?.title ?: getString(R.string.app_name))
        theme.withThemeColor(filterColor).applyToContext(this)
    }

    private val filterColor: ThemeColor
        get() = filter?.tint?.takeIf { it != 0 }
            ?.let { colorProvider.getThemeColor(it, true) } ?: theme.themeColor

    override fun onResume() {
        super.onResume()
        if (currentNightMode != nightMode || currentPro != inventory.hasPro) {
            recreate()
            return
        }
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

    override suspend fun onTaskListItemClicked(task: Task?) {
        AndroidUtilities.assertMainThread()
        if (task == null) {
            return
        }
        taskEditFragment?.save(remove = false)
        clearUi()
        coroutineScope {
            val freshTask = async { if (task.isNew) task else taskDao.fetch(task.id) ?: task }
            val list = async { defaultFilterProvider.getList(task) }
            val location = async { locationDao.getLocation(task, preferences) }
            val tags = async { tagDataDao.getTags(task) }
            val alarms = async { alarmDao.getAlarms(task) }
            val fragment = withContext(Dispatchers.Default) {
                newTaskEditFragment(
                        freshTask.await(),
                        list.await(),
                        location.await(),
                        tags.await(),
                        alarms.await(),
                )
            }
            supportFragmentManager.beginTransaction()
                    .replace(R.id.detail, fragment, TaskEditFragment.TAG_TASKEDIT_FRAGMENT)
                    .runOnCommit { showDetailFragment() }
                    .commitNowAllowingStateLoss()

        }
    }

    override fun onNavigationIconClicked() {
        hideKeyboard()
        viewModel.setDrawerOpen(true)
    }

    private val taskListFragment: TaskListFragment?
        get() = supportFragmentManager.findFragmentByTag(FRAG_TAG_TASK_LIST) as TaskListFragment?

    private val taskEditFragment: TaskEditFragment?
        get() = supportFragmentManager.findFragmentByTag(TaskEditFragment.TAG_TASKEDIT_FRAGMENT) as TaskEditFragment?

    private val isSinglePaneLayout: Boolean
        get() = !resources.getBoolean(R.bool.two_pane_layout)

    private fun removeTaskEditFragment() {
        val removeTask = intent.removeTask
        val finishAffinity = intent.finishAffinity
        if (finishAffinity || taskListFragment == null) {
            finishAffinity()
        } else {
            if (removeTask && intent.broughtToFront) {
                moveTaskToBack(true)
            }
            hideKeyboard()
            hideDetailFragment()
            taskListFragment?.let {
                setFilter(it.getFilter())
                it.loadTaskListContent()
            }
        }
    }

    override fun onSupportActionModeStarted(mode: ActionMode) {
        super.onSupportActionModeStarted(mode)
        actionMode = mode
    }

    private fun finishActionMode() {
        actionMode?.finish()
        actionMode = null
    }

    companion object {
        /** For indicating the new list screen should be launched at fragment setup time  */
        const val OPEN_FILTER = "open_filter" // $NON-NLS-1$
        const val LOAD_FILTER = "load_filter"
        const val CREATE_TASK = "open_task" // $NON-NLS-1$
        const val CREATE_SOURCE = "create_source"
        const val OPEN_TASK = "open_new_task" // $NON-NLS-1$
        const val REMOVE_TASK = "remove_task"
        const val FINISH_AFFINITY = "finish_affinity"
        private const val FRAG_TAG_TASK_LIST = "frag_tag_task_list"
        private const val FRAG_TAG_WHATS_NEW = "frag_tag_whats_new"
        private const val FLAG_FROM_HISTORY
                = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
        const val REQUEST_NEW_LIST = 10100
        const val REQUEST_NEW_PLACE = 10104

        val Intent.getFilter: Filter?
            get() = if (isFromHistory) {
                null
            } else {
                getParcelableExtra<Filter?>(OPEN_FILTER)?.let {
                    removeExtra(OPEN_FILTER)
                    it
                }
            }

        val Intent.getFilterString: String?
            get() = if (isFromHistory) {
                null
            } else {
                getStringExtra(LOAD_FILTER)?.let {
                    removeExtra(LOAD_FILTER)
                    it
                }
            }

        val Intent.removeTask: Boolean
            get() = if (isFromHistory) {
                false
            } else {
                getBooleanExtra(REMOVE_TASK, false).let {
                    removeExtra(REMOVE_TASK)
                    it
                }
            }

        val Intent.finishAffinity: Boolean
            get() = if (isFromHistory) {
                false
            } else {
                getBooleanExtra(FINISH_AFFINITY, false).let {
                    removeExtra(FINISH_AFFINITY)
                    it
                }
            }

        val Intent.isFromHistory: Boolean
            get() = flags and FLAG_FROM_HISTORY == FLAG_FROM_HISTORY

        val Intent.broughtToFront: Boolean
            get() = flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT > 0

        val Intent.flagsToString
            get() = Intent::class.java.declaredFields
                .filter { it.name.startsWith("FLAG_") }
                .filter { flags or it.getInt(null) == flags }
                .joinToString(" | ") { it.name }
    }
}