package org.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue

import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.BasicAlertDialog
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.tasks.auth.OAuthProvider
import org.tasks.compose.drawer.DrawerItem
import org.tasks.compose.drawer.TaskListDrawer

import co.touchlab.kermit.Logger
import org.tasks.auth.TasksServerEnvironment
import org.tasks.compose.NavigationBarScrim
import org.tasks.compose.PlatformBackHandler
import org.tasks.compose.settings.CaldavAccountSettingsDetail
import org.tasks.compose.settings.CaldavAccountSettingsPane
import org.tasks.compose.settings.EtebaseAccountSettingsDetail
import org.tasks.compose.settings.EtebaseAccountSettingsPane
import org.tasks.compose.settings.LocalAccountSettingsDetail
import org.tasks.compose.settings.LocalAccountSettingsPane
import org.tasks.compose.settings.MainSettingsScreen
import org.tasks.compose.settings.OpenTaskAccountSettingsDetail
import org.tasks.compose.settings.OpenTaskAccountSettingsPane
import org.tasks.compose.settings.ProCardState
import org.tasks.compose.settings.SettingsPane
import org.tasks.compose.settings.TasksAccountSettingsDetail
import org.tasks.compose.settings.TasksAccountSettingsPane
import org.tasks.compose.settings.DesktopProScreen
import org.tasks.compose.settings.LinkDesktopScreen
import org.tasks.compose.StatusBarScrim
import org.tasks.compose.platformSidebarInsets
import org.tasks.compose.platformStatusBarInsets
import org.tasks.compose.platformNavigationBarsPadding
import org.tasks.compose.SignInProvider
import org.tasks.compose.SignInProviderDialog
import org.tasks.compose.WelcomeScreenLayout
import org.tasks.compose.accounts.AddAccountScreen
import org.tasks.compose.accounts.AddAccountViewModel
import org.tasks.compose.accounts.Platform
import org.tasks.compose.pricing.PricingMode
import org.tasks.compose.pricing.PricingScreen
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.markdownAnimations
import org.jetbrains.compose.resources.stringResource
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Reporting
import org.tasks.compose.chips.ChipDataProvider
import org.tasks.compose.chips.ChipGroup
import org.tasks.compose.chips.Chip
import org.tasks.compose.chips.StartDateChip
import org.tasks.compose.chips.SubtaskChip
import org.tasks.data.isHidden
import org.tasks.time.startOfDay
import org.tasks.compose.sort.BottomSheetContent
import org.tasks.compose.sort.SortPicker
import org.tasks.compose.sort.SortSheetContent
import org.tasks.compose.sort.completedOptions
import org.tasks.compose.sort.groupOptions
import org.tasks.compose.sort.subtaskOptions
import org.tasks.viewmodel.SortSettingsViewModel
import org.tasks.kmp.org.tasks.themes.ColorProvider
import org.tasks.themes.BLUE
import org.tasks.themes.TasksTheme
import org.tasks.data.TaskContainer
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.filters.MyTasksFilter
import org.tasks.filters.PlaceFilter
import org.tasks.filters.TagFilter
import com.todoroo.astrid.core.SortHelper
import org.tasks.kmp.org.tasks.time.getRelativeDateTime
import org.tasks.kmp.formatTime
import org.tasks.tasklist.HeaderFormatter
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.tasklist.SectionedDataSource
import org.tasks.tasklist.TasksResults
import org.tasks.viewmodel.AppViewModel
import org.tasks.viewmodel.DrawerViewModel
import org.tasks.viewmodel.TaskEditViewModel
import org.tasks.viewmodel.MainSettingsViewModel
import org.tasks.viewmodel.ProCardViewModel
import org.tasks.viewmodel.TaskListViewModel
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.settings
import tasks.kmp.generated.resources.show_less
import tasks.kmp.generated.resources.show_more
import tasks.kmp.generated.resources.url_google_play
import tasks.kmp.generated.resources.url_sponsor

@Serializable
data object WelcomeDestination : NavKey

@Serializable
data object AddAccountDestination : NavKey

@Serializable
data object TaskListDestination : NavKey

@Serializable
data object CaldavSignInDestination : NavKey

@Serializable
data object EtebaseSignInDestination : NavKey

@Serializable
data object SettingsDestination : NavKey

@Serializable
data object LinkDesktopDestination : NavKey

@Serializable
data object DesktopProDestination : NavKey

@Serializable
data class PricingDestination(
    val mode: PricingMode = PricingMode.BOTH,
    val source: String = AnalyticsEvents.SOURCE_SETTINGS,
) : NavKey

@OptIn(ExperimentalMaterial3Api::class)
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
                val normalized = if (uri.contains("://")) uri else "https://$uri"
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
            val hasAccount by appViewModel.hasAccount.collectAsState()

            if (hasAccount == null) {
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
                            subclass(SettingsDestination::class, SettingsDestination.serializer())
                            subclass(LinkDesktopDestination::class, LinkDesktopDestination.serializer())
                            subclass(DesktopProDestination::class, DesktopProDestination.serializer())
                            subclass(PricingDestination::class, PricingDestination.serializer())
                        }
                    }
                },
                if (hasAccount == true) TaskListDestination else WelcomeDestination,
            )

            LaunchedEffect(hasAccount) {
                when (hasAccount) {
                    true -> {
                        if (backStack.lastOrNull() !is TaskListDestination) {
                            backStack.clear()
                            backStack.add(TaskListDestination)
                        }
                    }
                    false -> {
                        if (backStack.lastOrNull() !is WelcomeDestination
                            && backStack.lastOrNull() !is AddAccountDestination) {
                            backStack.clear()
                            backStack.add(WelcomeDestination)
                        }
                    }
                    null -> {}
                }
            }

            NavDisplay(
                backStack = backStack,
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
                                backStack.add(AddAccountDestination)
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
                                // On desktop, gate CalDAV/EteSync behind pro
                                if (configuration.billingProvider == org.tasks.billing.BillingProvider.PADDLE
                                    && (platform == Platform.CALDAV || platform == Platform.ETEBASE)
                                    && !addAccountViewModel.hasPro
                                ) {
                                    backStack.add(PricingDestination(mode = PricingMode.NYP_ONLY, source = platform.name))
                                    return@AddAccountScreen
                                }
                                when (platform) {
                                    Platform.TASKS_ORG -> {
                                        backStack.add(PricingDestination(mode = PricingMode.CLOUD_ONLY, source = platform.name))
                                    }
                                    Platform.CALDAV -> backStack.add(CaldavSignInDestination)
                                    Platform.ETEBASE -> backStack.add(EtebaseSignInDestination)
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
                        )
                    }
                    entry<CaldavSignInDestination> {
                        org.tasks.compose.settings.CaldavSignInScreen(
                            onNavigateBack = { backStack.removeLastOrNull() },
                            onAccountCreated = { backStack.removeLastOrNull() },
                        )
                    }
                    entry<EtebaseSignInDestination> {
                        org.tasks.compose.settings.EtebaseSignInScreen(
                            onNavigateBack = { backStack.removeLastOrNull() },
                            onAccountCreated = { backStack.removeLastOrNull() },
                        )
                    }
                    entry<TaskListDestination> {
                        val taskListViewModel = koinViewModel<TaskListViewModel>()
                        val drawerViewModel = koinViewModel<DrawerViewModel>()
                        LaunchedEffect(Unit) {
                            taskListViewModel.setFilter(MyTasksFilter.create())
                        }
                        TaskListScreen(
                            viewModel = taskListViewModel,
                            drawerViewModel = drawerViewModel,
                            onSettingsClick = { backStack.add(SettingsDestination) },
                        )
                    }
                    entry<SettingsDestination> {
                        val purchaseState = koinInject<org.tasks.billing.PurchaseState>()
                        SettingsScreen(
                            onBack = { backStack.removeLastOrNull() },
                            onAddAccountClick = { backStack.add(AddAccountDestination) },
                            onLinkDesktopClick = {
                                if (purchaseState.hasPro) {
                                    backStack.add(LinkDesktopDestination)
                                } else {
                                    co.touchlab.kermit.Logger.withTag("App")
                                        .e { "Link desktop clicked without pro subscription" }
                                }
                            },
                            onUpgradeClick = { backStack.add(PricingDestination()) },
                        )
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
                    entry<DesktopProDestination> {
                        val desktopLinkClient = koinInject<org.tasks.billing.DesktopLinkClient>()
                        val gitHubSponsorClient = koinInject<org.tasks.billing.GitHubSponsorClient>()
                        LaunchedEffect(Unit) {
                            reporting.logEvent(AnalyticsEvents.SCREEN_RESTORE_PURCHASES)
                        }
                        DesktopProScreen(
                            onBack = { backStack.removeLastOrNull() },
                            onCreateLink = { desktopLinkClient.createLink() },
                            onPollStatus = { code -> desktopLinkClient.pollStatus(code) },
                            onLinkSuccess = { jwt, refreshToken, sku, formattedPrice ->
                                reporting.logEvent(
                                    AnalyticsEvents.RESTORE_SUCCESS,
                                    AnalyticsEvents.PARAM_SELECTION to AnalyticsEvents.SELECTION_GOOGLE_PLAY,
                                )
                                desktopLinkClient.onLinkSuccess(jwt, refreshToken, sku, formattedPrice)
                            },
                            onGitHubSignIn = { gitHubSponsorClient.signIn(openUrl) },
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
                        )
                    }
                    entry<PricingDestination> { destination ->
                        var showSignInDialog by remember { mutableStateOf(false) }
                        val addAccountViewModel = koinViewModel<AddAccountViewModel>()
                        val signInState by addAccountViewModel.signInState.collectAsState()
                        LaunchedEffect(Unit) {
                            reporting.logEvent(
                                AnalyticsEvents.SCREEN_PRICING,
                                AnalyticsEvents.PARAM_SOURCE to destination.source,
                                AnalyticsEvents.PARAM_TYPE to destination.mode.name,
                            )
                            addAccountViewModel.accountAdded.collect {
                                backStack.removeLastOrNull()
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
                            onRestorePurchases = { backStack.add(DesktopProDestination) },
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
                        )
                    }
                },
            )
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
                    text = "Sign in failed",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = errorState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp),
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
                ) {
                    Text("OK")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TaskListScreen(
    viewModel: TaskListViewModel,
    drawerViewModel: DrawerViewModel,
    onSettingsClick: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val drawerState by drawerViewModel.state.collectAsState()
    val headerFormatter = koinInject<HeaderFormatter>()
    val chipDataProvider = koinInject<ChipDataProvider>()
    val reporting = koinInject<Reporting>()
    val sortViewModel = koinViewModel<SortSettingsViewModel>()
    val sortState by sortViewModel.state.collectAsState()
    var showSortSheet by remember { mutableStateOf(false) }
    val materialDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val navigator = rememberListDetailPaneScaffoldNavigator<Long>()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(state.filter) {
        drawerViewModel.setSelectedFilter(state.filter)
    }

    // TODO: use user's theme color preference instead of BLUE
    val filterTint = state.filter.tint
    val isDark = isSystemInDarkTheme()
    val themeColor = remember(filterTint, isDark) {
        ColorProvider.themeColor(
            seedColor = if (filterTint != 0) filterTint else BLUE,
            isDark = isDark,
        )
    }

    var sidebarExpanded by remember { mutableStateOf(false) }
    val selectedTaskId = navigator.currentDestination?.contentKey

    val onDrawerItemClick: (DrawerItem) -> Unit = { item ->
        when (item) {
            is DrawerItem.Filter -> {
                viewModel.setFilter(item.filter)
                drawerViewModel.setSelectedFilter(item.filter)
                if (materialDrawerState.isOpen) {
                    scope.launch { materialDrawerState.close() }
                }
                scope.launch { if (navigator.canNavigateBack()) navigator.navigateBack() }
            }
            is DrawerItem.Header -> {
                drawerViewModel.toggleCollapsed(item.header)
            }
        }
    }

    val hasDetailOpen = selectedTaskId != null
    val listPaneHidden = navigator.canNavigateBack()

    // Note: when the detail pane is open, TaskEditScreen installs its own back handler
    // so that back/escape saves the edit before navigating back.
    PlatformBackHandler(enabled = sidebarExpanded && !hasDetailOpen) {
        sidebarExpanded = false
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        when {
            // Wide/Medium: sidebar + list/detail content
            maxWidth >= 600.dp -> {
                val showSidebar = !listPaneHidden && !(hasDetailOpen && maxWidth < 840.dp)
                val sidebarInsetsPadding = platformSidebarInsets()
                val sidebarListState = androidx.compose.foundation.lazy.rememberLazyListState()
                val sidebarWidth by androidx.compose.animation.core.animateDpAsState(
                    targetValue = if (sidebarExpanded) 280.dp else 72.dp,
                )
                val cutoutPadding = sidebarInsetsPadding.calculateLeftPadding(LayoutDirection.Ltr)
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
                                onQueryChange = { drawerViewModel.setMenuQuery(it) },
                                onClick = onDrawerItemClick,
                                onAddClick = { /* TODO: add new list/tag/place */ },
                                onErrorClick = { /* TODO: show sync error */ },
                                expanded = sidebarExpanded,
                                listState = sidebarListState,
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
                    TaskListContent(
                        state = state,
                        navigator = navigator,
                        selectedTaskId = selectedTaskId,
                        headerFormatter = headerFormatter,
                        chipDataProvider = chipDataProvider,
                        reporting = reporting,
                        viewModel = viewModel,
                        themeColor = themeColor,
                        onShowSortSheet = { showSortSheet = true },
                        onTaskClick = { taskId ->
                            scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, taskId) }
                        },
                        showMenuButton = true,
                        onMenuClick = { sidebarExpanded = !sidebarExpanded },
                        onSettingsClick = onSettingsClick,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            // Narrow: modal drawer
            else -> {
                PlatformBackHandler(enabled = materialDrawerState.isOpen) {
                    scope.launch { materialDrawerState.close() }
                }
                ModalNavigationDrawer(
                    drawerState = materialDrawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            windowInsets = WindowInsets(0),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(
                                        start = platformSidebarInsets()
                                            .calculateLeftPadding(LayoutDirection.Ltr),
                                    ),
                            ) {
                                TaskListDrawer(
                                    drawerOpen = materialDrawerState.isOpen,
                                    drawerState = drawerState,
                                    onQueryChange = { drawerViewModel.setMenuQuery(it) },
                                    onClick = onDrawerItemClick,
                                    onAddClick = { /* TODO: add new list/tag/place */ },
                                    onErrorClick = { /* TODO: show sync error */ },
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
                    },
                ) {
                    TaskListContent(
                        state = state,
                        navigator = navigator,
                        selectedTaskId = selectedTaskId,
                        headerFormatter = headerFormatter,
                        chipDataProvider = chipDataProvider,
                        reporting = reporting,
                        viewModel = viewModel,
                        themeColor = themeColor,
                        onShowSortSheet = { showSortSheet = true },
                        onTaskClick = { taskId ->
                            scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, taskId) }
                        },
                        showMenuButton = true,
                        onMenuClick = {
                            scope.launch {
                                if (materialDrawerState.isOpen) materialDrawerState.close()
                                else materialDrawerState.open()
                            }
                        },
                        onSettingsClick = onSettingsClick,
                    )
                }
            }
        }
    }

    SortSheetHost(
        showSortSheet = showSortSheet,
        onDismiss = { showSortSheet = false },
        sortState = sortState,
        sortViewModel = sortViewModel,
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TaskListContent(
    state: TaskListViewModel.State,
    navigator: androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator<Long>,
    selectedTaskId: Long?,
    headerFormatter: HeaderFormatter,
    chipDataProvider: ChipDataProvider,
    reporting: org.tasks.analytics.Reporting,
    viewModel: TaskListViewModel,
    themeColor: org.tasks.kmp.org.tasks.themes.ThemeColor,
    onShowSortSheet: () -> Unit,
    onTaskClick: (Long) -> Unit,
    showMenuButton: Boolean,
    onMenuClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val taskEditViewModel = koinViewModel<TaskEditViewModel>()

    ListDetailPaneScaffold(
        modifier = modifier.fillMaxSize(),
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            TaskListPane(
                state = state,
                headerFormatter = headerFormatter,
                chipDataProvider = chipDataProvider,
                reporting = reporting,
                viewModel = viewModel,
                themeColor = themeColor,
                showMenuButton = showMenuButton,
                onShowSortSheet = onShowSortSheet,
                onMenuClick = onMenuClick,
                onSettingsClick = onSettingsClick,
                onTaskClick = onTaskClick,
                onCreateTask = {
                    reporting.addTask("fab")
                    scope.launch {
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, 0L)
                    }
                },
                modifier = Modifier.preferredWidth(TaskListPanePreferredWidth),
            )
        },
        detailPane = {
            selectedTaskId?.let { taskId ->
                TaskEditScreen(
                    viewModel = taskEditViewModel,
                    taskId = taskId.takeIf { it > 0 },
                    onClose = {
                        scope.launch {
                            navigator.navigateBack(BackNavigationBehavior.PopLatest)
                        }
                    },
                )
            }
        },
    )
}

private val TaskListPanePreferredWidth = 400.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TaskListPane(
    state: TaskListViewModel.State,
    headerFormatter: HeaderFormatter,
    chipDataProvider: ChipDataProvider,
    reporting: org.tasks.analytics.Reporting,
    viewModel: TaskListViewModel,
    themeColor: org.tasks.kmp.org.tasks.themes.ThemeColor,
    showMenuButton: Boolean,
    onShowSortSheet: () -> Unit,
    onMenuClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTaskClick: (Long) -> Unit,
    onCreateTask: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
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
    LaunchedEffect(state.filter) {
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
        // During narrow-mode pane transitions the listPane can briefly be composed with
        // a zero/near-zero width. Skip rendering in that window to avoid negative layout
        // constraints from padded descendants (e.g. FloatingToolbar's 16.dp padding).
        if (maxWidth < 48.dp) return@BoxWithConstraints
        when (val results = state.tasks) {
            is TasksResults.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            is TasksResults.Results -> TaskList(
                tasks = results.tasks,
                filter = state.filter,
                headerFormatter = headerFormatter,
                chipDataProvider = chipDataProvider,
                listState = listState,
                topPadding = topBarHeight,
                onTaskClick = { task -> onTaskClick(task.id) },
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
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(Res.string.settings),
                    )
                }
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
            onSearchClick = { /* TODO: search */ },
            onSortClick = onShowSortSheet,
            onMoreClick = { /* TODO: more options */ },
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
private fun SortSheetHost(
    showSortSheet: Boolean,
    onDismiss: () -> Unit,
    sortState: SortSettingsViewModel.ViewState,
    sortViewModel: SortSettingsViewModel,
) {
    if (!showSortSheet) return

    var showGroupPicker by remember { mutableStateOf(false) }
    var showSortPicker by remember { mutableStateOf(false) }
    var showCompletedPicker by remember { mutableStateOf(false) }
    var showSubtaskPicker by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                setSortAscending = { sortViewModel.setSortAscending(it) },
                setGroupAscending = { sortViewModel.setGroupAscending(it) },
                setCompletedAscending = { sortViewModel.setCompletedAscending(it) },
                setSubtaskAscending = { sortViewModel.setSubtaskAscending(it) },
                setCompletedAtBottom = { sortViewModel.setCompletedAtBottom(it) },
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
    headerFormatter: HeaderFormatter,
    chipDataProvider: ChipDataProvider,
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    topPadding: Dp = 0.dp,
    onTaskClick: (TaskContainer) -> Unit,
    onCompleteTask: (TaskContainer, Boolean) -> Unit,
    onToggleGroup: (Long) -> Unit = {},
    onToggleSubtasks: (Long, Boolean) -> Unit = { _, _ -> },
    onFilterClick: (Filter) -> Unit = {},
    is24Hour: Boolean = false,
) {
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
                    headerValue = section.value,
                    collapsed = section.collapsed,
                    groupMode = tasks.groupMode,
                    headerFormatter = headerFormatter,
                    onToggle = { onToggleGroup(section.value) },
                )
                return@items
            }
            val task = tasks.getItem(index)
            TaskRow(
                task = task,
                filter = filter,
                groupMode = tasks.groupMode,
                chipDataProvider = chipDataProvider,
                is24Hour = is24Hour,
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
    headerValue: Long,
    collapsed: Boolean,
    groupMode: Int,
    headerFormatter: HeaderFormatter,
    onToggle: () -> Unit,
) {
    val headerText by produceState("", headerValue, groupMode) {
        value = headerFormatter.headerString(headerValue, groupMode)
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
            text = headerText,
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

@Composable
private fun TaskRow(
    task: TaskContainer,
    filter: Filter,
    groupMode: Int,
    chipDataProvider: ChipDataProvider,
    is24Hour: Boolean,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
    onToggleSubtasks: () -> Unit,
    onFilterClick: (Filter) -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val checkColor = if (task.isCompleted) {
        MaterialTheme.colorScheme.outline
    } else {
        when (task.priority) {
            0 -> MaterialTheme.colorScheme.error
            1 -> MaterialTheme.colorScheme.tertiary
            2 -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                start = (20 * task.indent).dp,
                end = 16.dp,
            ),
        verticalAlignment = Alignment.Top,
    ) {
        IconButton(
            onClick = onToggleComplete,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = if (task.isCompleted)
                    Icons.Filled.CheckCircle
                else
                    Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = checkColor,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(modifier = Modifier.weight(1f).padding(top = 12.dp, bottom = 12.dp)) {
            val dueDateText by produceState<String?>(null, task.dueDate, groupMode, is24Hour) {
                value = if (!task.hasDueDate()) {
                    null
                } else if (groupMode == SortHelper.SORT_DUE
                    && (task.sortGroup ?: 0) >= currentTimeMillis().startOfDay()
                ) {
                    if (task.hasDueTime()) formatTime(task.dueDate, is24Hour) else null
                } else {
                    getRelativeDateTime(task.dueDate, is24Hour)
                }
            }
            val isOverdue = task.hasDueDate() && !task.isCompleted
                    && task.dueDate < (if (task.hasDueTime()) currentTimeMillis() else currentTimeMillis().startOfDay())
            Row(verticalAlignment = Alignment.CenterVertically) {
                val titleColor = if (task.isCompleted) {
                    MaterialTheme.colorScheme.outline
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
                Markdown(
                    content = task.title ?: "",
                    colors = markdownColor(text = titleColor),
                    typography = markdownTypography(
                        paragraph = MaterialTheme.typography.bodyLarge.copy(
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        ),
                    ),
                    animations = markdownAnimations(animateTextSize = { this }),
                    modifier = Modifier.weight(1f),
                )
                if (dueDateText != null) {
                    Text(
                        text = dueDateText!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isOverdue) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            if (!task.notes.isNullOrBlank()) {
                val content = task.notes!!.trim()
                var expanded by remember { mutableStateOf(false) }
                val lines = content.lines()
                val hasMore = lines.size > 2
                val mdColors = markdownColor(
                    text = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val mdTypography = markdownTypography(
                    paragraph = MaterialTheme.typography.bodyMedium,
                )
                Markdown(
                    content = if (expanded || !hasMore) content
                              else lines.take(2).joinToString("\n"),
                    colors = mdColors,
                    typography = mdTypography,
                    animations = markdownAnimations(
                        animateTextSize = { this },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (hasMore) {
                    Text(
                        text = stringResource(if (expanded) Res.string.show_less else Res.string.show_more),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 36.dp)
                            .clickable { expanded = !expanded }
                            .wrapContentHeight(Alignment.CenterVertically),
                    )
                }
            }
            val startDate = task.task.hideUntil
            val showStartDate = task.task.isHidden
                    && startDate != task.dueDate
                    && startDate != task.dueDate.startOfDay()
            val showList = task.indent == 0
                    && filter !is CaldavFilter
                    && chipDataProvider.getCaldavList(task.caldav) != null
            val showPlace = task.hasLocation()
                    && filter !is PlaceFilter
            val tags = task.tagsString
                ?.takeIf { it.isNotBlank() }
                ?.split(",")
                ?.let { uuids ->
                    if (filter is TagFilter) uuids - filter.uuid else uuids
                }
                ?.mapNotNull { chipDataProvider.getTag(it) }
                ?.sortedBy { it.title }
                ?: emptyList()
            val hasChips = task.hasChildren() || showStartDate || showList || showPlace || tags.isNotEmpty()
            if (hasChips) {
                ChipGroup(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                    if (task.hasChildren()) {
                        SubtaskChip(
                            collapsed = task.isCollapsed,
                            children = task.children,
                            compact = true,
                            onClick = onToggleSubtasks,
                        )
                    }
                    if (showStartDate) {
                        StartDateChip(
                            sortGroup = task.sortGroup,
                            startDate = startDate,
                            compact = true,
                            timeOnly = false,
                            is24HourFormat = is24Hour,
                            chipColor = chipColor(0, isDark),
                        )
                    }
                    if (showPlace) {
                        task.location?.let { location ->
                            Chip(
                                text = location.place.displayName,
                                icon = location.place.icon ?: "place",
                                color = chipColor(location.place.color, isDark),
                                onClick = { onFilterClick(PlaceFilter(location.place)) },
                            )
                        }
                    }
                    if (showList) {
                        chipDataProvider.getCaldavList(task.caldav)?.let { list ->
                            Chip(
                                text = list.title,
                                icon = list.icon ?: "list",
                                color = chipColor(list.tint, isDark),
                                onClick = { onFilterClick(list) },
                            )
                        }
                    }
                    tags.forEach { tag ->
                        Chip(
                            text = tag.title,
                            icon = tag.icon ?: "label",
                            color = chipColor(tag.tint, isDark),
                            onClick = { onFilterClick(tag) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun chipColor(seedColor: Int, isDark: Boolean): Color {
    return if (seedColor == 0) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        Color(
            org.tasks.themes.chipColors(seedColor, isDark).backgroundColor
                    or 0xFF000000.toInt()
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FloatingToolbar(
    showMenuButton: Boolean = true,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSortClick: () -> Unit,
    onMoreClick: () -> Unit,
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
        IconButton(onClick = onSearchClick) {
            Icon(Icons.Outlined.Search, contentDescription = "Search")
        }
        IconButton(onClick = onSortClick) {
            Icon(Icons.Outlined.SwapVert, contentDescription = "Sort")
        }
        IconButton(onClick = onMoreClick) {
            Icon(Icons.Outlined.MoreVert, contentDescription = "More")
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
) {
    val viewModel = koinViewModel<MainSettingsViewModel>()
    val proCardViewModel = koinViewModel<ProCardViewModel>()
    val accounts by proCardViewModel.filteredAccounts.collectAsState()
    val proCardState by proCardViewModel.proCardState.collectAsState()
    val environmentLabel by proCardViewModel.environmentLabel.collectAsState()
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val navigator = rememberListDetailPaneScaffoldNavigator<SettingsPane>()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val selectedContent = navigator.currentDestination
        ?.takeIf { it.pane == ListDetailPaneScaffoldRole.Detail }
        ?.contentKey

    PlatformBackHandler(enabled = selectedContent != null) {
        scope.launch {
            if (!navigator.navigateBack()) {
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
                                is ProCardState.Subscribed -> {}
                                is ProCardState.SignIn -> {}
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
                            )
                        }
                    }
                    is LocalAccountSettingsPane -> {
                        LocalAccountSettingsDetail(
                            pane = selectedContent,
                            onNavigateBack = {
                                scope.launch { navigator.navigateBack() }
                            },
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

