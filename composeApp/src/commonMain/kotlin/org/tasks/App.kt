package org.tasks

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.tasks.auth.OAuthProvider
import org.tasks.compose.drawer.DrawerItem
import org.tasks.compose.drawer.TaskListDrawer
import org.tasks.auth.TasksServerEnvironment
import org.tasks.compose.NavigationBarScrim
import org.tasks.compose.PlatformBackHandler
import org.tasks.compose.StatusBarScrim
import org.tasks.compose.platformNavigationBarsPadding
import org.tasks.compose.SignInProvider
import org.tasks.compose.SignInProviderDialog
import org.tasks.compose.WelcomeScreenLayout
import org.tasks.compose.accounts.AddAccountScreen
import org.tasks.compose.accounts.Platform
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Reporting
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
import org.tasks.filters.MyTasksFilter
import org.tasks.tasklist.HeaderFormatter
import org.tasks.tasklist.SectionedDataSource
import org.tasks.tasklist.TasksResults
import org.tasks.viewmodel.AddAccountViewModel
import org.tasks.viewmodel.AppViewModel
import org.tasks.viewmodel.DrawerViewModel
import org.tasks.viewmodel.TaskListViewModel

@Serializable
data object WelcomeDestination : NavKey

@Serializable
data object AddAccountDestination : NavKey

@Serializable
data object TaskListDestination : NavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    openUrl: (String) -> Unit = {},
    environments: List<TasksServerEnvironment.Environment> = emptyList(),
    currentEnvironment: String = TasksServerEnvironment.ENV_PRODUCTION,
    onSelectEnvironment: (String) -> Unit = {},
) {
    TasksTheme {
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
                            subclass(TaskListDestination::class, TaskListDestination.serializer())
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
                                reporting.logEvent(AnalyticsEvents.ONBOARDING_COMPLETE, AnalyticsEvents.PARAM_SELECTION to "local")
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
                        val signInState by addAccountViewModel.signInState.collectAsState()
                        var showProviderPicker by remember { mutableStateOf(false) }
                        AddAccountScreen(
                            configuration = configuration,
                            hasTasksAccount = false,
                            hasPro = false,
                            needsConsent = false,
                            onBack = { backStack.removeLastOrNull() },
                            signIn = { platform ->
                                reporting.logEvent(
                                    AnalyticsEvents.ADD_ACCOUNT,
                                    AnalyticsEvents.PARAM_SOURCE to "onboarding",
                                    AnalyticsEvents.PARAM_SELECTION to platform.name,
                                )
                                when (platform) {
                                    Platform.TASKS_ORG -> showProviderPicker = true
                                    else -> addAccountViewModel.signIn(platform)
                                }
                            },
                            openUrl = { platform ->
                                // TODO: handle open URL for platform
                            },
                            openLegalUrl = openUrl,
                        )
                        if (showProviderPicker) {
                            BasicAlertDialog(onDismissRequest = { showProviderPicker = false }) {
                                SignInProviderDialog(
                                    onSelected = { provider ->
                                        showProviderPicker = false
                                        val oauthProvider = when (provider) {
                                            SignInProvider.GOOGLE -> OAuthProvider.GOOGLE
                                            SignInProvider.GITHUB -> OAuthProvider.GITHUB
                                        }
                                        addAccountViewModel.signIn(
                                            platform = Platform.TASKS_ORG,
                                            provider = oauthProvider,
                                            openUrl = openUrl,
                                        )
                                    },
                                    onHelp = {
                                        showProviderPicker = false
                                        openUrl("https://tasks.org/docs/sync")
                                    },
                                    onCancel = { showProviderPicker = false },
                                )
                            }
                        }
                        val errorState = signInState as? AddAccountViewModel.SignInState.Error
                        if (errorState != null) {
                            BasicAlertDialog(onDismissRequest = { addAccountViewModel.dismissError() }) {
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
                                            onClick = { addAccountViewModel.dismissError() },
                                            modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
                                        ) {
                                            Text("OK")
                                        }
                                    }
                                }
                            }
                        }
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
                        )
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TaskListScreen(
    viewModel: TaskListViewModel,
    drawerViewModel: DrawerViewModel,
) {
    val state by viewModel.state.collectAsState()
    val drawerState by drawerViewModel.state.collectAsState()
    val headerFormatter = koinInject<HeaderFormatter>()
    val reporting = koinInject<Reporting>()
    val sortViewModel = koinViewModel<SortSettingsViewModel>()
    val sortState by sortViewModel.state.collectAsState()
    var showSortSheet by remember { mutableStateOf(false) }
    val materialDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val navigator = rememberListDetailPaneScaffoldNavigator<Long>()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val floatingToolbarScrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(
        exitDirection = androidx.compose.material3.FloatingToolbarExitDirection.Bottom,
    )
    val density = androidx.compose.ui.platform.LocalDensity.current
    var topBarHeight by remember { mutableStateOf(0.dp) }
    var topBarHeightPx by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var topBarOffsetPx by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
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

    PlatformBackHandler(enabled = materialDrawerState.isOpen) {
        scope.launch { materialDrawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = materialDrawerState,
        drawerContent = {
            ModalDrawerSheet(
                windowInsets = WindowInsets(0),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    TaskListDrawer(
                        drawerOpen = materialDrawerState.isOpen,
                        drawerState = drawerState,
                        onQueryChange = { drawerViewModel.setMenuQuery(it) },
                        onClick = { item ->
                            when (item) {
                                is DrawerItem.Filter -> {
                                    viewModel.setFilter(item.filter)
                                    drawerViewModel.setSelectedFilter(item.filter)
                                    scope.launch { materialDrawerState.close() }
                                }
                                is DrawerItem.Header -> {
                                    drawerViewModel.toggleCollapsed(item.header)
                                }
                            }
                        },
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(topBarScrollConnection)
            .nestedScroll(floatingToolbarScrollBehavior),
    ) {
        ListDetailPaneScaffold(
            directive = navigator.scaffoldDirective,
            value = navigator.scaffoldValue,
            listPane = {
                when (val results = state.tasks) {
                    is TasksResults.Loading -> {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is TasksResults.Results -> {
                        TaskList(
                            tasks = results.tasks,
                            headerFormatter = headerFormatter,
                            listState = listState,
                            topPadding = topBarHeight,
                            onTaskClick = { task ->
                                scope.launch {
                                    navigator.navigateTo(
                                        ListDetailPaneScaffoldRole.Detail,
                                        task.id,
                                    )
                                }
                            },
                            onCompleteTask = { task, newState ->
                                viewModel.onCompleteTask(task, newState)
                                if (newState) {
                                    reporting.completeTask("task_list")
                                }
                            },
                            onToggleGroup = { viewModel.toggleCollapsed(it) },
                        )
                    }
                }
            },
            detailPane = {
                navigator.currentDestination?.contentKey?.let { taskId ->
                    TaskDetailPlaceholder(taskId)
                }
            },
        )

        TopAppBar(
            modifier = Modifier
                .onSizeChanged { size ->
                    topBarHeightPx = size.height.toFloat()
                    topBarHeight = with(density) { size.height.toDp() }
                }
                .graphicsLayer {
                    translationY = topBarOffsetPx
                },
            title = {
                Text(
                    text = state.filter.title.ifEmpty { "Tasks" },
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
            onMenuClick = {
                scope.launch {
                    if (materialDrawerState.isOpen) {
                        materialDrawerState.close()
                    } else {
                        materialDrawerState.open()
                    }
                }
            },
            onSearchClick = { /* TODO: search */ },
            onSortClick = { showSortSheet = true },
            onMoreClick = { /* TODO: more options */ },
            onAddClick = { /* TODO: create task */ },
            scrollBehavior = floatingToolbarScrollBehavior,
            fabContainerColor = Color(themeColor.primaryColor),
            fabContentColor = Color(themeColor.onPrimaryColor),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .platformNavigationBarsPadding()
                .padding(16.dp),
        )
    }

    if (showSortSheet) {
        var showGroupPicker by remember { mutableStateOf(false) }
        var showSortPicker by remember { mutableStateOf(false) }
        var showCompletedPicker by remember { mutableStateOf(false) }
        var showSubtaskPicker by remember { mutableStateOf(false) }
        ModalBottomSheet(
            onDismissRequest = { showSortSheet = false },
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

    } // ModalNavigationDrawer
}

@Composable
private fun TaskList(
    tasks: SectionedDataSource,
    headerFormatter: HeaderFormatter,
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    topPadding: Dp = 0.dp,
    onTaskClick: (TaskContainer) -> Unit,
    onCompleteTask: (TaskContainer, Boolean) -> Unit,
    onToggleGroup: (Long) -> Unit = {},
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
                onClick = { onTaskClick(task) },
                onToggleComplete = { onCompleteTask(task, !task.isCompleted) },
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
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
) {
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
            .padding(start = (20 * task.indent).dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
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
        Text(
            text = task.title ?: "",
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
            color = if (task.isCompleted) {
                MaterialTheme.colorScheme.outline
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FloatingToolbar(
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
        IconButton(onClick = onMenuClick) {
            Icon(Icons.Outlined.Menu, contentDescription = "Menu")
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

@Composable
private fun TaskDetailPlaceholder(taskId: Long) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Task #$taskId",
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}
