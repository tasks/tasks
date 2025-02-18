/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.HingePolicy
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat.getParcelableExtra
import androidx.fragment.compose.AndroidFragment
import androidx.fragment.compose.rememberFragmentState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.todoroo.andlib.utility.AndroidUtilities.atLeastR
import com.todoroo.astrid.activity.TaskEditFragment.Companion.EXTRA_TASK
import com.todoroo.astrid.activity.TaskListFragment.Companion.EXTRA_FILTER
import com.todoroo.astrid.adapter.SubheaderClickHandler
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.service.TaskCreator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.TasksApplication
import org.tasks.activities.TagSettingsActivity
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseActivity
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
import org.tasks.data.listSettingsClass
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
import timber.log.Timber
import javax.inject.Inject

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
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
    @Inject lateinit var firebase: Firebase
    @Inject lateinit var caldavDao: CaldavDao

    private val viewModel: MainActivityViewModel by viewModels()
    private var currentNightMode = 0
    private var currentPro = false
    private var actionMode: ActionMode? = null

    /** @see android.app.Activity.onCreate
     */
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        theme.applyTheme(this)
        currentNightMode = nightMode
        currentPro = inventory.hasPro

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT
            )
        )

        setContent {
            TasksTheme(theme = theme.themeBase.index) {
                val navigator = rememberListDetailPaneScaffoldNavigator(
                    calculatePaneScaffoldDirective(
                        windowAdaptiveInfo = currentWindowAdaptiveInfo(),
                        verticalHingePolicy = HingePolicy.AlwaysAvoid,
                    ).copy(
                        horizontalPartitionSpacerSize = 0.dp,
                        verticalPartitionSpacerSize = 0.dp,
                    ),

                )
                val state = viewModel.state.collectAsStateWithLifecycle().value
                val isListVisible =
                    navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded
                val isDetailVisible =
                    navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded
                val scope = rememberCoroutineScope()

                LaunchedEffect(state.task) {
                    if (state.task == null) {
                        if (intent.finishAffinity) {
                            finishAffinity()
                        } else {
                            if (intent.removeTask && intent.broughtToFront) {
                                moveTaskToBack(true)
                            }
                            hideKeyboard()
                            navigator.navigateTo(pane = ThreePaneScaffoldRole.Secondary)
                        }
                    } else {
                        navigator.navigateTo(pane = ThreePaneScaffoldRole.Primary)
                    }
                }

                BackHandler(enabled = state.task == null) {
                    Timber.d("onBackPressed")
                    if (intent.finishAffinity) {
                        finishAffinity()
                    } else if (isDetailVisible && navigator.canNavigateBack()) {
                        scope.launch {
                            navigator.navigateBack()
                        }
                    } else {
                        finish()
                        if (!preferences.getBoolean(R.string.p_open_last_viewed_list, true)) {
                            runBlocking {
                                viewModel.resetFilter()
                            }
                        }
                    }
                }
                LaunchedEffect(state.filter, state.task) {
                    actionMode?.finish()
                    actionMode = null
                    viewModel.closeDrawer()
                }
                ListDetailPaneScaffold(
                    directive = navigator.scaffoldDirective,
                    value = navigator.scaffoldValue,
                    listPane = {
                        key (state.filter) {
                            AndroidFragment<TaskListFragment>(
                                fragmentState = rememberFragmentState(),
                                arguments = remember(state.filter) {
                                    Bundle()
                                        .apply { putParcelable(EXTRA_FILTER, state.filter) }
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    },
                    detailPane = {
                        if (state.task == null) {
                            if (isListVisible && isDetailVisible) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painter = painterResource(org.tasks.kmp.R.drawable.ic_launcher_no_shadow_foreground),
                                        contentDescription = null,
                                        modifier = Modifier.size(192.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            key (state.task) {
                                AndroidFragment<TaskEditFragment>(
                                    fragmentState = rememberFragmentState(),
                                    arguments = remember(state.task) {
                                        Bundle()
                                            .apply { putParcelable(EXTRA_TASK, state.task) }
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    },
                )

                if (viewModel.drawerOpen.collectAsStateWithLifecycle().value) {
                    val sheetState = rememberModalBottomSheetState(
                        skipPartiallyExpanded = true,
                        confirmValueChange = { true },
                    )
                    ModalBottomSheet(
                        modifier = Modifier.statusBarsPadding(),
                        sheetState = sheetState,
                        containerColor = MaterialTheme.colorScheme.surface,
                        onDismissRequest = { viewModel.closeDrawer() },
                        contentWindowInsets = {
                            WindowInsets(
                                left = 0,
                                top = 0,
                                right = 0,
                                bottom = 0
                            )
                        },
                    ) {
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
                                                caldavDao.getAccount(it.header.id.toLong())
                                                    ?: return@launch
                                            when (it.header.subheaderType) {
                                                NavigationDrawerSubheader.SubheaderType.CALDAV,
                                                NavigationDrawerSubheader.SubheaderType.TASKS,
                                                    ->
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
        logIntent("onCreate")
        handleIntent()
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
        private const val FRAG_TAG_WHATS_NEW = "frag_tag_whats_new"
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
