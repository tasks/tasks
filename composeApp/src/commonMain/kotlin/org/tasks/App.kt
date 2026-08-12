package org.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.PaneExpansionState
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.currentWindowDpSize
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.todoroo.astrid.alarms.AlarmService
import org.tasks.compose.pickers.DueDatePickerSheet
import org.tasks.compose.pickers.SnoozeDialog
import org.tasks.compose.pickers.alarmFromSelection
import org.tasks.compose.pickers.alarmToSelection
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.DatePickerPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.tasks.data.SubtaskTreeRegistry
import org.tasks.data.deletions
import org.koin.compose.viewmodel.koinViewModel
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Reporting
import org.tasks.auth.OAuthProvider
import org.tasks.auth.TasksServerEnvironment
import org.tasks.billing.SubscriptionProvider
import org.tasks.caldav.TasksAccountDataRepository
import org.tasks.compose.components.AnimatedBanner
import org.tasks.compose.NavigationBarScrim
import org.tasks.compose.PlatformBackHandler
import org.tasks.compose.SignInProvider
import org.tasks.compose.SignInProviderDialog
import org.tasks.compose.StatusBarScrim
import org.tasks.compose.WelcomeScreenLayout
import org.tasks.compose.accounts.AddAccountScreen
import org.tasks.compose.accounts.AddAccountViewModel
import org.tasks.compose.accounts.Platform
import org.tasks.compose.chips.ChipDataProvider
import org.tasks.compose.tasklist.RowState
import org.tasks.compose.tasklist.TaskRow
import org.tasks.compose.tasklist.rowState
import org.tasks.compose.drawer.DrawerItem
import org.tasks.compose.drawer.DrawerItemInset
import org.tasks.compose.drawer.SearchButtonSize
import org.tasks.compose.drawer.TaskListDrawer
import org.tasks.compose.horizontalResizeCursor
import org.tasks.compose.platformNavigationBarsPadding
import org.tasks.compose.platformSidebarInsets
import org.tasks.compose.platformStatusBarInsets
import org.tasks.compose.pricing.PricingMode
import org.tasks.compose.pricing.PricingScreen
import org.tasks.compose.settings.CaldavAccountSettingsDetail
import org.tasks.compose.settings.CaldavAccountSettingsPane
import org.tasks.compose.settings.DesktopProScreen
import org.tasks.compose.settings.EtebaseAccountSettingsDetail
import org.tasks.compose.settings.EtebaseAccountSettingsPane
import org.tasks.compose.settings.GoogleTasksAccountSettingsDetail
import org.tasks.compose.settings.GoogleTasksAccountSettingsPane
import org.tasks.compose.settings.MicrosoftAccountSettingsDetail
import org.tasks.compose.settings.MicrosoftAccountSettingsPane
import org.tasks.compose.settings.HelpAndFeedbackDetail
import org.tasks.compose.settings.NotificationsDetail
import org.tasks.compose.settings.TaskDefaultsDetail
import org.tasks.compose.settings.LinkDesktopScreen
import org.tasks.compose.settings.ListSettingsScreen
import org.tasks.compose.settings.TagSettingsScreen
import org.tasks.compose.settings.LocalAccountSettingsDetail
import org.tasks.compose.settings.LocalAccountSettingsPane
import org.tasks.compose.settings.MainSettingsScreen
import org.tasks.compose.settings.ManageSubscriptionSheetContent
import org.tasks.compose.settings.OpenTaskAccountSettingsDetail
import org.tasks.compose.settings.OpenTaskAccountSettingsPane
import org.tasks.compose.settings.ProCardState
import org.tasks.compose.settings.SettingsMenuButton
import org.tasks.compose.settings.SettingsPane
import org.tasks.compose.settings.TasksAccountSettingsDetail
import org.tasks.compose.settings.TasksAccountSettingsPane
import org.tasks.compose.sort.BottomSheetContent
import org.tasks.compose.sort.SortPicker
import org.tasks.compose.sort.SortSheetContent
import org.tasks.compose.sort.completedOptions
import org.tasks.compose.sort.groupOptions
import org.tasks.compose.sort.subtaskOptions
import org.tasks.data.TaskContainer
import org.tasks.data.UUIDHelper
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.TagData
import org.tasks.data.getAccountForNewList
import org.tasks.data.getLocalList
import org.tasks.extensions.guarded
import org.tasks.filters.CaldavFilter
import org.tasks.filters.EmptyFilter
import org.tasks.filters.Filter
import org.tasks.filters.FilterProvider.Companion.REQUEST_NEW_TAGS
import org.tasks.filters.key
import org.tasks.filters.MyTasksFilter
import org.tasks.filters.TagFilter
import org.tasks.kmp.org.tasks.themes.ColorProvider
import org.tasks.compose.rememberDateFormatter
import org.tasks.tasklist.SectionedDataSource
import org.tasks.tasklist.TasksResults
import org.tasks.themes.BLUE
import org.tasks.themes.TasksTheme
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.reminders.SNOOZE_PICKER_OFFSET
import org.tasks.viewmodel.AppViewModel
import org.tasks.viewmodel.CaldavCalendarSettingsViewModel
import org.tasks.viewmodel.DrawerViewModel
import org.tasks.viewmodel.EtebaseCalendarSettingsViewModel
import org.tasks.viewmodel.LocalListSettingsViewModel
import org.tasks.viewmodel.TagSettingsViewModel
import org.tasks.viewmodel.FilterPickerViewModel
import org.tasks.viewmodel.ListSettingsViewModel
import org.tasks.viewmodel.GoogleTaskListSettingsViewModel
import org.tasks.viewmodel.MicrosoftListSettingsViewModel
import org.tasks.viewmodel.MainSettingsViewModel
import org.tasks.viewmodel.PendingTaskSaves
import org.tasks.viewmodel.ProCardViewModel
import org.tasks.viewmodel.SortSettingsViewModel
import org.tasks.viewmodel.TaskEditViewModel
import org.tasks.viewmodel.TaskListViewModel
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.no_task_selected
import tasks.kmp.generated.resources.add_account
import tasks.kmp.generated.resources.add_platform_account
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.dismiss
import tasks.kmp.generated.resources.local_list_description
import tasks.kmp.generated.resources.local_list_title
import tasks.kmp.generated.resources.caldav
import tasks.kmp.generated.resources.etesync
import tasks.kmp.generated.resources.failed_to_save_task
import tasks.kmp.generated.resources.gtasks_GPr_header
import tasks.kmp.generated.resources.not_available_desktop
import tasks.kmp.generated.resources.ok
import tasks.kmp.generated.resources.resize_panes
import tasks.kmp.generated.resources.settings
import tasks.kmp.generated.resources.subscription_not_found
import tasks.kmp.generated.resources.url_google_play
import tasks.kmp.generated.resources.url_sponsor
import tasks.kmp.generated.resources.wrong_account
import kotlin.math.roundToInt

@Serializable
data object WelcomeDestination : NavKey

@Serializable
data object AddAccountDestination : NavKey

@Serializable
data object TaskListDestination : NavKey

@Serializable
data class TaskEditDestination(
    val taskId: Long,
    val remoteId: String,
    val listId: Long? = null,
    val tagUuid: String? = null,
    val isSubtaskDraft: Boolean = false,
) : NavKey

@Serializable
data object CaldavSignInDestination : NavKey

@Serializable
data object EtebaseSignInDestination : NavKey

@Serializable
data object SettingsDestination : NavKey

@Serializable
data object LinkDesktopDestination : NavKey

@Serializable
data class DesktopProDestination(val source: String? = null) : NavKey

@Serializable
data class PricingDestination(
    val mode: PricingMode = PricingMode.BOTH,
    val source: String = AnalyticsEvents.SOURCE_SETTINGS,
) : NavKey

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun App(
    openUrl: (String) -> Unit = {},
    environments: List<TasksServerEnvironment.Environment> = emptyList(),
    currentEnvironment: String = TasksServerEnvironment.ENV_PRODUCTION,
    onSelectEnvironment: (String) -> Unit = {},
) {
    val uriHandler = remember(openUrl) {
        object : androidx.compose.ui.platform.UriHandler {
            override fun openUri(uri: String) {
                val normalized = when {
                    uri.contains("://") || uri.startsWith("mailto:") -> uri
                    else -> "https://$uri"
                }
                openUrl(normalized)
            }
        }
    }
    TasksTheme {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalUriHandler provides uriHandler,
        ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val appViewModel = koinViewModel<AppViewModel>()
            val configuration = koinInject<PlatformConfiguration>()
            val reporting = koinInject<Reporting>()
            val pendingSaves = koinInject<PendingTaskSaves>()
            val subtaskTrees = koinInject<SubtaskTreeRegistry>()
            val hasAccount by appViewModel.hasAccount.collectAsState()
            val subscriptionProvider = koinInject<SubscriptionProvider>()
            val subscriptionInfo by subscriptionProvider.subscription.collectAsState(initial = null)

            // Layout geometry is read off the main thread; hold the first frame until it lands
            // rather than drawing defaults and animating to the stored values a frame later. Read
            // from AppViewModel, which is built either way - gating on a view model that also runs
            // the drawer's filter queries meant running those queries behind the welcome screen.
            //
            // It only ever goes from null to a value, so this gate cannot close again: everything
            // below - the back stack, and every view model hanging off it - lives inside this
            // branch, and re-entering the gate would dispose the lot.
            val layout = appViewModel.layout.collectAsState().value

            SnoozeRequests()

            if (hasAccount == null || layout == null) {
                return@Surface
            }

            val backStack = rememberNavBackStack(
                SavedStateConfiguration {
                    serializersModule = SerializersModule {
                        polymorphic(NavKey::class) {
                            subclass(WelcomeDestination::class, WelcomeDestination.serializer())
                            subclass(AddAccountDestination::class, AddAccountDestination.serializer())
                            subclass(CaldavSignInDestination::class, CaldavSignInDestination.serializer())
                            subclass(EtebaseSignInDestination::class, EtebaseSignInDestination.serializer())
                            subclass(TaskListDestination::class, TaskListDestination.serializer())
                            subclass(TaskEditDestination::class, TaskEditDestination.serializer())
                            subclass(SettingsDestination::class, SettingsDestination.serializer())
                            subclass(LinkDesktopDestination::class, LinkDesktopDestination.serializer())
                            subclass(DesktopProDestination::class, DesktopProDestination.serializer())
                            subclass(PricingDestination::class, PricingDestination.serializer())
                        }
                    }
                },
                if (hasAccount == true) TaskListDestination else WelcomeDestination,
            )

            // This also runs on a fresh composition after the activity is recreated, when the back
            // stack has already been restored and hasAccount hasn't actually changed. So it has to
            // decide from the stack's contents, not from "is the top the one screen I expect":
            // anything else wipes a restored settings, pricing or sign-in screen on every rotation.
            // Driven off the stack as well as off hasAccount, not keyed on hasAccount alone. The
            // Add Account exemption below defers the decision, and hasAccount comes from a
            // watch-the-database flow that cannot re-emit the same value - so keying on it left the
            // deferred case never revisited: backing out of Add Account with no account landed on a
            // task list with no view models, nothing rendered in either pane and no way back to
            // onboarding.
            LaunchedEffect(Unit) {
                snapshotFlow { hasAccount to backStack.isAddingAccount() }
                    .distinctUntilChanged()
                    .collect { (account, addingAccount) ->
                        when (account) {
                            // Onboarding is over. Everything else - settings, pricing, a sign-in
                            // flow started from settings - is reachable with an account and stays
                            // put.
                            true -> if (backStack.any { it is WelcomeDestination }) {
                                backStack.replaceAllWith(TaskListDestination)
                            }
                            // Only send the user back to onboarding if they aren't already somewhere
                            // in it. Onboarding always keeps WelcomeDestination at the root, and
                            // that is the only thing separating it from the same sign-in or pricing
                            // screen opened from settings by a user whose last account has just gone
                            // away.
                            //
                            // Adding an account is exempt even outside onboarding. It is reachable
                            // from settings, from the drawer's sign-in row and from the new-list
                            // dialog, and an account disappearing underneath it - removed on another
                            // device, or a sign-in that failed after partially creating one - is
                            // exactly when the user is trying to add one. Wiping the stack there
                            // takes away the screen they are using along with any sign-in already in
                            // flight. Leaving it is what makes revisiting this necessary: the wipe
                            // happens when they leave that flow instead.
                            false -> if (
                                backStack.none { it is WelcomeDestination } && !addingAccount
                            ) {
                                backStack.replaceAllWith(WelcomeDestination)
                            }
                            null -> {}
                        }
                    }
            }

            // Not built during onboarding. All three start querying the moment they are constructed
            // - the task list from its own state flow, the drawer and the picker from init - and
            // none of them has an account to query for yet, so building them unconditionally had
            // them running the task list, drawer and list-picker queries behind the welcome screen.
            val accountScope = rememberAccountViewModelStoreOwner(hasAccount == true)
            val taskListViewModel = if (hasAccount == true) {
                koinViewModel<TaskListViewModel>(viewModelStoreOwner = accountScope)
            } else {
                null
            }
            val drawerViewModel = if (hasAccount == true) {
                koinViewModel<DrawerViewModel>(viewModelStoreOwner = accountScope)
            } else {
                null
            }
            // Scoped to the app rather than to the task edit nav entry. Inside the entry it was
            // rebuilt on every task open, re-running listPickerItems() and allLists() each time.
            val listPickerViewModel = if (hasAccount == true) {
                koinViewModel<FilterPickerViewModel>(
                    viewModelStoreOwner = accountScope,
                    key = "list_picker",
                    parameters = { org.koin.core.parameter.parametersOf(true) },
                )
            } else {
                null
            }

            val topDestination = backStack.lastOrNull()
            val showChrome = topDestination is TaskListDestination || topDestination is TaskEditDestination
            val hasDetailOpen = topDestination is TaskEditDestination

            val density = LocalDensity.current
            val layoutDirection = LocalLayoutDirection.current
            val windowWidth = currentWindowDpSize().width
            val wideLayout = windowWidth >= 600.dp

            val storedSidebarWidth = layout.sidebarWidth
            val storedPaneWidth = layout.taskListPaneWidth
            val sidebarExpanded = layout.sidebarExpanded
            // The start edge, not the left one. The sidebar is laid out at the start, so in RTL it
            // has to reserve the right-hand inset - and this is no longer only padding: every width
            // decision below is computed from it, so taking the wrong side told the app it had space
            // it didn't and let it pick a two-pane layout that squeezed the editor under its minimum.
            val sidebarCutout = platformSidebarInsets().calculateStartPadding(layoutDirection)

            // With a task open the panes come first, and the sidebar gives way in steps rather than
            // all at once: expanded sidebar, then rail, then nothing. Every step has to leave room
            // for both panes - a sidebar of any width beside a lone editor is clutter, not
            // navigation - so the only thing that changes between the steps is the footprint.
            val sidebarNeeds = if (hasDetailOpen) TwoPaneMinWidth else TaskListPaneMinWidth
            // Widths are clamped for display only. The stored preference is never overwritten by a
            // window too narrow to honour it, so it comes back when there's room again.
            val maxSidebarWidth =
                (windowWidth - sidebarCutout - PanelGutterWidth - sidebarNeeds)
                    .coerceAtLeast(SidebarMinWidth)
            // Non-null only while the user is dragging the sidebar handle.
            var draggedSidebarWidth by remember { mutableStateOf<Dp?>(null) }
            val sidebarWidth = (draggedSidebarWidth ?: storedSidebarWidth)
                .coerceIn(SidebarMinWidth, maxSidebarWidth)

            val expandedSidebarSpace = sidebarCutout + sidebarWidth + PanelGutterWidth
            val railSidebarSpace = sidebarCutout + SidebarRailWidth
            // The rail is the cheapest footprint there is, so if the panes don't fit beside it they
            // don't fit beside any sidebar. Both rungs have to be monotonic in the sidebar's own
            // state, or the menu button inverts: testing "does the *current* footprint fit" made
            // expanding hide the sidebar wherever the drawer didn't fit but the rail did. Which
            // rung applies never depends on sidebarExpanded, and - because maxSidebarWidth is
            // clamped against the same sidebarNeeds - never on the live drag width either.
            val showSidebar = wideLayout &&
                (!hasDetailOpen || windowWidth >= TwoPaneWindowMinWidth) &&
                windowWidth - railSidebarSpace >= sidebarNeeds
            // maxSidebarWidth is clamped up to SidebarMinWidth, so there is a band of widths - with
            // a task open, roughly 840dp to 888dp - where the rail fits beside the panes but no
            // sidebar can ever expand beside them. Everything that offers to expand has to know,
            // or it offers something that cannot happen.
            val canExpandSidebar = showSidebar &&
                windowWidth - expandedSidebarSpace >= sidebarNeeds
            val sidebarShownExpanded = canExpandSidebar && sidebarExpanded
            // Stepping down to the rail is animated, but only the sidebar itself animates.
            val sidebarExpandProgress by animateFloatAsState(
                targetValue = if (sidebarShownExpanded) 1f else 0f,
                label = "sidebarExpand",
            )
            val currentSidebarWidth = lerp(SidebarRailWidth, sidebarWidth, sidebarExpandProgress)
            // Where the sidebar is going, never where it currently is. Everything below decides how
            // many panes there are, and the scene strategy tears the editor pane down and rebuilds
            // it whenever that answer changes - so an answer derived from a value that is mid-flight
            // flips twice per animation. Opening a task at ~850dp collapses the sidebar to a rail,
            // and reading the in-flight width there said "one pane" for the first half of the
            // collapse and "two panes" for the rest. The panes are laid out inside whatever the
            // animating sidebar leaves them either way; it is only the count that has to hold still.
            val sidebarSpace = if (showSidebar) {
                sidebarCutout +
                    if (sidebarShownExpanded) sidebarWidth + PanelGutterWidth else SidebarRailWidth
            } else {
                0.dp
            }
            val paneAreaWidth = windowWidth - sidebarSpace
            // The window size class decides whether two panes are wanted; the space actually left
            // over beside the sidebar decides whether they fit.
            val twoPaneLayout =
                windowWidth >= TwoPaneWindowMinWidth && paneAreaWidth >= TwoPaneMinWidth
            val maxListPaneWidth = (paneAreaWidth - PanelGutterWidth - TaskEditPaneMinWidth)
                .coerceAtLeast(TaskListPaneMinWidth)
            val listPaneWidth = storedPaneWidth.coerceIn(TaskListPaneMinWidth, maxListPaneWidth)

            val paneExpansionState = rememberPaneExpansionState()
            LaunchedEffect(paneExpansionState, listPaneWidth, density) {
                paneExpansionState.setFirstPaneWidth(with(density) { listPaneWidth.roundToPx() })
            }
            val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
            val paneDirective = remember(windowAdaptiveInfo, twoPaneLayout) {
                val directive = calculatePaneScaffoldDirective(windowAdaptiveInfo)
                    .copy(horizontalPartitionSpacerSize = PanelGutterWidth)
                if (twoPaneLayout) directive else directive.copy(maxHorizontalPartitions = 1)
            }
            val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(
                directive = paneDirective,
                paneExpansionState = paneExpansionState,
                paneExpansionDragHandle = { state ->
                    TaskListPaneDragHandle(
                        paneExpansionState = state,
                        width = listPaneWidth,
                        maxWidth = maxListPaneWidth,
                        storedWidth = storedPaneWidth,
                        onWidthChanged = { appViewModel.setTaskListPaneWidth(it) },
                    )
                },
            )

            val taskListState = taskListViewModel?.state?.collectAsState()?.value
            val drawerState = drawerViewModel?.let { it.state.collectAsState().value }
                ?: EmptyDrawerState
            val materialDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val chromeScope = rememberCoroutineScope()
            val chromeCaldavDao = koinInject<CaldavDao>()
            val tasksAccountDataRepository = koinInject<TasksAccountDataRepository>()
            val isDarkChrome = isSystemInDarkTheme()
            var newListAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
            var createTaskAfterList by rememberSaveable { mutableStateOf(false) }
            var showNewTag by rememberSaveable { mutableStateOf(false) }
            val writableListFlow = remember(chromeCaldavDao) { chromeCaldavDao.watchHasWritableList() }
            val hasWritableList by writableListFlow.collectAsState(initial = true)

            // Seeded here rather than in the task list nav entry: that entry is disposed whenever a
            // task is opened in single-pane, and re-running this on back would undo the user's pick.
            LaunchedEffect(taskListViewModel) {
                if (taskListViewModel != null &&
                    taskListViewModel.state.value.filter is EmptyFilter
                ) {
                    taskListViewModel.setFilter(MyTasksFilter.create())
                }
            }

            // The one place the drawer's selection is set. Everywhere that changes the filter -
            // the drawer itself, the list and tag dialogs, the seed above, a restored back stack -
            // goes through the task list view model, so mirroring it here rather than at each of
            // those call sites leaves nothing to keep in sync by hand.
            LaunchedEffect(taskListState?.filter) {
                taskListState?.filter?.let { drawerViewModel?.setSelectedFilter(it) }
            }

            fun closeDetail() {
                if (backStack.lastOrNull() is TaskEditDestination) {
                    backStack.removeLastOrNull()
                }
            }

            fun openTask(destination: TaskEditDestination): Boolean {
                // The detail entry has to sit directly on the list entry: ListDetailSceneStrategy
                // walks back from the top and stops at the first entry belonging to another scene,
                // so anything wedged in between collapses the scene to the editor alone and leaves
                // back pointing at that screen instead of the list.
                return Snapshot.withMutableSnapshot {
                    val listIndex = backStack.indexOfLast { it is TaskListDestination }
                    // No task list on the stack at all means this request has outlived the screen it
                    // came from: the last account went away and onboarding replaced everything.
                    // Rebuilding the stack here destroyed that onboarding - or an in-flight sign-in
                    // - and dropped an accountless user onto a task list and editor that have no
                    // view models to render with.
                    if (listIndex < 0) {
                        return@withMutableSnapshot false
                    }
                    // Only ever displaces another editor. Callers can suspend on the way here - the
                    // FAB waits for a list to be created first - and the user can have opened
                    // settings in the meantime; popping that would clear its view models and saved
                    // state and drop them into an editor they never asked for. A request that stale
                    // is dropped instead.
                    if (backStack.drop(listIndex + 1).any { it !is TaskEditDestination }) {
                        return@withMutableSnapshot false
                    }
                    while (backStack.size > listIndex + 1) {
                        backStack.removeLastOrNull()
                    }
                    backStack.add(destination)
                    true
                }
            }

            //
            val taskRequests = koinInject<TaskRequests>()
            LaunchedEffect(taskRequests) {
                taskRequests.openRequests.collect { request ->
                    //
                    request.complete(
                        guarded(
                            tag = "App",
                            what = "Failed to open ${request.destination}",
                            fallback = false,
                        ) {
                            openTask(request.destination)
                        }
                    )
                }
            }

            fun applyOpen(action: OpenTask, destination: TaskEditDestination) {
                when (action) {
                    is OpenTask.Ignore -> Unit
                    is OpenTask.Replace -> openTask(destination)
                    is OpenTask.Stack -> Snapshot.withMutableSnapshot {
                        backStack.add(destination)
                    }
                    is OpenTask.Resume -> Snapshot.withMutableSnapshot {
                        while (backStack.size > action.index + 1) {
                            backStack.removeLastOrNull()
                        }
                    }
                }
            }

            fun openSubtask(destination: TaskEditDestination) {
                applyOpen(openSubtask(backStack, destination), destination)
            }

            fun openTaskFromList(destination: TaskEditDestination) {
                applyOpen(
                    openTaskFromList(
                        backStack = backStack,
                        destination = destination,
                        heldByEditor = subtaskTrees.holds(
                            destination.taskId,
                            destination.remoteId,
                        ),
                        doomedByEditor = subtaskTrees.isDoomed(
                            destination.taskId,
                            destination.remoteId,
                        ),
                    ),
                    destination,
                )
            }

            // The filter is passed in rather than read from taskListState: a caller that just
            // called setFilter still sees the previous value here, because the collector that
            // publishes it hasn't been dispatched yet.
            // Read through these rather than closing over the values directly. NavDisplay builds
            // its entries inside a remember keyed on the back stack, so a callback an entry
            // captured keeps whatever these were at the last navigation - and window geometry, the
            // selected filter and "is there a writable list" all change without one.
            val currentTaskListState by rememberUpdatedState(taskListState)
            val currentHasWritableList by rememberUpdatedState(hasWritableList)
            val currentCanExpandSidebar by rememberUpdatedState(canExpandSidebar)
            val currentSidebarShownExpanded by rememberUpdatedState(sidebarShownExpanded)

            fun navigateToNewTask(filter: Filter? = currentTaskListState?.filter) {
                reporting.addTask("fab")
                openTask(
                    TaskEditDestination(
                        taskId = 0L,
                        remoteId = UUIDHelper.newUUID(),
                        listId = (filter as? CaldavFilter)?.calendar?.id,
                        tagUuid = (filter as? TagFilter)?.uuid,
                    )
                )
            }

            val onCreateTask: () -> Unit = {
                chromeScope.launch {
                    if (currentHasWritableList) {
                        navigateToNewTask()
                    } else {
                        val account = chromeCaldavDao.getAccountForNewList(tasksAccountDataRepository)
                        if (account != null) {
                            createTaskAfterList = true
                            newListAccountId = account.id
                        } else {
                            chromeCaldavDao.getLocalList()
                            navigateToNewTask()
                        }
                    }
                }
            }

            val onMenuClick: () -> Unit = {
                if (currentCanExpandSidebar) {
                    // Toggles what is actually on screen, not the stored preference. Where the two
                    // disagree the preference is one this window can't honour, and writing the
                    // flipped value back inverts the button the next time it can.
                    appViewModel.setSidebarExpanded(!currentSidebarShownExpanded)
                } else {
                    // Either narrow, or wide with the sidebar pinned to the rail. A rail has
                    // nowhere to show list names, so the menu button opens the modal drawer.
                    chromeScope.launch {
                        if (materialDrawerState.isOpen) materialDrawerState.close()
                        else materialDrawerState.open()
                    }
                }
            }

            val onDrawerItemClick: (DrawerItem) -> Unit = { item ->
                when (item) {
                    is DrawerItem.Filter -> {
                        taskListViewModel?.setFilter(item.filter)
                        if (materialDrawerState.isOpen) {
                            chromeScope.launch { materialDrawerState.close() }
                        }
                        closeDetail()
                    }
                    is DrawerItem.Header -> drawerViewModel?.toggleCollapsed(item.header)
                    is DrawerItem.SignIn -> {}
                }
            }

            val onAddClick: (DrawerItem.Header) -> Unit = { header ->
                when (header.header.addIntentRc) {
                    REQUEST_NEW_TAGS -> showNewTag = true
                    else -> header.header.id.toLongOrNull()?.let { newListAccountId = it }
                }
            }

            // Lives here, not inside TaskListChrome: the branches below are disposed whenever the
            // chrome is hidden or the window crosses the narrow/wide boundary, and this is no
            // longer inside a nav entry whose saveable state the decorator would preserve.
            val sidebarListState = rememberLazyListState()
            // The modal drawer needs its own. With the sidebar pinned to a rail both are composed
            // at once, and two lists can't share one LazyListState.
            val modalDrawerListState = rememberLazyListState()

            TaskListChrome(
                drawerViewModel = drawerViewModel,
                drawerState = drawerState,
                // The effective state, not the stored preference: when the window can't hold an
                // expanded sidebar alongside the panes it is rendered as a rail, and the drawer,
                // its resize handle and the collapse-on-back handler all have to agree with that.
                sidebarExpanded = sidebarShownExpanded,
                onSetSidebarExpanded = { appViewModel.setSidebarExpanded(it) },
                sidebarWidth = currentSidebarWidth,
                sidebarListState = sidebarListState,
                modalDrawerListState = modalDrawerListState,
                onSidebarWidthDelta = { delta ->
                    // Accumulate onto the live drag value: drag deltas are drained in batches with
                    // no recomposition between them, so anything read from the last composition
                    // would be stale by the second delta.
                    val base = draggedSidebarWidth ?: sidebarWidth
                    draggedSidebarWidth = (base + delta)
                        .coerceIn(SidebarMinWidth, maxSidebarWidth)
                },
                onSidebarResizeFinished = {
                    draggedSidebarWidth?.let {
                        appViewModel.setSidebarWidth(
                            resolvePersistedWidth(
                                dragged = it,
                                stored = storedSidebarWidth,
                                maxWidth = maxSidebarWidth,
                            )
                        )
                    }
                    draggedSidebarWidth = null
                },
                materialDrawerState = materialDrawerState,
                hasDetailOpen = hasDetailOpen,
                twoPaneLayout = twoPaneLayout,
                wideLayout = wideLayout,
                showSidebar = showSidebar,
                canExpandSidebar = canExpandSidebar,
                visible = showChrome,
                onDrawerItemClick = onDrawerItemClick,
                onAddClick = onAddClick,
                onAddAccount = { backStack.push(AddAccountDestination) },
            ) {
            NavDisplay(
                backStack = backStack,
                sceneStrategies = listOf(listDetailStrategy),
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    entry<WelcomeDestination> {
                        LaunchedEffect(Unit) {
                            reporting.logEvent(AnalyticsEvents.SCREEN_WELCOME)
                        }
                        WelcomeScreenLayout(
                            showLegalDisclosure = !configuration.isLibre,
                            showImportBackup = configuration.supportsBackupImport,
                            onSignIn = {
                                backStack.push(AddAccountDestination)
                            },
                            onContinueWithoutSync = {
                                appViewModel.continueWithoutSync()
                            },
                            openLegalUrl = openUrl,
                            environments = environments,
                            currentEnvironment = currentEnvironment,
                            onSelectEnvironment = onSelectEnvironment,
                        )
                    }
                    entry<AddAccountDestination> {
                        LaunchedEffect(Unit) {
                            reporting.logEvent(AnalyticsEvents.SCREEN_ADD_ACCOUNT)
                        }
                        val addAccountViewModel = koinViewModel<AddAccountViewModel>()
                        LaunchedEffect(Unit) {
                            addAccountViewModel.accountAdded.collect {
                                if (backStack.lastOrNull() is AddAccountDestination) {
                                    backStack.removeLastOrNull()
                                }
                            }
                        }
                        val signInState by addAccountViewModel.signInState.collectAsState()
                        AddAccountScreen(
                            configuration = configuration,
                            hasTasksAccount = addAccountViewModel.hasTasksAccount,
                            hasPro = addAccountViewModel.hasPro,
                            needsConsent = false,
                            onBack = { backStack.removeLastOrNull() },
                            signIn = { platform ->
                                reporting.logEvent(
                                    AnalyticsEvents.ADD_ACCOUNT,
                                    AnalyticsEvents.PARAM_SOURCE to "onboarding",
                                    AnalyticsEvents.PARAM_SELECTION to platform.name,
                                )
                                // On desktop, gate CalDAV/EteSync/Google Tasks behind pro
                                if (configuration.billingProvider == org.tasks.billing.BillingProvider.PADDLE
                                    && !addAccountViewModel.hasPro
                                ) {
                                    when (platform) {
                                        Platform.CALDAV, Platform.ETEBASE, Platform.GOOGLE_TASKS, Platform.MICROSOFT -> {
                                            backStack.push(PricingDestination(mode = PricingMode.NYP_ONLY, source = platform.name))
                                            return@AddAccountScreen
                                        }
                                        else -> {}
                                    }
                                }
                                when (platform) {
                                    Platform.TASKS_ORG -> {
                                        backStack.push(PricingDestination(mode = PricingMode.CLOUD_ONLY, source = platform.name))
                                    }
                                    Platform.CALDAV -> backStack.push(CaldavSignInDestination)
                                    Platform.ETEBASE -> backStack.push(EtebaseSignInDestination)
                                    else -> addAccountViewModel.signIn(platform)
                                }
                            },
                            openUrl = { platform ->
                                // TODO: handle open URL for platform
                            },
                            openLegalUrl = openUrl,
                        )
                        SignInErrorDialog(
                            signInState = signInState,
                            onDismiss = { addAccountViewModel.dismissError() },
                            reporting = reporting,
                            onPaymentRequired = {
                                backStack.push(PricingDestination(mode = PricingMode.CLOUD_ONLY, source = "sign_in_402"))
                            },
                        )
                    }
                    entry<CaldavSignInDestination> {
                        org.tasks.compose.settings.CaldavSignInScreen(
                            onNavigateBack = { backStack.removeLastOrNull() },
                            onAccountCreated = {
                                if (backStack.lastOrNull() is CaldavSignInDestination) {
                                    backStack.removeLastOrNull()
                                }
                            },
                        )
                    }
                    entry<EtebaseSignInDestination> {
                        org.tasks.compose.settings.EtebaseSignInScreen(
                            onNavigateBack = { backStack.removeLastOrNull() },
                            onAccountCreated = {
                                if (backStack.lastOrNull() is EtebaseSignInDestination) {
                                    backStack.removeLastOrNull()
                                }
                            },
                        )
                    }
                    entry<TaskListDestination>(
                        metadata = ListDetailSceneStrategy.listPane(
                            detailPlaceholder = { NoTaskSelected() },
                        ) + ListDetailSceneStrategy.preferredPaneSize(
                            width = listPaneWidth,
                        ),
                    ) {
                        // Null only in the frame between the last account going away and the
                        // effect above replacing this entry with onboarding. Rendering a
                        // placeholder rather than nothing at all: an empty pane with no chrome
                        // leaves the user nothing to press if that frame ever turns out to last.
                        val viewModel = taskListViewModel
                        if (viewModel != null && drawerViewModel != null) {
                            TaskListScreen(
                                viewModel = viewModel,
                                drawerViewModel = drawerViewModel,
                                onSettingsClick = { backStack.push(SettingsDestination) },
                                onSubscribe = { backStack.push(PricingDestination()) },
                                onAddAccount = { backStack.push(AddAccountDestination) },
                                onTaskClick = { destination -> openTaskFromList(destination) },
                                onCreateTask = onCreateTask,
                                onMenuClick = onMenuClick,
                            )
                        } else {
                            LoadingPane()
                        }
                    }
                    entry<TaskEditDestination>(
                        metadata = ListDetailSceneStrategy.detailPane(),
                    ) { destination ->
                        val filterPickerViewModel = listPickerViewModel
                        if (filterPickerViewModel != null) {
                            TaskEditEntry(
                                destination = destination,
                                filterPickerViewModel = filterPickerViewModel,
                                onOpenSubtask = { taskId, remoteId, isDraft ->
                                    openSubtask(
                                        TaskEditDestination(
                                            taskId = taskId,
                                            remoteId = remoteId,
                                            isSubtaskDraft = isDraft,
                                        )
                                    )
                                },
                                // The drawer's own handler is registered after this whole subtree
                                // and so takes precedence - see TaskListChrome. This stands the
                                // editor down as well, so a back press while the drawer is open
                                // cannot reach it even if that ordering ever changes.
                                backHandlerEnabled = !materialDrawerState.isOpen,
                                onAddAccount = { backStack.push(AddAccountDestination) },
                                onSubscribe = { backStack.push(PricingDestination()) },
                                onListsChanged = { drawerViewModel?.updateFilters() },
                                onClose = { closeDetail() },
                            )
                        } else {
                            LoadingPane()
                        }
                    }
                    entry<SettingsDestination> {
                        val purchaseState = koinInject<org.tasks.billing.PurchaseState>()
                        // Saveable for the same reason as the pricing screen's dialog: a rotation
                        // recreates the activity, and this sheet was vanishing mid-flow.
                        var showManageSheet by rememberSaveable { mutableStateOf(false) }
                        SettingsScreen(
                            onBack = { backStack.removeLastOrNull() },
                            onAddAccountClick = { backStack.push(AddAccountDestination) },
                            onLinkDesktopClick = {
                                if (purchaseState.hasPro) {
                                    backStack.push(LinkDesktopDestination)
                                } else {
                                    co.touchlab.kermit.Logger.withTag("App")
                                        .e { "Link desktop clicked without pro subscription" }
                                }
                            },
                            onUpgradeClick = { backStack.push(PricingDestination()) },
                            onMigrateToCloud = {
                                backStack.push(
                                    PricingDestination(
                                        mode = PricingMode.CLOUD_ONLY,
                                        source = "local_account_migrate",
                                    )
                                )
                            },
                            onSignInClick = {
                                backStack.push(PricingDestination(mode = PricingMode.CLOUD_ONLY, source = "sign_in"))
                            },
                            onSubscribedClick = { showManageSheet = true },
                        )
                        if (showManageSheet) {
                            val isGitHubSponsor = subscriptionInfo?.isGitHubSponsor == true
                            val sponsorUrl = stringResource(Res.string.url_sponsor)
                            val googlePlayUrl = TasksUrls.GOOGLE_PLAY_SUBSCRIPTIONS
                            ModalBottomSheet(
                                onDismissRequest = { showManageSheet = false },
                                containerColor = MaterialTheme.colorScheme.surface,
                            ) {
                                ManageSubscriptionSheetContent(
                                    onUpgrade = {
                                        showManageSheet = false
                                        backStack.push(PricingDestination(mode = PricingMode.CLOUD_ONLY, source = "subscribed"))
                                    },
                                    onModify = {
                                        showManageSheet = false
                                        openUrl(sponsorUrl)
                                    },
                                    onCancel = {
                                        showManageSheet = false
                                        openUrl(if (isGitHubSponsor) sponsorUrl else googlePlayUrl)
                                    },
                                    showModify = isGitHubSponsor,
                                )
                            }
                        }
                    }
                    entry<LinkDesktopDestination> {
                        val qrScanner = koinInject<org.tasks.billing.QrScanner>()
                        val desktopLinkService = koinInject<org.tasks.billing.DesktopLinkService>()
                        LinkDesktopScreen(
                            onBack = { backStack.removeLastOrNull() },
                            onScan = { qrScanner.scan() },
                            onConfirm = { code -> desktopLinkService.confirmLink(code) },
                        )
                    }
                    entry<DesktopProDestination> { destination ->
                        val desktopLinkClient = koinInject<org.tasks.billing.DesktopLinkClient>()
                        val gitHubSponsorClient = koinInject<org.tasks.billing.GitHubSponsorClient>()
                        LaunchedEffect(Unit) {
                            reporting.logEvent(AnalyticsEvents.SCREEN_RESTORE_PURCHASES)
                        }
                        val successButtonText = when (destination.source) {
                            Platform.CALDAV.name -> stringResource(Res.string.add_platform_account, stringResource(Res.string.caldav))
                            Platform.ETEBASE.name -> stringResource(Res.string.add_platform_account, stringResource(Res.string.etesync))
                            Platform.GOOGLE_TASKS.name -> stringResource(Res.string.add_platform_account, stringResource(Res.string.gtasks_GPr_header))
                            else -> null
                        }
                        DesktopProScreen(
                            onBack = { backStack.removeLastOrNull() },
                            onCreateLink = { desktopLinkClient.createLink() },
                            onPollStatus = { code -> desktopLinkClient.pollStatus(code) },
                            onLinkSuccess = { jwt, refreshToken, sku, formattedPrice ->
                                desktopLinkClient
                                    .onLinkSuccess(jwt, refreshToken, sku, formattedPrice)
                                    .also { stored ->
                                        if (stored) {
                                            reporting.logEvent(
                                                AnalyticsEvents.RESTORE_SUCCESS,
                                                AnalyticsEvents.PARAM_SELECTION to AnalyticsEvents.SELECTION_GOOGLE_PLAY,
                                            )
                                        }
                                    }
                            },
                            onGitHubSignIn = { gitHubSponsorClient.signIn() },
                            onOpenSponsorPage = {
                                reporting.logEvent(AnalyticsEvents.RESTORE_SPONSOR_CLICK)
                                openUrl("https://github.com/sponsors/abaker")
                            },
                            onGooglePlaySelected = {
                                reporting.logEvent(
                                    AnalyticsEvents.RESTORE_SELECTION,
                                    AnalyticsEvents.PARAM_SELECTION to AnalyticsEvents.SELECTION_GOOGLE_PLAY,
                                )
                            },
                            onGitHubSelected = {
                                reporting.logEvent(
                                    AnalyticsEvents.RESTORE_SELECTION,
                                    AnalyticsEvents.PARAM_SELECTION to AnalyticsEvents.SELECTION_GITHUB,
                                )
                            },
                            onNotSponsor = {
                                reporting.logEvent(AnalyticsEvents.RESTORE_NOT_SPONSOR)
                            },
                            onLinkError = {
                                reporting.logEvent(
                                    AnalyticsEvents.RESTORE_ERROR,
                                    AnalyticsEvents.PARAM_SELECTION to AnalyticsEvents.SELECTION_GOOGLE_PLAY,
                                )
                            },
                            onGitHubSuccess = {
                                reporting.logEvent(
                                    AnalyticsEvents.RESTORE_SUCCESS,
                                    AnalyticsEvents.PARAM_SELECTION to AnalyticsEvents.SELECTION_GITHUB,
                                )
                            },
                            onGitHubFailed = {
                                reporting.logEvent(
                                    AnalyticsEvents.RESTORE_ERROR,
                                    AnalyticsEvents.PARAM_SELECTION to AnalyticsEvents.SELECTION_GITHUB,
                                )
                            },
                            successButtonText = successButtonText,
                        )
                    }
                    entry<PricingDestination> { destination ->
                        // Saveable: this is the payment flow, and the provider dialog was silently
                        // disappearing on rotation because the activity is recreated while the back
                        // stack and the entry's own saved state survive.
                        var showSignInDialog by rememberSaveable { mutableStateOf(false) }
                        val addAccountViewModel = koinViewModel<AddAccountViewModel>()
                        val signInState by addAccountViewModel.signInState.collectAsState()
                        // Signing in and the account landing are published by two independent
                        // queries on the same invalidation, so the onboarding redirect above can
                        // replace the whole stack before these collectors run. This entry is still
                        // composed for that frame, and popping blind from here empties the stack -
                        // which NavDisplay treats as fatal.
                        fun popPricing(): Boolean =
                            if (backStack.lastOrNull() is PricingDestination) {
                                backStack.removeLastOrNull()
                                true
                            } else {
                                false
                            }
                        LaunchedEffect(Unit) {
                            reporting.logEvent(
                                AnalyticsEvents.SCREEN_PRICING,
                                AnalyticsEvents.PARAM_SOURCE to destination.source,
                                AnalyticsEvents.PARAM_TYPE to destination.mode.name,
                            )
                            addAccountViewModel.accountAdded.collect {
                                popPricing()
                            }
                        }
                        if (destination.mode == PricingMode.NYP_ONLY) {
                            LaunchedEffect(subscriptionInfo) {
                                if (subscriptionInfo != null) {
                                    when (destination.source) {
                                        Platform.CALDAV.name -> {
                                            if (popPricing()) {
                                                backStack.push(CaldavSignInDestination)
                                            }
                                        }
                                        Platform.ETEBASE.name -> {
                                            if (popPricing()) {
                                                backStack.push(EtebaseSignInDestination)
                                            }
                                        }
                                        Platform.GOOGLE_TASKS.name ->
                                            addAccountViewModel.signIn(Platform.GOOGLE_TASKS)
                                        Platform.MICROSOFT.name ->
                                            addAccountViewModel.signIn(Platform.MICROSOFT)
                                        else -> popPricing()
                                    }
                                }
                            }
                        } else {
                            LaunchedEffect(Unit) {
                                subscriptionProvider.subscription
                                    .distinctUntilChanged()
                                    .drop(1)
                                    .collect {
                                        if (it != null) {
                                            popPricing()
                                        }
                                    }
                            }
                        }
                        val googlePlayUrl = stringResource(Res.string.url_google_play)
                        val sponsorUrl = stringResource(Res.string.url_sponsor)
                        PricingScreen(
                            mode = destination.mode,
                            onBack = { backStack.removeLastOrNull() },
                            onSignIn = {
                                reporting.logEvent(AnalyticsEvents.PRICING_SIGN_IN_CLICK)
                                showSignInDialog = true
                            },
                            onRestorePurchases = { backStack.push(DesktopProDestination(source = destination.source)) },
                            onCloudSubscribeClick = {
                                reporting.logEvent(
                                    AnalyticsEvents.PRICING_SUBSCRIBE_CLICK,
                                    AnalyticsEvents.PARAM_TIER to AnalyticsEvents.TIER_CLOUD,
                                )
                                openUrl(googlePlayUrl)
                            },
                            onCloudSponsorClick = {
                                reporting.logEvent(
                                    AnalyticsEvents.PRICING_SPONSOR_CLICK,
                                    AnalyticsEvents.PARAM_TIER to AnalyticsEvents.TIER_CLOUD,
                                )
                                openUrl(sponsorUrl)
                            },
                            onNypSubscribeClick = {
                                reporting.logEvent(
                                    AnalyticsEvents.PRICING_SUBSCRIBE_CLICK,
                                    AnalyticsEvents.PARAM_TIER to AnalyticsEvents.TIER_NYP,
                                )
                                openUrl(googlePlayUrl)
                            },
                            onNypSponsorClick = {
                                reporting.logEvent(
                                    AnalyticsEvents.PRICING_SPONSOR_CLICK,
                                    AnalyticsEvents.PARAM_TIER to AnalyticsEvents.TIER_NYP,
                                )
                                openUrl(sponsorUrl)
                            },
                            onBillingToggle = { isAnnual ->
                                reporting.logEvent(
                                    AnalyticsEvents.PRICING_BILLING_TOGGLE,
                                    AnalyticsEvents.PARAM_PERIOD to if (isAnnual) AnalyticsEvents.PERIOD_ANNUAL else AnalyticsEvents.PERIOD_MONTHLY,
                                )
                            },
                            showSupporterBanner = destination.mode != PricingMode.NYP_ONLY && subscriptionInfo?.isTasksSubscription == false,
                        )
                        if (showSignInDialog) {
                            BasicAlertDialog(onDismissRequest = { showSignInDialog = false }) {
                                SignInProviderDialog(
                                    onSelected = { provider ->
                                        showSignInDialog = false
                                        val oauthProvider = when (provider) {
                                            SignInProvider.GOOGLE -> OAuthProvider.GOOGLE
                                            SignInProvider.GITHUB -> OAuthProvider.GITHUB
                                        }
                                        reporting.logEvent(
                                            AnalyticsEvents.SIGN_IN_PROVIDER_SELECTED,
                                            AnalyticsEvents.PARAM_PROVIDER to oauthProvider.name,
                                        )
                                        addAccountViewModel.signIn(
                                            platform = Platform.TASKS_ORG,
                                            provider = oauthProvider,
                                            openUrl = openUrl,
                                        )
                                    },
                                    onHelp = {
                                        showSignInDialog = false
                                        openUrl("https://tasks.org/docs/sync")
                                    },
                                    onCancel = { showSignInDialog = false },
                                )
                            }
                        }
                        SignInErrorDialog(
                            signInState = signInState,
                            onDismiss = { addAccountViewModel.dismissError() },
                            reporting = reporting,
                            onPaymentRequired = {},
                        )
                    }
                },
            )
            }

            // These two are siblings of NavDisplay rather than children of the task list entry, so
            // they survive navigation and would otherwise stay on top of whatever they navigated
            // to. NewListDialogHost dismisses itself before calling out; this one is inline.
            NewListDialogHost(
                accountId = newListAccountId,
                isDark = isDarkChrome,
                onDismiss = { created ->
                    val shouldCreateTask = createTaskAfterList
                    newListAccountId = null
                    createTaskAfterList = false
                    drawerViewModel?.updateFilters()
                    created?.let { newFilter ->
                        taskListViewModel?.setFilter(newFilter)
                        if (shouldCreateTask) {
                            navigateToNewTask(newFilter)
                        }
                    }
                },
                onSubscribe = { backStack.push(PricingDestination()) },
                onAddAccount = { backStack.push(AddAccountDestination) },
            )

            if (showNewTag) {
                // Saveable, because TagSettingsDialog keys its view model on this uuid. A plain
                // remember handed the dialog a fresh key on every rotation, so the typed name and
                // colour were replaced by a brand-new view model's empty state.
                val newTagUuid = rememberSaveable { UUIDHelper.newUUID() }
                val newTag = remember(newTagUuid) { TagData(remoteId = newTagUuid) }
                TagSettingsDialog(
                    tagData = newTag,
                    isDark = isDarkChrome,
                    onDismiss = { created ->
                        showNewTag = false
                        drawerViewModel?.updateFilters()
                        created?.let { tag ->
                            taskListViewModel?.setFilter(TagFilter(tag))
                        }
                    },
                    onSubscribe = {
                        showNewTag = false
                        backStack.push(PricingDestination())
                    },
                )
            }

            // A save started by a destroyed editor has no screen of its own left to report on, so
            // every save failure is reported here. The count is sticky rather than an event: on
            // Android the whole composition can be gone at the moment a teardown save fails, and
            // this collector with it, so a failure has to wait until something is back on screen.
            // showSnackbar suspends until the toast is gone, which keeps a second failure queued
            // behind the first rather than replacing it.
            val saveErrorMessage = stringResource(Res.string.failed_to_save_task)
            val saveErrorSnackbar = remember { SnackbarHostState() }
            // Keyed on Unit and driven off the flow, never off a Boolean derived from it. A
            // LaunchedEffect whose coroutine returns is not restarted until its key changes, and
            // the count dropping to 0 and back to 1 before the composition observes the 0 leaves
            // "count > 0" true throughout - so keying on that let the effect finish for good and
            // took save reporting down with it for the rest of the process.
            LaunchedEffect(Unit) {
                while (true) {
                    pendingSaves.saveFailures.first { it > 0 }
                    saveErrorSnackbar.showSnackbar(saveErrorMessage)
                    pendingSaves.acknowledgeSaveFailure()
                }
            }
            // Bottom-aligned, like every other snackbar in the app. Toaster centres its own host in
            // whatever it is given, which as a sibling of NavDisplay is the whole window - so a
            // failed save put a snackbar across the middle of the task list or the editor and, being
            // a Surface, ate every click behind it until it went away.
            Box(modifier = Modifier.fillMaxSize().platformNavigationBarsPadding()) {
                SnackbarHost(
                    hostState = saveErrorSnackbar,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                )
            }
        }
    }
    } // CompositionLocalProvider
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignInErrorDialog(
    signInState: AddAccountViewModel.SignInState?,
    onDismiss: () -> Unit,
    reporting: Reporting,
    onPaymentRequired: () -> Unit,
) {
    val errorState = signInState as? AddAccountViewModel.SignInState.Error ?: return
    LaunchedEffect(errorState) {
        reporting.logEvent(
            AnalyticsEvents.SIGN_IN_ERROR,
            AnalyticsEvents.PARAM_MESSAGE to errorState.message,
        )
    }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(Res.string.wrong_account),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (errorState.isPaymentRequired) {
                        stringResource(
                            Res.string.subscription_not_found,
                            TasksUrls.SUPPORT_EMAIL,
                        )
                    } else {
                        errorState.message
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp),
                )
                TextButton(
                    onClick = {
                        onDismiss()
                        if (errorState.isPaymentRequired) {
                            onPaymentRequired()
                        }
                    },
                    modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
                ) {
                    Text(stringResource(Res.string.ok))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TaskListChrome(
    /** Null during onboarding, where there is no drawer to show. */
    drawerViewModel: DrawerViewModel?,
    drawerState: org.tasks.viewmodel.DrawerViewModel.State,
    sidebarExpanded: Boolean,
    onSetSidebarExpanded: (Boolean) -> Unit,
    /** Already interpolated between the rail and the expanded width by the caller. */
    sidebarWidth: Dp,
    sidebarListState: LazyListState,
    modalDrawerListState: LazyListState,
    onSidebarWidthDelta: (Dp) -> Unit,
    onSidebarResizeFinished: () -> Unit,
    materialDrawerState: DrawerState,
    hasDetailOpen: Boolean,
    twoPaneLayout: Boolean,
    wideLayout: Boolean,
    showSidebar: Boolean,
    /** False where the window is too narrow to hold an expanded sidebar beside the panes. */
    canExpandSidebar: Boolean,
    visible: Boolean,
    onDrawerItemClick: (DrawerItem) -> Unit,
    onAddClick: (DrawerItem.Header) -> Unit,
    onAddAccount: () -> Unit,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()

    // content() holds the whole NavDisplay, and the branches below move it between three call
    // sites. Without movableContentOf, every branch flip - navigating to settings, or dragging a
    // window across the narrow/wide boundary - would dispose and rebuild the navigation subtree,
    // clearing every nav entry's ViewModelStore and saved state along with it.
    val currentContent by rememberUpdatedState(content)
    val movableContent = remember { movableContentOf { currentContent() } }

    // Used wherever the permanent sidebar can't carry list names: narrow windows, and wide windows
    // where the panes leave room for the rail but never for an expanded sidebar.
    val modalDrawerSheet: @Composable () -> Unit = {
        ModalDrawerSheet(
            windowInsets = WindowInsets(0),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = platformSidebarInsets()
                            .calculateStartPadding(LocalLayoutDirection.current),
                    ),
            ) {
                TaskListDrawer(
                    drawerOpen = materialDrawerState.isOpen,
                    drawerState = drawerState,
                    onQueryChange = { drawerViewModel?.setMenuQuery(it) },
                    onClick = onDrawerItemClick,
                    onAddClick = onAddClick,
                    onErrorClick = { /* TODO: show sync error */ },
                    onSignIn = onAddAccount,
                    // Hoisted for the same reason as the sidebar's: this sheet is disposed every
                    // time the drawer closes, and it is not inside a nav entry whose saveable
                    // state the decorator would preserve.
                    listState = modalDrawerListState,
                    searchButtonInset = SearchButtonInset,
                )
                val drawerScrimColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.8f)
                StatusBarScrim(
                    color = drawerScrimColor,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
                NavigationBarScrim(
                    color = drawerScrimColor,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }

    // A drawer left open by a layout that no longer shows one would come back the next time one was
    // shown. That is not only the window crossing the sidebar boundary: the sheet's own sign-in row
    // pushes a screen without closing the drawer, which hides the chrome entirely, and backing out
    // of that used to land on the task list under a fully open drawer and scrim nobody asked for.
    val showsModalDrawer = visible && !(wideLayout && canExpandSidebar)
    LaunchedEffect(showsModalDrawer) {
        if (!showsModalDrawer && materialDrawerState.isOpen) {
            materialDrawerState.close()
        }
    }

    val singlePaneDetail = hasDetailOpen && !twoPaneLayout

    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    // Start edge, matching the padding it is applied as and the width arithmetic in App().
    val cutoutPadding = platformSidebarInsets().calculateStartPadding(layoutDirection)
    val panes: @Composable () -> Unit = {
        Row(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = showSidebar,
                enter = expandHorizontally(),
                exit = shrinkHorizontally(),
            ) {
                Box(
                    modifier = Modifier
                        .width(sidebarWidth + cutoutPadding)
                        .padding(start = cutoutPadding),
                ) {
                    TaskListDrawer(
                        drawerOpen = true,
                        drawerState = drawerState,
                        onQueryChange = { drawerViewModel?.setMenuQuery(it) },
                        onClick = onDrawerItemClick,
                        onAddClick = onAddClick,
                        onErrorClick = { /* TODO: show sync error */ },
                        onSignIn = onAddAccount,
                        expanded = sidebarExpanded,
                        onExpandDrawer = {
                            // Expanding is impossible in this window, so the rail's own
                            // affordances fall back to the modal drawer too.
                            if (canExpandSidebar) {
                                onSetSidebarExpanded(true)
                            } else {
                                scope.launch { materialDrawerState.open() }
                            }
                        },
                        listState = sidebarListState,
                        searchButtonInset = SearchButtonInset,
                    )
                    val sidebarScrimColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                    StatusBarScrim(
                        color = sidebarScrimColor,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                    NavigationBarScrim(
                        color = sidebarScrimColor,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
            if (showSidebar && sidebarExpanded) {
                // A window resize can tear the handle out of the composition mid-gesture, and a
                // detached draggable never reports onDragStopped - so commit on the way out too,
                // otherwise the drag is silently discarded.
                val currentResizeFinished by rememberUpdatedState(onSidebarResizeFinished)
                DisposableEffect(Unit) {
                    onDispose { currentResizeFinished() }
                }
                HorizontalResizeHandle(
                    onDelta = { delta ->
                        onSidebarWidthDelta(
                            with(density) { (if (isRtl) -delta else delta).toDp() }
                        )
                    },
                    onDragStopped = onSidebarResizeFinished,
                    modifier = Modifier.align(Alignment.CenterVertically),
                    // Drawer items are inset, task rows are full bleed
                    startInset = DrawerItemInset,
                    endInset = 0.dp,
                )
            }
            Box(modifier = Modifier.weight(1f)) { movableContent() }
        }
    }
    // Movable for the same reason as content(): the branches below call this from two places - bare,
    // and inside the modal drawer - and a plain lambda invoked from two call sites is two separate
    // groups, so every flip would forget everything the sidebar remembers. That includes
    // TaskListDrawer's own search field, and the flip is not only a window resize: canExpandSidebar
    // turns over when a task opens, because the panes then ask for more room. In the band where that
    // happens - roughly 840dp to 888dp, which includes the default desktop window - typing a search
    // in the sidebar and then tapping a task silently discarded it.
    val currentPanes by rememberUpdatedState(panes)
    val movablePanes = remember { movableContentOf { currentPanes() } }

    // Back handlers for the chrome, registered after the navigation subtree above rather than
    // before it. NavDisplay registers its own enabled handler on the way in to composing the scene,
    // and back resolves to the most recently registered handler - so anything armed above content()
    // loses to it and pops the back stack instead. That is how back closed the task being edited
    // while the drawer sat fully open on top of it.
    val chromeBackHandlers: @Composable () -> Unit = {
        // Only armed where a sidebar is actually on screen to collapse, and never while a task is
        // open: TaskEditScreen keeps its own back handler armed for as long as it is composed, so
        // back/escape closes the editor before it collapses the sidebar.
        PlatformBackHandler(
            enabled = visible && wideLayout && showSidebar && sidebarExpanded && !hasDetailOpen,
        ) {
            onSetSidebarExpanded(false)
        }
        // Last, so the drawer wins over everything underneath it while it is open.
        PlatformBackHandler(enabled = materialDrawerState.isOpen) {
            scope.launch { materialDrawerState.close() }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            !visible -> movableContent()
            showsModalDrawer -> ModalNavigationDrawer(
                drawerState = materialDrawerState,
                drawerContent = modalDrawerSheet,
                gesturesEnabled = materialDrawerState.isOpen || !singlePaneDetail,
            ) {
                // Narrow windows have no permanent sidebar at all; wide ones keep theirs as a rail
                // beside the drawer.
                if (wideLayout) movablePanes() else movableContent()
                chromeBackHandlers()
            }
            else -> {
                movablePanes()
                chromeBackHandlers()
            }
        }
    }
}

/**
 * Owns the view models that only mean anything while there is an account.
 *
 * They are resolved at the top of [App] rather than inside a nav entry - hoisted so that opening a
 * task doesn't rebuild the drawer and re-run its queries - which also means nothing pops them off a
 * back stack the way the entry decorator used to. Signing out only replaces the stack, so without
 * this they outlived the account: the drawer went on polling filterProvider for an account that had
 * gone, and signing back in reused a task list still pointing at the previous account's list.
 */
@Composable
private fun rememberAccountViewModelStoreOwner(hasAccount: Boolean): ViewModelStoreOwner {
    // Held in a view model rather than in a remember, so that the store survives an Android
    // configuration change the way the nav entry's own store used to. A plain remember is disposed
    // with the composition, so rotating dropped the task list's filter back to My Tasks.
    val owner = viewModel { AccountViewModelStoreOwner() }
    // Cleared from an effect rather than during composition, which is a frame later than the callers
    // stop resolving against it - and they have already resolved to null by then, so nothing is
    // holding an instance this clears.
    LaunchedEffect(hasAccount) {
        if (!hasAccount) {
            owner.viewModelStore.clear()
        }
    }
    return owner
}

private class AccountViewModelStoreOwner : ViewModel(), ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()

    override fun onCleared() {
        viewModelStore.clear()
    }
}

/** Placeholder for a pane whose view models are momentarily gone, so it never renders nothing. */
@Composable
private fun LoadingPane() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TaskListScreen(
    viewModel: TaskListViewModel,
    drawerViewModel: DrawerViewModel,
    onSettingsClick: () -> Unit,
    onSubscribe: () -> Unit,
    onAddAccount: () -> Unit,
    onTaskClick: (TaskEditDestination) -> Unit,
    onCreateTask: () -> Unit,
    onMenuClick: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val chipDataProvider = koinInject<ChipDataProvider>()
    val reporting = koinInject<Reporting>()
    val sortViewModel = koinViewModel<SortSettingsViewModel>()
    val sortState by sortViewModel.state.collectAsState()
    var showSortSheet by remember { mutableStateOf(false) }
    var editListCalendarId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editTagUuid by rememberSaveable { mutableStateOf<String?>(null) }
    val caldavDao = koinInject<CaldavDao>()
    val tagDataDao = koinInject<TagDataDao>()
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    val filterTint = state.filter.tint
    val themeColor = remember(filterTint, isDark) {
        ColorProvider.themeColor(
            seedColor = if (filterTint != 0) filterTint else BLUE,
            isDark = isDark,
        )
    }

    val editableCaldavFilter = state.filter as? CaldavFilter
    val editableTagFilter = state.filter as? TagFilter

    TaskListPane(
        state = state,
        chipDataProvider = chipDataProvider,
        reporting = reporting,
        viewModel = viewModel,
        themeColor = themeColor,
        onShowSortSheet = { showSortSheet = true },
        onTaskClick = onTaskClick,
        showMenuButton = true,
        onMenuClick = onMenuClick,
        onSettingsClick = onSettingsClick,
        showListSettings = editableCaldavFilter != null,
        onListSettingsClick = { editableCaldavFilter?.let { editListCalendarId = it.calendar.id } },
        showTagSettings = editableTagFilter != null,
        onTagSettingsClick = { editableTagFilter?.let { editTagUuid = it.uuid } },
        onCreateTask = onCreateTask,
        modifier = Modifier.fillMaxSize(),
    )

    SortSheetHost(
        showSortSheet = showSortSheet,
        onDismiss = { showSortSheet = false },
        sortState = sortState,
        sortViewModel = sortViewModel,
        completedAndHiddenEnabled = state.filter.supportsHiddenTasks(),
    )

    editListCalendarId?.let { calendarId ->
        val filter by produceState<CaldavFilter?>(null) {
            val calendar = caldavDao.getCalendarById(calendarId)
                ?: run { editListCalendarId = null; return@produceState }
            val account = caldavDao.getAccountByUuid(calendar.account!!)
                ?: run { editListCalendarId = null; return@produceState }
            value = CaldavFilter(calendar = calendar, account = account)
        }
        filter?.let { f ->
            ListSettingsDialog(
                account = f.account,
                calendar = f.calendar,
                isDark = isDark,
                onDismiss = { updated ->
                    editListCalendarId = null
                    drawerViewModel.updateFilters()
                    viewModel.setFilter(
                        CaldavFilter(calendar = updated ?: f.calendar, account = f.account)
                    )
                },
                onDeleted = {
                    editListCalendarId = null
                    drawerViewModel.updateFilters()
                    scope.launch { viewModel.setFilter(MyTasksFilter.create()) }
                },
                onSubscribe = onSubscribe,
                onAddAccount = onAddAccount,
            )
        }
    }

    editTagUuid?.let { uuid ->
        val tagData by produceState<TagData?>(null, uuid) {
            value = tagDataDao.getByUuid(uuid)
                ?: run { editTagUuid = null; return@produceState }
        }
        tagData?.let { tag ->
            TagSettingsDialog(
                tagData = tag,
                isDark = isDark,
                onDismiss = { updated ->
                    editTagUuid = null
                    drawerViewModel.updateFilters()
                    viewModel.setFilter(TagFilter(updated ?: tag))
                },
                onDeleted = {
                    editTagUuid = null
                    drawerViewModel.updateFilters()
                    scope.launch { viewModel.setFilter(MyTasksFilter.create()) }
                },
                onSubscribe = onSubscribe,
            )
        }
    }
}

@Composable
private fun TaskEditEntry(
    destination: TaskEditDestination,
    filterPickerViewModel: FilterPickerViewModel,
    backHandlerEnabled: Boolean,
    onAddAccount: () -> Unit,
    onSubscribe: () -> Unit,
    onListsChanged: () -> Unit,
    onOpenSubtask: (taskId: Long, remoteId: String, isDraft: Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val taskEditViewModel = koinViewModel<TaskEditViewModel> {
        org.koin.core.parameter.parametersOf(destination)
    }
    val isDark = isSystemInDarkTheme()
    var newListAccountId by rememberSaveable { mutableStateOf<Long?>(null) }

    TaskEditScreen(
        viewModel = taskEditViewModel,
        filterPickerViewModel = filterPickerViewModel,
        onCreateList = { accountId -> newListAccountId = accountId },
        onSignIn = onAddAccount,
        backHandlerEnabled = backHandlerEnabled,
        onOpenSubtask = onOpenSubtask,
        onClose = onClose,
    )

    NewListDialogHost(
        accountId = newListAccountId,
        isDark = isDark,
        onDismiss = { created ->
            newListAccountId = null
            onListsChanged()
            created?.let { taskEditViewModel.setList(it) }
        },
        onSubscribe = onSubscribe,
        onAddAccount = onAddAccount,
    )
}

/**
 * Hosts the "create list" dialog for [accountId], resolving the account first. [onDismiss] receives
 * the filter for the created list, or null if the dialog was cancelled or the account is gone.
 */
@Composable
internal fun NewListDialogHost(
    accountId: Long?,
    isDark: Boolean,
    onDismiss: (CaldavFilter?) -> Unit,
    onSubscribe: () -> Unit,
    onAddAccount: () -> Unit,
) {
    accountId ?: return
    val caldavDao = koinInject<CaldavDao>()
    val account by produceState<CaldavAccount?>(null, accountId) {
        value = caldavDao.getAccount(accountId) ?: run { onDismiss(null); return@produceState }
    }
    account?.let { resolved ->
        // Saveable for the same reason as the new-tag uuid: ListSettingsDialog keys its view model
        // on it, so a fresh one on rotation discards whatever the user had typed.
        val newCalendarUuid = rememberSaveable(accountId) { UUIDHelper.newUUID() }
        val newCalendar = remember(newCalendarUuid) { CaldavCalendar(uuid = newCalendarUuid) }
        ListSettingsDialog(
            account = resolved,
            calendar = newCalendar,
            isDark = isDark,
            onDismiss = { created ->
                onDismiss(created?.let { CaldavFilter(calendar = it, account = resolved) })
            },
            // Both of these navigate away. The dialog has to come down first: it draws over the
            // whole window, and at the App call site it isn't inside NavDisplay at all, so it
            // would sit on top of the screen it just navigated to.
            onSubscribe = {
                onDismiss(null)
                onSubscribe()
            },
            onAddAccount = {
                onDismiss(null)
                onAddAccount()
            },
        )
    }
}

@Composable
private fun NoTaskSelected() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.no_task_selected),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Stands in for the drawer's state during onboarding, where there is no DrawerViewModel yet. */
private val EmptyDrawerState = DrawerViewModel.State()

/**
 * Whether the stack holds an in-progress attempt to add an account.
 *
 * The whole flow, not just its first screen: the sign-in screens are pushed on top of Add Account,
 * and an account vanishing is not a reason to take away the screen the user is signing in with.
 */
private fun List<NavKey>.isAddingAccount(): Boolean = any {
    it is AddAccountDestination ||
        it is CaldavSignInDestination ||
        it is EtebaseSignInDestination
}

private val TaskListPaneMinWidth = 280.dp
private val TaskEditPaneMinWidth = 360.dp
private val SidebarMinWidth = 200.dp
private val SidebarRailWidth = 72.dp

private val PanelGutterWidth = 24.dp

private val FloatingToolbarHeight = 64.dp

internal val FloatingToolbarBottomMargin = 24.dp

private val SearchButtonInset =
    FloatingToolbarBottomMargin + (FloatingToolbarHeight - SearchButtonSize) / 2

/** Narrowest pane area that can hold a task list and a task editor side by side. */
private val TwoPaneMinWidth = TaskListPaneMinWidth + PanelGutterWidth + TaskEditPaneMinWidth

/** Window width at which two panes become desirable, matching the Expanded window size class. */
private val TwoPaneWindowMinWidth = 840.dp

/**
 * Minimum touch target for the resize handles. The handles only occupy [PanelGutterWidth] of
 * layout, so the extra width is taken as overhang into the neighbouring panes rather than as space.
 */
private val ResizeHandleTouchTargetWidth = 48.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HorizontalResizeHandle(
    onDelta: (Float) -> Unit,
    onDragStopped: () -> Unit,
    modifier: Modifier = Modifier,
    startInset: Dp = 0.dp,
    endInset: Dp = 0.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val label = stringResource(Res.string.resize_panes)
    Box(
        modifier = modifier.width(PanelGutterWidth),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            // requiredWidth rather than width: the gutter is narrower than the minimum touch
            // target, so the draggable overhangs the panes on either side instead of widening the
            // gap between them.
            modifier = Modifier
                .requiredWidth(ResizeHandleTouchTargetWidth)
                .fillMaxHeight()
                .horizontalResizeCursor()
                .draggable(
                    state = rememberDraggableState(onDelta),
                    orientation = Orientation.Horizontal,
                    interactionSource = interactionSource,
                    onDragStopped = { onDragStopped() },
                )
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            VerticalDragHandle(
                interactionSource = interactionSource,
                modifier = Modifier.offset(x = (endInset - startInset) / 2),
            )
        }
    }
}

@Composable
private fun TaskListPaneDragHandle(
    paneExpansionState: PaneExpansionState,
    width: Dp,
    maxWidth: Dp,
    storedWidth: Dp,
    onWidthChanged: (Dp) -> Unit,
) {
    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val minWidthPx = with(density) { TaskListPaneMinWidth.toPx() }
    val maxWidthPx = with(density) { maxWidth.toPx() }
    var widthPx by remember { mutableFloatStateOf(with(density) { width.toPx() }) }
    LaunchedEffect(width, density) {
        widthPx = with(density) { width.toPx() }
    }
    // Only true between the first delta and the commit, so a handle that is disposed without ever
    // being dragged doesn't overwrite the stored width with the one it happens to be showing.
    var dragged by remember { mutableStateOf(false) }
    val commitWidth by rememberUpdatedState {
        if (dragged) {
            dragged = false
            onWidthChanged(
                resolvePersistedWidth(
                    dragged = with(density) { widthPx.toDp() },
                    stored = storedWidth,
                    maxWidth = maxWidth,
                )
            )
        }
    }
    // ListDetailSceneStrategy removes this handle the moment the layout stops being two-pane, and a
    // detached draggable never reports onDragStopped - so an OS window resize, a posture change or
    // the editor closing from elsewhere mid-gesture would drop the drag. Same guard the sidebar
    // handle carries.
    DisposableEffect(Unit) {
        onDispose { commitWidth() }
    }
    HorizontalResizeHandle(
        onDelta = { delta ->
            dragged = true
            widthPx = (widthPx + if (isRtl) -delta else delta).coerceIn(minWidthPx, maxWidthPx)
            paneExpansionState.setFirstPaneWidth(widthPx.roundToInt())
        },
        onDragStopped = { commitWidth() },
        startInset = 0.dp,
        endInset = TaskEditIslandInset,
    )
}

/**
 * Picks the width to persist after a resize.
 *
 * A drag that ends pinned against [maxWidth] is the window running out of room, not a request to
 * shrink the preference, so a larger [stored] value survives. Without this a single nudge of a
 * divider in a small window permanently overwrites a preference the window can't currently honour.
 */
private fun resolvePersistedWidth(dragged: Dp, stored: Dp, maxWidth: Dp): Dp =
    if (dragged >= maxWidth && stored > dragged) stored else dragged

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TaskListPane(
    state: TaskListViewModel.State,
    chipDataProvider: ChipDataProvider,
    reporting: org.tasks.analytics.Reporting,
    viewModel: TaskListViewModel,
    themeColor: org.tasks.kmp.org.tasks.themes.ThemeColor,
    showMenuButton: Boolean,
    onShowSortSheet: () -> Unit,
    onMenuClick: () -> Unit,
    onSettingsClick: () -> Unit,
    showListSettings: Boolean = false,
    onListSettingsClick: () -> Unit = {},
    showTagSettings: Boolean = false,
    onTagSettingsClick: () -> Unit = {},
    onTaskClick: (TaskEditDestination) -> Unit,
    onCreateTask: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val floatingToolbarScrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(
        exitDirection = androidx.compose.material3.FloatingToolbarExitDirection.Bottom,
    )
    val density = androidx.compose.ui.platform.LocalDensity.current

    var topBarHeight by remember { mutableStateOf(0.dp) }
    var topBarHeightPx by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var topBarOffsetPx by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    // Custom scroll-to-hide for the overlaid TopAppBar. The TopAppBar is positioned as an
    // overlay via graphicsLayer (rather than in a Scaffold topBar slot) so the list content
    // draws underneath it as it scrolls off-screen. Built-in scroll behaviors require the
    // Scaffold slot, which reserves space and pushes content down instead of overlapping.
    val topBarScrollConnection = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource,
            ): androidx.compose.ui.geometry.Offset {
                if (available.y > 0f) {
                    topBarOffsetPx = (topBarOffsetPx + available.y).coerceIn(-topBarHeightPx, 0f)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource,
            ): androidx.compose.ui.geometry.Offset {
                if (consumed.y < 0f) {
                    topBarOffsetPx = (topBarOffsetPx + consumed.y).coerceIn(-topBarHeightPx, 0f)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    // When switching to another list, reveal the overlay top bar and floating toolbar
    // again — otherwise a new list that doesn't fill the screen can leave them stuck
    // hidden from a prior scroll.
    // Only a real switch counts. This effect also runs on first composition, and opening a task in
    // single-pane disposes this pane while its key stays on the back stack, so treating that first
    // run as a switch threw away the scroll position the nav entry had just restored.
    // Saved rather than remembered, and by key rather than by value: listState survives that
    // disposal via the entry's SaveableStateHolder while a plain remember does not, so a list
    // switched from the drawer over an open task - the sheet is one edge-swipe away there - came
    // back as a different list wearing the old one's scroll offset, top bar and toolbar still
    // hidden. EmptyFilter is the placeholder held before the default list is seeded, not a list to
    // return to, so it is never recorded as the one being switched away from.
    var previousFilterKey by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(state.filter) {
        val filter = state.filter
        if (filter is EmptyFilter) {
            return@LaunchedEffect
        }
        val previous = previousFilterKey
        val current = filter.key()
        previousFilterKey = current
        if (previous == null || previous == current) {
            return@LaunchedEffect
        }
        topBarOffsetPx = 0f
        floatingToolbarScrollBehavior.state.offset = 0f
        floatingToolbarScrollBehavior.state.contentOffset = 0f
        listState.scrollToItem(0)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .nestedScroll(topBarScrollConnection)
            .nestedScroll(floatingToolbarScrollBehavior),
    ) {
        // During pane transitions the listPane can briefly be composed with a zero or
        // near-zero size. Skip rendering in that window to avoid negative layout
        // constraints from padded descendants (e.g. FloatingToolbar's 16.dp padding)
        // and from the ExitAlwaysFloatingToolbarScrollBehavior layout modifier.
        if (maxWidth < 48.dp || maxHeight < 48.dp) return@BoxWithConstraints
        when (val results = state.tasks) {
            is TasksResults.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            is TasksResults.Results -> TaskList(
                tasks = results.tasks,
                filter = state.filter,
                chipDataProvider = chipDataProvider,
                listState = listState,
                topPadding = topBarHeight,
                onTaskClick = { task -> onTaskClick(TaskEditDestination(task.id, task.uuid)) },
                onCompleteTask = { task, newState ->
                    viewModel.onCompleteTask(task, newState)
                    if (newState) {
                        reporting.completeTask("task_list")
                    }
                },
                onToggleGroup = { viewModel.toggleCollapsed(it) },
                onToggleSubtasks = { id, collapsed ->
                    viewModel.toggleSubtasks(id, collapsed)
                },
                onFilterClick = { filter ->
                    viewModel.setFilter(filter)
                },
                is24Hour = org.tasks.time.is24HourFormat(),
            )
        }

        val statusBarTop = platformStatusBarInsets().calculateTopPadding()
        TopAppBar(
            modifier = Modifier
                .onSizeChanged { size ->
                    topBarHeightPx = size.height.toFloat()
                    topBarHeight = with(density) { size.height.toDp() }
                }
                .graphicsLayer { translationY = topBarOffsetPx },
            windowInsets = WindowInsets(top = statusBarTop),
            title = {
                Text(
                    text = state.filter.title.ifEmpty { "Tasks" },
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            actions = {
                SettingsMenuButton(
                    showListSettings = showListSettings,
                    showTagSettings = showTagSettings,
                    onSettingsClick = onSettingsClick,
                    onListSettingsClick = onListSettingsClick,
                    onTagSettingsClick = onTagSettingsClick,
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.background,
                titleContentColor = Color(themeColor.primaryColor),
            ),
        )

        val scrimColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
        StatusBarScrim(color = scrimColor, modifier = Modifier.align(Alignment.TopCenter))
        NavigationBarScrim(color = scrimColor, modifier = Modifier.align(Alignment.BottomCenter))

        FloatingToolbar(
            showMenuButton = showMenuButton,
            onMenuClick = onMenuClick,
            onSortClick = onShowSortSheet,
            onAddClick = onCreateTask,
            scrollBehavior = floatingToolbarScrollBehavior,
            fabContainerColor = Color(themeColor.primaryColor),
            fabContentColor = Color(themeColor.onPrimaryColor),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .platformNavigationBarsPadding()
                .padding(16.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListSettingsDialog(
    account: CaldavAccount,
    calendar: CaldavCalendar,
    isDark: Boolean,
    onDismiss: (CaldavCalendar?) -> Unit,
    onDeleted: () -> Unit = {},
    onSubscribe: () -> Unit,
    onAddAccount: () -> Unit = {},
    viewModelKey: String = "list_settings_${account.id}_${calendar.uuid}",
) {
    when {
        account.isGoogleTasks -> GoogleTaskListSettingsDialog(
            account = account,
            calendar = calendar,
            isDark = isDark,
            onDismiss = onDismiss,
            onDeleted = onDeleted,
            onSubscribe = onSubscribe,
            viewModelKey = viewModelKey,
        )
        account.isMicrosoft -> MicrosoftListSettingsDialog(
            account = account,
            calendar = calendar,
            isDark = isDark,
            onDismiss = onDismiss,
            onDeleted = onDeleted,
            onSubscribe = onSubscribe,
            viewModelKey = viewModelKey,
        )
        account.isEtebaseAccount -> EtebaseListSettingsDialog(
            account = account,
            calendar = calendar,
            isDark = isDark,
            onDismiss = onDismiss,
            onDeleted = onDeleted,
            onSubscribe = onSubscribe,
            viewModelKey = viewModelKey,
        )
        account.isLocalList -> LocalListSettingsDialog(
            account = account,
            calendar = calendar,
            isDark = isDark,
            onDismiss = onDismiss,
            onDeleted = onDeleted,
            onSubscribe = onSubscribe,
            onAddAccount = onAddAccount,
            viewModelKey = viewModelKey,
        )
        else -> CaldavListSettingsDialog(
            account = account,
            calendar = calendar,
            isDark = isDark,
            onDismiss = onDismiss,
            onDeleted = onDeleted,
            onSubscribe = onSubscribe,
            viewModelKey = viewModelKey,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaldavListSettingsDialog(
    account: CaldavAccount,
    calendar: CaldavCalendar,
    isDark: Boolean,
    onDismiss: (CaldavCalendar?) -> Unit,
    onDeleted: () -> Unit = {},
    onSubscribe: () -> Unit,
    viewModelKey: String,
) {
    val viewModel = koinViewModel<CaldavCalendarSettingsViewModel>(
        key = viewModelKey,
        parameters = { org.koin.core.parameter.parametersOf(isDark, account, calendar) },
    )
    val state by viewModel.state.collectAsState()

    val dismiss = { onDismiss(null) }

    BasicAlertDialog(
        onDismissRequest = {
            if (state.hasChanges) {
                viewModel.showDiscardDialog()
            } else {
                dismiss()
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        ListSettingsScreen(
            viewModel = viewModel,
            onSave = { viewModel.save { calendar -> onDismiss(calendar) } },
            onDelete = { viewModel.delete { onDeleted() } },
            onNavigateBack = dismiss,
            onSelectColor = {
                viewModel.selectColor(it?.originalColor ?: 0)
            },
            onColorWheelSelected = {
                viewModel.closeColorPicker()
                onSubscribe()
            },
            onSubscribe = { onSubscribe() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalListSettingsDialog(
    account: CaldavAccount,
    calendar: CaldavCalendar,
    isDark: Boolean,
    onDismiss: (CaldavCalendar?) -> Unit,
    onDeleted: () -> Unit = {},
    onSubscribe: () -> Unit,
    onAddAccount: () -> Unit = {},
    viewModelKey: String,
) {
    val viewModel = koinViewModel<LocalListSettingsViewModel>(
        key = viewModelKey,
        parameters = { org.koin.core.parameter.parametersOf(isDark, account, calendar) },
    )
    val state by viewModel.state.collectAsState()
    val showBanner by viewModel.showBanner.collectAsState()

    val dismiss = { onDismiss(null) }

    BasicAlertDialog(
        onDismissRequest = {
            if (state.hasChanges) {
                viewModel.showDiscardDialog()
            } else {
                dismiss()
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        ListSettingsScreen(
            viewModel = viewModel,
            onSave = {
                viewModel.save(
                    onDismiss = { onDismiss(null) },
                    onComplete = { _, calendar -> onDismiss(calendar) },
                )
            },
            onDelete = { viewModel.delete { onDeleted() } },
            onNavigateBack = dismiss,
            onSelectColor = {
                viewModel.selectColor(it?.originalColor ?: 0)
            },
            onColorWheelSelected = {
                viewModel.closeColorPicker()
                onSubscribe()
            },
            onSubscribe = { onSubscribe() },
            headerContent = {
                AnimatedBanner(
                    visible = showBanner,
                    title = stringResource(Res.string.local_list_title),
                    body = stringResource(Res.string.local_list_description),
                    dismissText = stringResource(Res.string.dismiss),
                    onDismiss = { viewModel.dismissBanner() },
                    action = stringResource(Res.string.add_account),
                    onAction = {
                        onDismiss(null)
                        onAddAccount()
                    },
                )
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoogleTaskListSettingsDialog(
    account: CaldavAccount,
    calendar: CaldavCalendar,
    isDark: Boolean,
    onDismiss: (CaldavCalendar?) -> Unit,
    onDeleted: () -> Unit = {},
    onSubscribe: () -> Unit,
    viewModelKey: String,
) {
    val viewModel = koinViewModel<GoogleTaskListSettingsViewModel>(
        key = viewModelKey,
        parameters = { org.koin.core.parameter.parametersOf(isDark, account, calendar) },
    )
    CaldavListSettingsDialog(viewModel, onDismiss, onDeleted, onSubscribe)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MicrosoftListSettingsDialog(
    account: CaldavAccount,
    calendar: CaldavCalendar,
    isDark: Boolean,
    onDismiss: (CaldavCalendar?) -> Unit,
    onDeleted: () -> Unit = {},
    onSubscribe: () -> Unit,
    viewModelKey: String,
) {
    val viewModel = koinViewModel<MicrosoftListSettingsViewModel>(
        key = viewModelKey,
        parameters = { org.koin.core.parameter.parametersOf(isDark, account, calendar) },
    )
    CaldavListSettingsDialog(viewModel, onDismiss, onDeleted, onSubscribe)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaldavListSettingsDialog(
    viewModel: ListSettingsViewModel,
    onDismiss: (CaldavCalendar?) -> Unit,
    onDeleted: () -> Unit,
    onSubscribe: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    val dismiss = { onDismiss(null) }

    BasicAlertDialog(
        onDismissRequest = {
            if (state.hasChanges) {
                viewModel.showDiscardDialog()
            } else {
                dismiss()
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        ListSettingsScreen(
            viewModel = viewModel,
            onSave = {
                viewModel.save(
                    onDismiss = { onDismiss(null) },
                    onComplete = { calendar -> onDismiss(calendar) },
                )
            },
            onDelete = { viewModel.delete { onDeleted() } },
            onNavigateBack = dismiss,
            onSelectColor = {
                viewModel.selectColor(it?.originalColor ?: 0)
            },
            onColorWheelSelected = {
                viewModel.closeColorPicker()
                onSubscribe()
            },
            onSubscribe = { onSubscribe() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EtebaseListSettingsDialog(
    account: CaldavAccount,
    calendar: CaldavCalendar,
    isDark: Boolean,
    onDismiss: (CaldavCalendar?) -> Unit,
    onDeleted: () -> Unit = {},
    onSubscribe: () -> Unit,
    viewModelKey: String,
) {
    val viewModel = koinViewModel<EtebaseCalendarSettingsViewModel>(
        key = viewModelKey,
        parameters = { org.koin.core.parameter.parametersOf(isDark, account, calendar) },
    )
    val state by viewModel.state.collectAsState()

    val dismiss = { onDismiss(null) }

    BasicAlertDialog(
        onDismissRequest = {
            if (state.hasChanges) {
                viewModel.showDiscardDialog()
            } else {
                dismiss()
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        ListSettingsScreen(
            viewModel = viewModel,
            onSave = { viewModel.save { calendar -> onDismiss(calendar) } },
            onDelete = { viewModel.delete { onDeleted() } },
            onNavigateBack = dismiss,
            onSelectColor = {
                viewModel.selectColor(it?.originalColor ?: 0)
            },
            onColorWheelSelected = {
                viewModel.closeColorPicker()
                onSubscribe()
            },
            onSubscribe = { onSubscribe() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagSettingsDialog(
    tagData: TagData,
    isDark: Boolean,
    onDismiss: (TagData?) -> Unit,
    onDeleted: () -> Unit = {},
    onSubscribe: () -> Unit,
    viewModelKey: String = "tag_settings_${tagData.remoteId}",
) {
    val viewModel = koinViewModel<TagSettingsViewModel>(
        key = viewModelKey,
        parameters = { org.koin.core.parameter.parametersOf(isDark, tagData) },
    )
    val state by viewModel.viewState.collectAsState()

    val dismiss = { onDismiss(null) }

    BasicAlertDialog(
        onDismissRequest = {
            if (state.hasChanges) {
                viewModel.showDiscardDialog()
            } else {
                dismiss()
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        TagSettingsScreen(
            viewModel = viewModel,
            onSave = {
                viewModel.save(
                    onDismiss = { onDismiss(null) },
                    onComplete = { tag -> onDismiss(tag) },
                )
            },
            onDelete = { viewModel.delete { onDeleted() } },
            onNavigateBack = dismiss,
            onSubscribe = { onSubscribe() },
            onColorWheelSelected = {
                viewModel.closeColorPicker()
                onSubscribe()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortSheetHost(
    showSortSheet: Boolean,
    onDismiss: () -> Unit,
    sortState: SortSettingsViewModel.ViewState,
    sortViewModel: SortSettingsViewModel,
    completedAndHiddenEnabled: Boolean,
) {
    if (!showSortSheet) return

    var showGroupPicker by remember { mutableStateOf(false) }
    var showSortPicker by remember { mutableStateOf(false) }
    var showCompletedPicker by remember { mutableStateOf(false) }
    var showSubtaskPicker by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
        ) {
            BottomSheetContent(
                groupMode = sortState.groupMode,
                sortMode = sortState.sortMode,
                completedMode = sortState.completedMode,
                subtaskMode = sortState.subtaskMode,
                sortAscending = sortState.sortAscending,
                groupAscending = sortState.groupAscending,
                completedAscending = sortState.completedAscending,
                subtaskAscending = sortState.subtaskAscending,
                manualSort = false,
                astridSort = false,
                completedAtBottom = sortState.completedAtBottom,
                showCompleted = sortState.showCompleted,
                showCompletedSubtasks = sortState.showCompletedSubtasks,
                showHidden = sortState.showHidden,
                showCompletedAndHiddenOptions = true,
                completedAndHiddenEnabled = completedAndHiddenEnabled,
                setSortAscending = { sortViewModel.setSortAscending(it) },
                setGroupAscending = { sortViewModel.setGroupAscending(it) },
                setCompletedAscending = { sortViewModel.setCompletedAscending(it) },
                setSubtaskAscending = { sortViewModel.setSubtaskAscending(it) },
                setCompletedAtBottom = { sortViewModel.setCompletedAtBottom(it) },
                setShowCompleted = { sortViewModel.setShowCompleted(it) },
                setShowCompletedSubtasks = { sortViewModel.setShowCompletedSubtasks(it) },
                setShowHidden = { sortViewModel.setShowHidden(it) },
                clickGroupMode = { showGroupPicker = true },
                clickSortMode = { showSortPicker = true },
                clickCompletedMode = { showCompletedPicker = true },
                clickSubtaskMode = { showSubtaskPicker = true },
            )
        }
    }
    if (showGroupPicker) {
        ModalBottomSheet(
            onDismissRequest = { showGroupPicker = false },
            containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = Color.Transparent,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                SortPicker(
                    selected = sortState.groupMode,
                    options = groupOptions,
                    onClick = {
                        sortViewModel.setGroupMode(it)
                        showGroupPicker = false
                    }
                )
            }
        }
    }
    if (showSortPicker) {
        ModalBottomSheet(
            onDismissRequest = { showSortPicker = false },
            containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = Color.Transparent,
        ) {
            SortSheetContent(
                manualSortEnabled = false,
                astridSortEnabled = false,
                manualSortSelected = false,
                selected = sortState.sortMode,
                setManualSort = {},
                setAstridSort = {},
                onSelected = {
                    sortViewModel.setSortMode(it)
                    showSortPicker = false
                }
            )
        }
    }
    if (showCompletedPicker) {
        ModalBottomSheet(
            onDismissRequest = { showCompletedPicker = false },
            containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = Color.Transparent,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                SortPicker(
                    selected = sortState.completedMode,
                    options = completedOptions,
                    onClick = {
                        sortViewModel.setCompletedMode(it)
                        showCompletedPicker = false
                    }
                )
            }
        }
    }
    if (showSubtaskPicker) {
        ModalBottomSheet(
            onDismissRequest = { showSubtaskPicker = false },
            containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = Color.Transparent,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                SortPicker(
                    selected = sortState.subtaskMode,
                    options = subtaskOptions,
                    onClick = {
                        sortViewModel.setSubtaskMode(it)
                        showSubtaskPicker = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TaskList(
    tasks: SectionedDataSource,
    filter: Filter,
    chipDataProvider: ChipDataProvider,
    listState: LazyListState = rememberLazyListState(),
    topPadding: Dp = 0.dp,
    onTaskClick: (TaskContainer) -> Unit,
    onCompleteTask: (TaskContainer, Boolean) -> Unit,
    onToggleGroup: (Long) -> Unit = {},
    onToggleSubtasks: (Long, Boolean) -> Unit = { _, _ -> },
    onFilterClick: (Filter) -> Unit = {},
    is24Hour: Boolean = false,
) {
    val dateFormatter = rememberDateFormatter(is24Hour)
    val subtaskTrees = koinInject<SubtaskTreeRegistry>()
    val deletions by remember(subtaskTrees) { subtaskTrees.deletions }
        .collectAsState(initial = emptyMap())
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = topPadding,
            bottom = 88.dp, // floating toolbar clearance
        ),
    ) {
        items(
            count = tasks.size,
            key = { if (tasks.isHeader(it)) -it.toLong() else tasks.getItem(it).id },
        ) { index ->
            if (tasks.isHeader(index)) {
                val section = tasks.getSection(index)
                SectionHeader(
                    header = if (filter.supportsSorting()) section.header else null,
                    collapsed = section.collapsed,
                    onToggle = { onToggleGroup(section.value) },
                )
                return@items
            }
            val task = tasks.getItem(index)
            val state = rowState(deletions, task)
            if (state == RowState.Hidden) {
                return@items
            }
            TaskRow(
                task = task,
                doomed = state == RowState.Doomed,
                filter = filter,
                groupMode = tasks.groupMode,
                chipDataProvider = chipDataProvider,
                is24Hour = is24Hour,
                dateFormatter = dateFormatter,
                onClick = { onTaskClick(task) },
                onToggleComplete = { onCompleteTask(task, !task.isCompleted) },
                onToggleSubtasks = { onToggleSubtasks(task.id, !task.isCollapsed) },
                onFilterClick = onFilterClick,
            )
        }
    }
}

@Composable
private fun SectionHeader(
    header: String?,
    collapsed: Boolean,
    onToggle: () -> Unit,
) {
    if (header == null) {
        return
    }
    val rotation by animateFloatAsState(
        targetValue = if (collapsed) -180f else 0f,
        animationSpec = tween(durationMillis = 250),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = header,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer { rotationZ = rotation },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FloatingToolbar(
    showMenuButton: Boolean = true,
    onMenuClick: () -> Unit,
    onSortClick: () -> Unit,
    onAddClick: () -> Unit,
    scrollBehavior: androidx.compose.material3.FloatingToolbarScrollBehavior? = null,
    fabContainerColor: Color = Color.Unspecified,
    fabContentColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
) {
    val resolvedFabContainer = fabContainerColor.takeOrElse { MaterialTheme.colorScheme.primaryContainer }
    val resolvedFabContent = fabContentColor.takeOrElse { MaterialTheme.colorScheme.onPrimaryContainer }
    HorizontalFloatingToolbar(
        expanded = true,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = resolvedFabContainer,
                contentColor = resolvedFabContent,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New task")
            }
        },
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
            toolbarContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            toolbarContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        scrollBehavior = scrollBehavior,
        modifier = modifier,
    ) {
        if (showMenuButton) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Outlined.Menu, contentDescription = "Menu")
            }
        }
        IconButton(onClick = onSortClick) {
            Icon(Icons.Outlined.SwapVert, contentDescription = "Sort")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun SettingsScreen(
    onBack: () -> Unit,
    onAddAccountClick: () -> Unit,
    onLinkDesktopClick: () -> Unit = {},
    onUpgradeClick: () -> Unit,
    onMigrateToCloud: () -> Unit = {},
    onSignInClick: () -> Unit = {},
    onSubscribedClick: () -> Unit = {},
) {
    val viewModel = koinViewModel<MainSettingsViewModel>()
    val proCardViewModel = koinViewModel<ProCardViewModel>()
    val accounts by proCardViewModel.filteredAccounts.collectAsState()
    val proCardState by proCardViewModel.proCardState.collectAsState()
    val environmentLabel by proCardViewModel.environmentLabel.collectAsState()
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val navigator = rememberListDetailPaneScaffoldNavigator<SettingsPane>()
    val scope = rememberCoroutineScope()
    val selectedContent = navigator.currentDestination
        ?.takeIf { it.pane == ListDetailPaneScaffoldRole.Detail }
        ?.contentKey

    PlatformBackHandler(enabled = selectedContent != null) {
        scope.launch {
            if (!navigator.navigateBack(BackNavigationBehavior.PopLatest)) {
                onBack()
            }
        }
    }

    ListDetailPaneScaffold(
        modifier = Modifier.fillMaxSize(),
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(Res.string.settings)) },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(Res.string.back),
                                )
                            }
                        },
                    )
                },
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    val configuration = koinInject<PlatformConfiguration>()
                    val purchaseState = koinInject<org.tasks.billing.PurchaseState>()
                    MainSettingsScreen(
                        accounts = accounts,
                        proCardState = proCardState,
                        environmentLabel = environmentLabel,
                        showBackupWarning = false,
                        showWidgets = viewModel.supportsWidgets,
                        showNotifications = configuration.supportsNotifications,
                        isDebug = viewModel.isDebug,
                        showDesktopLinking = configuration.supportsDesktopLinking
                                && !purchaseState.hasTasksAccount,
                        onLinkDesktopClick = onLinkDesktopClick,
                        onAccountClick = { account ->
                            when {
                                account.isLocalList -> {
                                    scope.launch {
                                        navigator.navigateTo(
                                            ListDetailPaneScaffoldRole.Detail,
                                            LocalAccountSettingsPane(account),
                                        )
                                    }
                                }
                                account.isCaldavAccount -> {
                                    scope.launch {
                                        navigator.navigateTo(
                                            ListDetailPaneScaffoldRole.Detail,
                                            CaldavAccountSettingsPane(account),
                                        )
                                    }
                                }
                                account.isEtebaseAccount -> {
                                    scope.launch {
                                        navigator.navigateTo(
                                            ListDetailPaneScaffoldRole.Detail,
                                            EtebaseAccountSettingsPane(account),
                                        )
                                    }
                                }
                                account.isGoogleTasks -> {
                                    scope.launch {
                                        navigator.navigateTo(
                                            ListDetailPaneScaffoldRole.Detail,
                                            GoogleTasksAccountSettingsPane(account),
                                        )
                                    }
                                }
                                account.isMicrosoft -> {
                                    scope.launch {
                                        navigator.navigateTo(
                                            ListDetailPaneScaffoldRole.Detail,
                                            MicrosoftAccountSettingsPane(account),
                                        )
                                    }
                                }
                                account.isOpenTasks -> {
                                    scope.launch {
                                        navigator.navigateTo(
                                            ListDetailPaneScaffoldRole.Detail,
                                            OpenTaskAccountSettingsPane(account),
                                        )
                                    }
                                }
                                else -> {
                                    Logger.w("App") { "Unhandled account click: ${account.accountType}" }
                                }
                            }
                        },
                        onAddAccountClick = onAddAccountClick,
                        onSettingsClick = { destination ->
                            scope.launch {
                                navigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    destination,
                                )
                            }
                        },
                        onProCardClick = {
                            when (val state = proCardState) {
                                is ProCardState.TasksOrgAccount -> {
                                    scope.launch {
                                        navigator.navigateTo(
                                            ListDetailPaneScaffoldRole.Detail,
                                            TasksAccountSettingsPane(state.account),
                                        )
                                    }
                                }
                                is ProCardState.Upgrade -> {
                                    onUpgradeClick()
                                }
                                is ProCardState.Subscribed -> {
                                    onSubscribedClick()
                                }
                                is ProCardState.SignIn -> {
                                    onSignInClick()
                                }
                                is ProCardState.Donate -> {
                                    onUpgradeClick()
                                }
                            }
                        },
                    )
                }
            }
        },
        detailPane = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface,
            ) {
                when (selectedContent) {
                    is org.tasks.compose.settings.SettingsDestination.HelpAndFeedback -> {
                        HelpAndFeedbackDetail(
                            onNavigateBack = {
                                scope.launch { navigator.navigateBack() }
                            },
                        )
                    }
                    is org.tasks.compose.settings.SettingsDestination.Notifications -> {
                        NotificationsDetail(
                            onNavigateBack = {
                                scope.launch { navigator.navigateBack() }
                            },
                        )
                    }
                    is org.tasks.compose.settings.SettingsDestination.TaskDefaults -> {
                        TaskDefaultsDetail(
                            onNavigateBack = {
                                scope.launch { navigator.navigateBack() }
                            },
                            onSignIn = onAddAccountClick,
                            onSubscribe = onUpgradeClick,
                            onAddAccount = onAddAccountClick,
                        )
                    }
                    is org.tasks.compose.settings.SettingsDestination.Debug -> {
                        org.tasks.compose.settings.DebugSettingsDetail(
                            onNavigateBack = {
                                scope.launch { navigator.navigateBack() }
                            },
                        )
                    }
                    is org.tasks.compose.settings.SettingsDestination -> {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = {
                                        Text(stringResource(selectedContent.titleRes))
                                    },
                                    navigationIcon = {
                                        IconButton(
                                            onClick = {
                                                scope.launch { navigator.navigateBack() }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = stringResource(Res.string.back),
                                            )
                                        }
                                    },
                                )
                            },
                        ) { padding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding),
                                contentAlignment = androidx.compose.ui.Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(Res.string.not_available_desktop),
                                    style = MaterialTheme.typography.headlineLarge,
                                )
                            }
                        }
                    }
                    is LocalAccountSettingsPane -> {
                        LocalAccountSettingsDetail(
                            pane = selectedContent,
                            onNavigateBack = {
                                scope.launch { navigator.navigateBack() }
                            },
                            onPurchase = onMigrateToCloud,
                            onSignIn = onSignInClick,
                        )
                    }
                    is TasksAccountSettingsPane -> {
                        TasksAccountSettingsDetail(
                            pane = selectedContent,
                            onNavigateBack = {
                                scope.launch { navigator.navigateBack() }
                            },
                            onAddAccountClick = onAddAccountClick,
                        )
                    }
                    is CaldavAccountSettingsPane -> {
                        CaldavAccountSettingsDetail(
                            pane = selectedContent,
                            onNavigateBack = {
                                scope.launch { navigator.navigateBack() }
                            },
                        )
                    }
                    is EtebaseAccountSettingsPane -> {
                        EtebaseAccountSettingsDetail(
                            pane = selectedContent,
                            onNavigateBack = {
                                scope.launch { navigator.navigateBack() }
                            },
                        )
                    }
                    is GoogleTasksAccountSettingsPane -> {
                        GoogleTasksAccountSettingsDetail(
                            pane = selectedContent,
                            onNavigateBack = {
                                scope.launch { navigator.navigateBack() }
                            },
                        )
                    }
                    is MicrosoftAccountSettingsPane -> {
                        MicrosoftAccountSettingsDetail(
                            pane = selectedContent,
                            onNavigateBack = {
                                scope.launch { navigator.navigateBack() }
                            },
                        )
                    }
                    is OpenTaskAccountSettingsPane -> {
                        OpenTaskAccountSettingsDetail(
                            pane = selectedContent,
                            onNavigateBack = {
                                scope.launch { navigator.navigateBack() }
                            },
                        )
                    }

                    null -> {}
                }
            }
        },
    )
}

private fun <T> MutableList<T>.replaceAllWith(item: T) {
    Snapshot.withMutableSnapshot {
        clear()
        add(item)
    }
}

private data class SnoozeRequest(val taskId: Long, val picking: Boolean = false)

@Composable
private fun SnoozeRequests() {
    val taskRequests = koinInject<TaskRequests>()
    val alarmService = koinInject<AlarmService>()
    val appPreferences = koinInject<AppPreferences>()
    val scope = rememberCoroutineScope()
    var request by remember { mutableStateOf<SnoozeRequest?>(null) }
    var datePrefs by remember { mutableStateOf(DatePickerPreferences()) }
    LaunchedEffect(taskRequests) {
        taskRequests.snoozeRequests.collect { taskId ->
            try {
                snapshotFlow { request }.first { it == null }
                request = SnoozeRequest(taskId)
            } catch (e: CancellationException) {
                taskRequests.snooze(taskId)
                throw e
            }
            datePrefs = guarded(
                tag = "App",
                what = "Failed to read date picker preferences",
                fallback = datePrefs,
                warnOnly = true,
            ) {
                appPreferences.datePickerPreferences()
            }
        }
    }
    DisposableEffect(taskRequests) {
        onDispose {
            request?.let { taskRequests.snooze(it.taskId) }
        }
    }
    val current = request ?: return
    fun snooze(timestamp: Long) {
        request = null
        scope.launch(NonCancellable) {
            guarded(
                tag = "App",
                what = "Failed to snooze ${current.taskId}",
                fallback = Unit,
                onFailure = { taskRequests.snooze(current.taskId) },
            ) {
                alarmService.snooze(timestamp, listOf(current.taskId))
            }
        }
    }
    if (current.picking) {
        val (initialDay, initialTime) = remember(current) {
            alarmToSelection(currentTimeMillis() + SNOOZE_PICKER_OFFSET)
        }
        DueDatePickerSheet(
            initialDay = initialDay,
            initialTime = initialTime,
            is24Hour = org.tasks.time.is24HourFormat(),
            showNoDate = false,
            showNoTime = false,
            times = datePrefs.quickPickTimes,
            onSelected = { day, time ->
                val timestamp = alarmFromSelection(day, time)
                if (timestamp > 0) snooze(timestamp) else request = null
            },
            onDismiss = { request = null },
        )
    } else {
        SnoozeDialog(
            visible = true,
            loadTimes = { appPreferences.datePickerPreferences().quickPickTimes },
            is24Hour = org.tasks.time.is24HourFormat(),
            onSelected = { snooze(it) },
            onPickDateTime = { request = current.copy(picking = true) },
            onDismiss = { request = null },
        )
    }
}

/**
 * Adds [key] without ever leaving two equal keys on the stack.
 *
 * Navigation 3 uses the key as the content key for both the saveable state holder and the
 * ViewModel store, so a duplicate shares - and on pop destroys - the state of the entry it
 * duplicates. An existing entry is moved to the top instead, which keeps its state intact.
 */
private fun MutableList<NavKey>.push(key: NavKey) {
    if (lastOrNull() == key) return
    Snapshot.withMutableSnapshot {
        remove(key)
        add(key)
    }
}

