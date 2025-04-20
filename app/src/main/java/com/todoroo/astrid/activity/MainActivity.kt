/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat.getParcelableExtra
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.todoroo.astrid.adapter.SubheaderClickHandler
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.service.TaskCreator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.compose.HomeDestination
import org.tasks.compose.home.HomeScreen
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.Place
import org.tasks.data.entity.Task
import org.tasks.dialogs.NewFilterDialog
import org.tasks.extensions.Context.nightMode
import org.tasks.extensions.broughtToFront
import org.tasks.extensions.flagsToString
import org.tasks.extensions.isFromHistory
import org.tasks.filters.Filter
import org.tasks.filters.FilterProvider.Companion.REQUEST_NEW_LIST
import org.tasks.filters.FilterProvider.Companion.REQUEST_NEW_PLACE
import org.tasks.filters.PlaceFilter
import org.tasks.location.LocationPickerActivity.Companion.EXTRA_PLACE
import org.tasks.preferences.DefaultFilterProvider
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        theme.themeBase.set(this)
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
            TasksTheme(
                theme = theme.themeBase.index,
                primary = theme.themeColor.primaryColor,
            ) {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = HomeDestination,
                ) {
                    composable<HomeDestination> {
                        val scope = rememberCoroutineScope()
                        val state = viewModel.state.collectAsStateWithLifecycle().value
                        val drawerState = rememberDrawerState(
                            initialValue = DrawerValue.Closed,
                            confirmStateChange = {
                                viewModel.setDrawerState(it == DrawerValue.Open)
                                true
                            }
                        )
                        val navigator = rememberListDetailPaneScaffoldNavigator(
                            calculatePaneScaffoldDirective(
                                windowAdaptiveInfo = currentWindowAdaptiveInfo(),
                            ).copy(
                                horizontalPartitionSpacerSize = 0.dp,
                                verticalPartitionSpacerSize = 0.dp,
                            )
                        )
                        val keyboard = LocalSoftwareKeyboardController.current
                        LaunchedEffect(state.task) {
                            if (state.task == null) {
                                if (intent.finishAffinity) {
                                    finishAffinity()
                                } else {
                                    if (intent.removeTask && intent.broughtToFront) {
                                        moveTaskToBack(true)
                                    }
                                    keyboard?.hide()
                                    navigator.navigateTo(pane = ThreePaneScaffoldRole.Secondary)
                                }
                            } else {
                                navigator.navigateTo(pane = ThreePaneScaffoldRole.Primary)
                            }
                        }

                        val isDetailVisible =
                            navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded
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
                            if (state.task == null) {
                                keyboard?.hide()
                            }
                            drawerState.close()
                        }
                        HomeScreen(
                            state = state,
                            drawerState = drawerState,
                            navigator = navigator,
                            newList = {
                                startActivityForResult(
                                    Intent(this@MainActivity, it),
                                    REQUEST_NEW_LIST
                                )
                            },
                            showNewFilterDialog = {
                                NewFilterDialog.newFilterDialog().show(
                                    supportFragmentManager,
                                    SubheaderClickHandler.FRAG_TAG_NEW_FILTER
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
                |$caller
                |**********
                |broughtToFront: ${intent.broughtToFront}
                |isFromHistory: ${intent.isFromHistory}
                |flags: ${intent.flagsToString}
                ${intent?.extras?.keySet()?.joinToString("\n") { "|$it: ${intent.extras?.get(it)}" } ?: "|NO EXTRAS"}
                |**********""".trimMargin()
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
        Timber.d("onResume")
        if (currentNightMode != nightMode || currentPro != inventory.hasPro) {
            restartActivity()
            return
        }
    }

    override fun onPause() {
        super.onPause()
        Timber.d("onPause")
    }

    override fun onSupportActionModeStarted(mode: ActionMode) {
        super.onSupportActionModeStarted(mode)
        actionMode = mode
    }

    fun restartActivity() {
        finish()
        startActivity(
            Intent(this, MainActivity::class.java),
            ActivityOptions.makeCustomAnimation(
                this@MainActivity,
                android.R.anim.fade_in, android.R.anim.fade_out
            ).toBundle()
        )
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
    }
}
