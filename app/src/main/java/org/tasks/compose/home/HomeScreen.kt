package org.tasks.compose.home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.compose.AndroidFragment
import androidx.fragment.compose.rememberFragmentState
import androidx.hilt.navigation.compose.hiltViewModel
import com.todoroo.astrid.activity.MainActivityViewModel
import com.todoroo.astrid.activity.TaskEditFragment
import com.todoroo.astrid.activity.TaskEditFragment.Companion.EXTRA_TASK
import com.todoroo.astrid.activity.TaskListFragment
import com.todoroo.astrid.activity.TaskListFragment.Companion.EXTRA_FILTER
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.TasksApplication
import org.tasks.activities.TagSettingsActivity
import org.tasks.billing.PurchaseActivity
import org.tasks.compose.drawer.DrawerAction
import org.tasks.compose.drawer.DrawerItem
import org.tasks.compose.drawer.MenuSearchBar
import org.tasks.compose.drawer.TaskListDrawer
import org.tasks.data.listSettingsClass
import org.tasks.extensions.Context.openUri
import org.tasks.filters.FilterProvider
import org.tasks.filters.FilterProvider.Companion.REQUEST_NEW_LIST
import org.tasks.filters.FilterProvider.Companion.REQUEST_NEW_PLACE
import org.tasks.filters.FilterProvider.Companion.REQUEST_NEW_TAGS
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.kmp.org.tasks.compose.TouchSlopMultiplier
import org.tasks.kmp.org.tasks.compose.rememberImeState
import org.tasks.location.LocationPickerActivity
import org.tasks.preferences.HelpAndFeedback
import org.tasks.preferences.MainPreferences
import timber.log.Timber

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun HomeScreen(
    viewModel: MainActivityViewModel = hiltViewModel(),
    newList: suspend (Class<out Activity>) -> Unit,
    state: MainActivityViewModel.State,
    drawerState: DrawerState,
    showNewFilterDialog: () -> Unit,
    navigator: ThreePaneScaffoldNavigator<Any>,
) {
    val currentWindowInsets = WindowInsets.systemBars.asPaddingValues()
    val windowInsets = remember { mutableStateOf(currentWindowInsets) }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(currentWindowInsets) {
        Timber.d("insets: $currentWindowInsets")
        if (currentWindowInsets.calculateTopPadding() != 0.dp || currentWindowInsets.calculateBottomPadding() != 0.dp) {
            windowInsets.value = currentWindowInsets
        }
    }
    val isListVisible =
        navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded
    val isDetailVisible =
        navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded

    TouchSlopMultiplier {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = isListVisible,
            drawerContent = {
                ModalDrawerSheet(
                    drawerState = drawerState,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                ) {
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()
                    TaskListDrawer(
                        arrangement = if (state.menuQuery.isBlank()) Arrangement.Top else Arrangement.Bottom,
                        filters = if (state.menuQuery.isNotEmpty()) state.searchItems else state.drawerItems,
                        onClick = {
                            when (it) {
                                is DrawerItem.Filter -> {
                                    viewModel.setFilter(it.filter)
                                    scope.launch {
                                        drawerState.close()
                                        keyboard?.hide()
                                    }
                                }

                                is DrawerItem.Header -> {
                                    viewModel.toggleCollapsed(it.header)
                                }
                            }
                        },
                        onAddClick = {
                            scope.launch {
                                drawerState.close()
                                when (it.header.addIntentRc) {
                                    FilterProvider.REQUEST_NEW_FILTER ->
                                        showNewFilterDialog()

                                    REQUEST_NEW_PLACE ->
                                        newList(LocationPickerActivity::class.java)

                                    REQUEST_NEW_TAGS ->
                                        newList(TagSettingsActivity::class.java)

                                    REQUEST_NEW_LIST ->
                                        when (it.header.subheaderType) {
                                            NavigationDrawerSubheader.SubheaderType.CALDAV,
                                            NavigationDrawerSubheader.SubheaderType.TASKS ->
                                                viewModel
                                                    .getAccount(it.header.id.toLong())
                                                    ?.let { newList(it.listSettingsClass()) }

                                            else -> {}
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
                                    scope.launch {
                                        drawerState.close()
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

                                            DrawerAction.HELP_AND_FEEDBACK ->
                                                context.startActivity(
                                                    Intent(
                                                        context,
                                                        HelpAndFeedback::class.java
                                                    )
                                                )
                                        }
                                    }
                                },
                                query = state.menuQuery,
                                onQueryChange = { viewModel.queryMenu(it) },
                            )
                        },
                    )
                }
            }
        ) {
            val scope = rememberCoroutineScope()
            ListDetailPaneScaffold(
                directive = navigator.scaffoldDirective,
                value = navigator.scaffoldValue,
                listPane = {
                    key (state.filter) {
                        val fragment = remember { mutableStateOf<TaskListFragment?>(null) }
                        val keyboardOpen = rememberImeState()
                        AndroidFragment<TaskListFragment>(
                            fragmentState = rememberFragmentState(),
                            arguments = remember(state.filter) {
                                Bundle()
                                    .apply { putParcelable(EXTRA_FILTER, state.filter) }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .imePadding(),
                        ) { tlf ->
                            fragment.value = tlf
                            tlf.applyInsets(windowInsets.value)
                            tlf.setNavigationClickListener {
                                scope.launch { drawerState.open() }
                            }
                        }
                        LaunchedEffect(fragment, windowInsets, keyboardOpen.value) {
                            fragment.value?.applyInsets(
                                if (keyboardOpen.value) {
                                    PaddingValues(
                                        top = windowInsets.value.calculateTopPadding(),
                                    )
                                } else {
                                    windowInsets.value
                                }
                            )
                        }
                    }
                },
                detailPane = {
                    val direction = LocalLayoutDirection.current
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = windowInsets.value.calculateTopPadding(),
                                start = windowInsets.value.calculateStartPadding(direction),
                                end = windowInsets.value.calculateEndPadding(direction),
                                bottom = if (rememberImeState().value)
                                    WindowInsets.ime.asPaddingValues().calculateBottomPadding()
                                else
                                    windowInsets.value.calculateBottomPadding()
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.task == null) {
                            if (isListVisible && isDetailVisible) {
                                Icon(
                                    painter = painterResource(org.tasks.kmp.R.drawable.ic_launcher_no_shadow_foreground),
                                    contentDescription = null,
                                    modifier = Modifier.size(192.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            key(state.task) {
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
                    }
                },
            )
        }
    }
}
