package org.tasks.compose.home

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat.getParcelableExtra
import androidx.fragment.compose.AndroidFragment
import androidx.fragment.compose.rememberFragmentState
import androidx.hilt.navigation.compose.hiltViewModel
import com.todoroo.astrid.activity.MainActivity.Companion.OPEN_FILTER
import com.todoroo.astrid.activity.MainActivityViewModel
import com.todoroo.astrid.activity.TaskEditFragment
import com.todoroo.astrid.activity.TaskEditFragment.Companion.EXTRA_TASK
import com.todoroo.astrid.activity.TaskListFragment
import com.todoroo.astrid.activity.TaskListFragment.Companion.EXTRA_FILTER
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.activities.TagSettingsActivity
import org.tasks.billing.PurchaseActivity
import org.tasks.billing.PurchaseActivityViewModel.Companion.EXTRA_NAME_YOUR_PRICE
import org.tasks.billing.PurchaseActivityViewModel.Companion.EXTRA_SHOW_MORE_OPTIONS
import org.tasks.billing.PurchaseActivityViewModel.Companion.EXTRA_SOURCE
import org.tasks.caldav.BaseCaldavCalendarSettingsActivity.Companion.EXTRA_CALDAV_ACCOUNT
import org.tasks.caldav.LocalListSettingsActivity
import org.tasks.compose.drawer.DrawerItem
import org.tasks.compose.drawer.TaskListDrawer
import org.tasks.data.listSettingsClass
import org.tasks.filters.Filter
import org.tasks.filters.FilterProvider
import org.tasks.filters.FilterProvider.Companion.REQUEST_NEW_LIST
import org.tasks.filters.FilterProvider.Companion.REQUEST_NEW_PLACE
import org.tasks.filters.FilterProvider.Companion.REQUEST_NEW_TAGS
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.kmp.org.tasks.compose.TouchSlopMultiplier
import org.tasks.kmp.org.tasks.compose.rememberImeState
import org.tasks.location.LocationPickerActivity
import org.tasks.preferences.MainPreferences
import timber.log.Timber

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun HomeScreen(
    viewModel: MainActivityViewModel = hiltViewModel(LocalActivity.current as ComponentActivity),
    state: MainActivityViewModel.State,
    drawerState: DrawerState,
    showNewFilterDialog: () -> Unit,
    navigator: ThreePaneScaffoldNavigator<Any>,
) {
    val drawerViewModel = viewModel.drawerViewModel
    val drawerViewModelState by drawerViewModel.state.collectAsStateWithLifecycle()
    val currentWindowInsets = WindowInsets.systemBars.asPaddingValues()
    val windowInsets = remember { mutableStateOf(currentWindowInsets) }
    val keyboard = LocalSoftwareKeyboardController.current
    val newList =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data
                    ?.let { getParcelableExtra(it, OPEN_FILTER, Filter::class.java) }
                    ?.let { viewModel.setFilter(it) }
            }
        }

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

    val openTaskAppDialog = remember { mutableStateOf<org.tasks.data.OpenTaskApp?>(null) }
    val guestDialog = remember { mutableStateOf(false) }
    val context = LocalContext.current

    openTaskAppDialog.value?.let { app ->
        AlertDialog(
            onDismissRequest = { openTaskAppDialog.value = null },
            text = {
                Text(
                    text = "To create new lists, open ${app.name} and add them there.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            dismissButton = {
                TextButton(onClick = { openTaskAppDialog.value = null }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    openTaskAppDialog.value = null
                    context.packageManager
                        .getLaunchIntentForPackage(app.packageName)
                        ?.let { context.startActivity(it) }
                }) {
                    Text("Open ${app.name}")
                }
            },
        )
    }

    if (guestDialog.value) {
        AlertDialog(
            onDismissRequest = { guestDialog.value = false },
            title = { Text(stringResource(R.string.upgrade_to_pro)) },
            text = { Text(stringResource(R.string.guest_create_list_message)) },
            dismissButton = {
                TextButton(onClick = {
                    guestDialog.value = false
                    newList.launch(
                        Intent(context, LocalListSettingsActivity::class.java)
                    )
                }) { Text(stringResource(R.string.local_lists)) }
            },
            confirmButton = {
                TextButton(onClick = {
                    guestDialog.value = false
                    context.startActivity(
                        Intent(context, PurchaseActivity::class.java)
                            .putExtra(EXTRA_SOURCE, "guest_create_list")
                            .putExtra(EXTRA_NAME_YOUR_PRICE, false)
                            .putExtra(EXTRA_SHOW_MORE_OPTIONS, false)
                    )
                }) { Text(stringResource(R.string.button_subscribe)) }
            },
        )
    }

    TouchSlopMultiplier {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = isListVisible,
            drawerContent = {
                ModalDrawerSheet(
                    drawerState = drawerState,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                ) {
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()
                    Box(modifier = Modifier.fillMaxSize()) {
                        TaskListDrawer(
                            drawerOpen = drawerState.isOpen,
                            drawerState = drawerViewModelState,
                            onQueryChange = { drawerViewModel.setMenuQuery(it) },
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
                                        drawerViewModel.toggleCollapsed(it.header)
                                    }
                                }
                            },
                            onAddClick = {
                                if (it.openTaskApp != null) {
                                    openTaskAppDialog.value = it.openTaskApp
                                } else {
                                    scope.launch {
                                        drawerState.close()
                                        when (it.header.addIntentRc) {
                                            FilterProvider.REQUEST_NEW_FILTER ->
                                                showNewFilterDialog()

                                            REQUEST_NEW_PLACE ->
                                                newList.launch(Intent(context, LocationPickerActivity::class.java))

                                            REQUEST_NEW_TAGS ->
                                                newList.launch(Intent(context, TagSettingsActivity::class.java))

                                            REQUEST_NEW_LIST ->
                                                when (it.header.subheaderType) {
                                                    NavigationDrawerSubheader.SubheaderType.TASKS -> {
                                                        val account = viewModel.getAccount(it.header.id.toLong())
                                                        if (account != null && viewModel.isTasksGuest()) {
                                                            guestDialog.value = true
                                                        } else if (account != null) {
                                                            newList.launch(
                                                                Intent(context, account.listSettingsClass())
                                                                    .putExtra(EXTRA_CALDAV_ACCOUNT, account)
                                                            )
                                                        }
                                                    }

                                                    NavigationDrawerSubheader.SubheaderType.CALDAV ->
                                                        viewModel
                                                            .getAccount(it.header.id.toLong())
                                                            ?.let {
                                                                newList.launch(
                                                                    Intent(context, it.listSettingsClass())
                                                                        .putExtra(EXTRA_CALDAV_ACCOUNT, it)
                                                                )
                                                            }

                                                    else -> {}
                                                }

                                            else -> Timber.e("Unhandled request code: $it")
                                        }
                                    }
                                }
                            },
                            onErrorClick = {
                                context.startActivity(Intent(context, MainPreferences::class.java))
                            },
                        )

                        SystemBarScrim(
                            modifier = Modifier
                                .windowInsetsTopHeight(WindowInsets.systemBars)
                                .align(Alignment.TopCenter),
                            color = MaterialTheme.colorScheme.surface,
                        )
                        SystemBarScrim(
                            modifier = Modifier
                                .windowInsetsBottomHeight(WindowInsets.systemBars)
                                .align(Alignment.BottomCenter),
                            color = MaterialTheme.colorScheme.surface,
                        )
                    }
                }
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
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

                SystemBarScrim(
                    modifier = Modifier
                        .windowInsetsTopHeight(WindowInsets.systemBars)
                        .align(Alignment.TopCenter),
                )
            }
        }
    }
}
