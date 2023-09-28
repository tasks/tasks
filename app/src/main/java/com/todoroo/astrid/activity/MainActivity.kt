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
import androidx.core.content.IntentCompat.getParcelableExtra
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.composethemeadapter.MdcTheme
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.astrid.activity.TaskEditFragment.Companion.newTaskEditFragment
import com.todoroo.astrid.adapter.SubheaderClickHandler
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.service.TaskCreator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tasks.BuildConfig
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
import org.tasks.location.LocationPickerActivity.Companion.EXTRA_PLACE
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.HelpAndFeedback
import org.tasks.preferences.MainPreferences
import org.tasks.preferences.Preferences
import org.tasks.themes.ColorProvider
import org.tasks.themes.Theme
import org.tasks.ui.EmptyTaskEditFragment.Companion.newEmptyTaskEditFragment
import org.tasks.ui.MainActivityEvent
import org.tasks.ui.MainActivityEventBus
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var theme: Theme
    @Inject lateinit var taskDao: TaskDao
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
        logIntent("onCreate")
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
                    LaunchedEffect(sheetState.currentValue) {
                        if (sheetState.currentValue == SheetValue.Expanded) {
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
                                        viewModel.setFilter(it.type())
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
                updateSystemBars(viewModel.state.value.filter)
            }
        }

        viewModel
            .state
            .flowWithLifecycle(lifecycle)
            .map { it.filter to it.task }
            .distinctUntilChanged()
            .onEach { (newFilter, task) ->
                Timber.d("filter: $newFilter task: $task")
                val existingTlf =
                    supportFragmentManager.findFragmentByTag(FRAG_TAG_TASK_LIST) as TaskListFragment?
                val existingFilter = existingTlf?.getFilter()
                val tlf = if (
                    existingFilter != null
                    && existingFilter.areItemsTheSame(newFilter)
                    && existingFilter == newFilter
                // && check if manual sort changed
                ) {
                    existingTlf
                } else {
                    clearUi()
                    TaskListFragment.newTaskListFragment(newFilter)
                }
                val existingTef =
                    supportFragmentManager.findFragmentByTag(FRAG_TAG_TASK_EDIT) as TaskEditFragment?
                val transaction = supportFragmentManager.beginTransaction()
                if (task == null) {
                    if (intent.finishAffinity) {
                        finishAffinity()
                    } else if (existingTef != null) {
                        if (intent.removeTask && intent.broughtToFront) {
                            moveTaskToBack(true)
                        }
                        hideKeyboard()
                        transaction
                            .replace(R.id.detail, newEmptyTaskEditFragment())
                            .runOnCommit {
                                if (isSinglePaneLayout) {
                                    binding.master.visibility = View.VISIBLE
                                    binding.detail.visibility = View.GONE
                                }
                            }
                    }
                } else if (task != existingTef?.task) {
                    existingTef?.save(remove = false)
                    transaction
                        .replace(R.id.detail, newTaskEditFragment(task), FRAG_TAG_TASK_EDIT)
                        .runOnCommit {
                            if (isSinglePaneLayout) {
                                binding.detail.visibility = View.VISIBLE
                                binding.master.visibility = View.GONE
                            }
                        }
                }
                defaultFilterProvider.setLastViewedFilter(newFilter)
                theme
                    .withThemeColor(getFilterColor(newFilter))
                    .applyToContext(this) // must happen before committing fragment
                transaction
                    .replace(R.id.master, tlf, FRAG_TAG_TASK_LIST)
                    .runOnCommit { updateSystemBars(newFilter) }
                    .commit()
            }
            .launchIn(lifecycleScope)
    }

    private fun process(event: MainActivityEvent) = when (event) {
        is MainActivityEvent.ClearTaskEditFragment ->
            viewModel.setTask(null)
    }

    @Deprecated("Deprecated in Java")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_NEW_LIST ->
                if (resultCode == RESULT_OK && data != null) {
                    getParcelableExtra(data, OPEN_FILTER, Filter::class.java)?.let {
                        viewModel.setFilter(it)
                    }
                }
            REQUEST_NEW_PLACE ->
                if (resultCode == RESULT_OK && data != null) {
                    getParcelableExtra(data, EXTRA_PLACE, Place::class.java)?.let {
                        viewModel.setFilter(PlaceFilter(it))
                    }
                }

            else ->
                super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        logIntent("onNewIntent")
        handleIntent()
    }

    private fun clearUi() {
        actionMode?.finish()
        actionMode = null
        viewModel.setDrawerOpen(false)
    }

    private suspend fun getTaskToLoad(filter: Filter?): Task? = when {
        intent.isFromHistory -> null
        intent.hasExtra(CREATE_TASK) -> {
            val source = intent.getStringExtra(CREATE_SOURCE)
            firebase.addTask(source ?: "unknown")
            intent.removeExtra(CREATE_TASK)
            intent.removeExtra(CREATE_SOURCE)
            taskCreator.createWithValues(filter, "")
        }

        intent.hasExtra(OPEN_TASK) -> {
            val task = getParcelableExtra(intent, OPEN_TASK, Task::class.java)
            intent.removeExtra(OPEN_TASK)
            task
        }

        else -> null
    }

    private fun logIntent(caller: String) {
        if (BuildConfig.DEBUG) {
            Timber.d("""
                $caller            
                **********
                broughtToFront: ${intent.broughtToFront}
                isFromHistory: ${intent.isFromHistory}
                flags: ${intent.flagsToString}
                OPEN_FILTER: ${getParcelableExtra(intent, OPEN_FILTER, Filter::class.java)?.let { "${it.title}: $it" }}
                LOAD_FILTER: ${intent.getStringExtra(LOAD_FILTER)}
                OPEN_TASK: ${getParcelableExtra(intent, OPEN_TASK, Task::class.java)}
                CREATE_TASK: ${intent.hasExtra(CREATE_TASK)}
                **********""".trimIndent()
            )
        }
    }

    private fun handleIntent() {
        lifecycleScope.launch {
            val filter = intent.getFilter
                ?: intent.getFilterString?.let { defaultFilterProvider.getFilterFromPreference(it) }
                ?: viewModel.state.value.filter
            val task = getTaskToLoad(filter)
            viewModel.setFilter(filter = filter, task = task)
        }
    }

    private fun updateSystemBars(filter: Filter) {
        with (getFilterColor(filter)) {
            applyToNavigationBar(this@MainActivity)
            applyTaskDescription(this@MainActivity, filter.title ?: getString(R.string.app_name))
        }
    }

    private fun getFilterColor(filter: Filter) =
        if (filter.tint != 0)
            colorProvider.getThemeColor(filter.tint, true)
        else
            theme.themeColor

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

    private suspend fun newTaskEditFragment(task: Task): TaskEditFragment {
        AndroidUtilities.assertMainThread()
        clearUi()
        return coroutineScope {
            withContext(Dispatchers.Default) {
                val freshTask = async { if (task.isNew) task else taskDao.fetch(task.id) ?: task }
                val list = async { defaultFilterProvider.getList(task) }
                val location = async { locationDao.getLocation(task, preferences) }
                val tags = async { tagDataDao.getTags(task) }
                val alarms = async { alarmDao.getAlarms(task) }
                newTaskEditFragment(
                    freshTask.await(),
                    list.await(),
                    location.await(),
                    tags.await(),
                    alarms.await(),
                )
            }
        }
    }

    private val isSinglePaneLayout: Boolean
        get() = !resources.getBoolean(R.bool.two_pane_layout)

    override fun onSupportActionModeStarted(mode: ActionMode) {
        super.onSupportActionModeStarted(mode)
        actionMode = mode
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
        private const val FRAG_TAG_TASK_EDIT = "frag_tag_task_edit"
        private const val FLAG_FROM_HISTORY
                = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
        const val REQUEST_NEW_LIST = 10100
        const val REQUEST_NEW_PLACE = 10104

        val Intent.getFilter: Filter?
            get() = if (isFromHistory) {
                null
            } else {
                getParcelableExtra(this, OPEN_FILTER, Filter::class.java)?.let {
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