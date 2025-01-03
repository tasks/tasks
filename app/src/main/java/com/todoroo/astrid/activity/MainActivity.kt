/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.IntentCompat.getParcelableExtra
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.andlib.utility.AndroidUtilities.atLeastR
import com.todoroo.astrid.activity.TaskEditFragment.Companion.newTaskEditFragment
import com.todoroo.astrid.adapter.SubheaderClickHandler
import com.todoroo.astrid.dao.TaskDao
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
import org.tasks.TasksApplication
import org.tasks.activities.GoogleTaskListSettingsActivity
import org.tasks.activities.TagSettingsActivity
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseActivity
import org.tasks.caldav.BaseCaldavCalendarSettingsActivity
import org.tasks.caldav.BaseCaldavCalendarSettingsActivity.Companion.EXTRA_CALDAV_ACCOUNT
import org.tasks.compose.drawer.DrawerAction
import org.tasks.compose.drawer.DrawerItem
import org.tasks.compose.drawer.MenuSearchBar
import org.tasks.compose.drawer.TaskListDrawer
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.Place
import org.tasks.data.entity.Task
import org.tasks.data.getLocation
import org.tasks.data.listSettingsClass
import org.tasks.databinding.TaskListActivityBinding
import org.tasks.dialogs.NewFilterDialog
import org.tasks.dialogs.WhatsNewDialog
import org.tasks.extensions.Context.findActivity
import org.tasks.extensions.Context.nightMode
import org.tasks.extensions.Context.openUri
import org.tasks.extensions.hideKeyboard
import org.tasks.filters.Filter
import org.tasks.filters.FilterProvider
import org.tasks.filters.FilterProvider.Companion.REQUEST_NEW_LIST
import org.tasks.filters.FilterProvider.Companion.REQUEST_NEW_PLACE
import org.tasks.filters.FilterProvider.Companion.REQUEST_NEW_TAGS
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.filters.PlaceFilter
import org.tasks.location.LocationPickerActivity
import org.tasks.location.LocationPickerActivity.Companion.EXTRA_PLACE
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.HelpAndFeedback
import org.tasks.preferences.MainPreferences
import org.tasks.preferences.Preferences
import org.tasks.themes.ColorProvider
import org.tasks.themes.TasksTheme
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
    @Inject lateinit var caldavDao: CaldavDao

    private val viewModel: MainActivityViewModel by viewModels()
    private var currentNightMode = 0
    private var currentPro = false
    private var actionMode: ActionMode? = null
    private lateinit var binding: TaskListActivityBinding

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
            if (viewModel.drawerOpen.collectAsStateWithLifecycle().value) {
                TasksTheme(theme = theme.themeBase.index) {
                    val sheetState = rememberModalBottomSheetState(
                        skipPartiallyExpanded = true,
                        confirmValueChange = { true },
                    )
                    ModalBottomSheet(
                        sheetState = sheetState,
                        containerColor = MaterialTheme.colorScheme.surface,
                        onDismissRequest = { viewModel.closeDrawer() },
                    ) {
                        val state = viewModel.state.collectAsStateWithLifecycle().value
                        val context = LocalContext.current
                        val settingsRequest = rememberLauncherForActivityResult(
                            ActivityResultContracts.StartActivityForResult()
                        ) {
                            context.findActivity()?.recreate()
                        }
                        val scope = rememberCoroutineScope()
                        val bottomSearchBar = atLeastR()
                        TaskListDrawer(
                            arrangement = when {
                                state.menuQuery.isBlank() -> Arrangement.Top
                                bottomSearchBar -> Arrangement.Bottom
                                else -> Arrangement.Top
                            },
                            bottomSearchBar = bottomSearchBar,
                            filters = if (state.menuQuery.isNotEmpty()) state.searchItems else state.drawerItems,
                            onClick = {
                                when (it) {
                                    is DrawerItem.Filter -> {
                                        viewModel.setFilter(it.filter)
                                        scope.launch(Dispatchers.Default) {
                                            sheetState.hide()
                                            viewModel.closeDrawer()
                                        }
                                    }

                                    is DrawerItem.Header -> {
                                        viewModel.toggleCollapsed(it.header)
                                    }
                                }
                            },
                            onAddClick = {
                                scope.launch(Dispatchers.Default) {
                                    sheetState.hide()
                                    viewModel.closeDrawer()
                                    when (it.header.addIntentRc) {
                                        FilterProvider.REQUEST_NEW_FILTER ->
                                            NewFilterDialog.newFilterDialog().show(
                                                supportFragmentManager,
                                                SubheaderClickHandler.FRAG_TAG_NEW_FILTER
                                            )

                                        REQUEST_NEW_PLACE ->
                                            startActivityForResult(
                                                Intent(
                                                    this@MainActivity,
                                                    LocationPickerActivity::class.java
                                                ),
                                                REQUEST_NEW_PLACE
                                            )

                                        REQUEST_NEW_TAGS ->
                                            startActivityForResult(
                                                Intent(
                                                    this@MainActivity,
                                                    TagSettingsActivity::class.java
                                                ),
                                                REQUEST_NEW_LIST
                                            )

                                        REQUEST_NEW_LIST -> {
                                            val account =
                                                caldavDao.getAccount(it.header.id.toLong()) ?: return@launch
                                            when (it.header.subheaderType) {
                                                NavigationDrawerSubheader.SubheaderType.GOOGLE_TASKS ->
                                                    startActivityForResult(
                                                        Intent(
                                                            this@MainActivity,
                                                            GoogleTaskListSettingsActivity::class.java
                                                        )
                                                            .putExtra(
                                                                EXTRA_CALDAV_ACCOUNT,
                                                                account
                                                            ),
                                                        REQUEST_NEW_LIST
                                                    )

                                                NavigationDrawerSubheader.SubheaderType.CALDAV,
                                                NavigationDrawerSubheader.SubheaderType.TASKS ->
                                                    startActivityForResult(
                                                        Intent(
                                                            this@MainActivity,
                                                            account.listSettingsClass()
                                                        )
                                                            .putExtra(
                                                                EXTRA_CALDAV_ACCOUNT,
                                                                account
                                                            ),
                                                        REQUEST_NEW_LIST
                                                    )

                                                else -> {}
                                            }
                                        }

                                        else -> Timber.e("Unhandled request code: $it")
                                    }
                                }
                            },
                            onErrorClick = {
                                context.startActivity(Intent(context, MainPreferences::class.java))
                            },
                            searchBar = {
                                MenuSearchBar(
                                    begForMoney = state.begForMoney,
                                    onDrawerAction = {
                                        viewModel.closeDrawer()
                                        when (it) {
                                            DrawerAction.PURCHASE ->
                                                if (TasksApplication.IS_GENERIC)
                                                    context.openUri(R.string.url_donate)
                                                else
                                                    context.startActivity(
                                                        Intent(
                                                            context,
                                                            PurchaseActivity::class.java
                                                        )
                                                    )

                                            DrawerAction.SETTINGS ->
                                                settingsRequest.launch(
                                                    Intent(
                                                        context,
                                                        MainPreferences::class.java
                                                    )
                                                )

                                            DrawerAction.HELP_AND_FEEDBACK ->
                                                context.startActivity(
                                                    Intent(
                                                        context,
                                                        HelpAndFeedback::class.java
                                                    )
                                                )
                                        }
                                    },
                                    query = state.menuQuery,
                                    onQueryChange = { viewModel.queryMenu(it) },
                                )
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
                } else if (task == existingTef.task) {
                    transaction
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
        viewModel.closeDrawer()
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