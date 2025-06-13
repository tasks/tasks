/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Parcelable
import android.speech.RecognizerIntent
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LAYOUT_DIRECTION_LTR
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.app.ShareCompat
import androidx.core.content.IntentCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.material.snackbar.Snackbar
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.astrid.adapter.TaskAdapter
import com.todoroo.astrid.adapter.TaskAdapterProvider
import com.todoroo.astrid.api.AstridApiConstants.EXTRAS_OLD_DUE_DATE
import com.todoroo.astrid.api.AstridApiConstants.EXTRAS_TASK_ID
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.repeats.RepeatTaskHelper
import com.todoroo.astrid.service.TaskCompleter
import com.todoroo.astrid.service.TaskCreator
import com.todoroo.astrid.service.TaskDuplicator
import com.todoroo.astrid.service.TaskMover
import com.todoroo.astrid.timers.TimerPlugin
import com.todoroo.astrid.utility.Flags
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.ShortcutManager
import org.tasks.TasksApplication
import org.tasks.activities.FilterSettingsActivity
import org.tasks.activities.PlaceSettingsActivity
import org.tasks.activities.TagSettingsActivity
import org.tasks.analytics.Firebase
import org.tasks.billing.PurchaseActivity
import org.tasks.caldav.BaseCaldavCalendarSettingsActivity
import org.tasks.compose.AlarmsDisabledBanner
import org.tasks.compose.AppUpdatedBanner
import org.tasks.compose.FilterSelectionActivity.Companion.launch
import org.tasks.compose.FilterSelectionActivity.Companion.registerForListPickerResult
import org.tasks.compose.NotificationsDisabledBanner
import org.tasks.compose.QuietHoursBanner
import org.tasks.compose.SubscriptionNagBanner
import org.tasks.compose.SyncWarningGoogleTasks
import org.tasks.compose.SyncWarningMicrosoft
import org.tasks.data.TaskContainer
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.db.Database
import org.tasks.data.db.SuspendDbUtils.chunkedMap
import org.tasks.data.entity.Task
import org.tasks.data.listSettingsClass
import org.tasks.data.open
import org.tasks.data.sql.QueryTemplate
import org.tasks.databinding.FragmentTaskListBinding
import org.tasks.dialogs.DateTimePicker.Companion.newDateTimePicker
import org.tasks.dialogs.DialogBuilder
import org.tasks.dialogs.PriorityPicker.Companion.newPriorityPicker
import org.tasks.dialogs.SortSettingsActivity
import org.tasks.dialogs.WhatsNewDialog
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.extensions.Context.openAppNotificationSettings
import org.tasks.extensions.Context.openReminderSettings
import org.tasks.extensions.Context.openUri
import org.tasks.extensions.Context.toast
import org.tasks.extensions.Fragment.safeStartActivityForResult
import org.tasks.extensions.hideKeyboard
import org.tasks.extensions.setOnQueryTextListener
import org.tasks.filters.AstridOrderingFilter
import org.tasks.filters.CaldavFilter
import org.tasks.filters.CustomFilter
import org.tasks.filters.Filter
import org.tasks.filters.FilterImpl
import org.tasks.filters.MyTasksFilter
import org.tasks.filters.PlaceFilter
import org.tasks.filters.SearchFilter
import org.tasks.filters.TagFilter
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.getRelativeDateTime
import org.tasks.markdown.MarkdownProvider
import org.tasks.preferences.Device
import org.tasks.preferences.MainPreferences
import org.tasks.preferences.Preferences
import org.tasks.preferences.ResourceResolver.getData
import org.tasks.scheduling.NotificationSchedulerIntentService
import org.tasks.sync.SyncAdapters
import org.tasks.tags.TagPickerActivity
import org.tasks.tasklist.DragAndDropRecyclerAdapter
import org.tasks.tasklist.SectionedDataSource
import org.tasks.tasklist.TaskViewHolder
import org.tasks.tasklist.TasksResults
import org.tasks.tasklist.ViewHolderFactory
import org.tasks.themes.ColorProvider
import org.tasks.themes.TasksTheme
import org.tasks.themes.Theme
import org.tasks.themes.ThemeColor
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.ui.Banner
import org.tasks.ui.TaskListEvent
import org.tasks.ui.TaskListEventBus
import org.tasks.ui.TaskListViewModel
import org.tasks.ui.TaskListViewModel.Companion.createSearchQuery
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import kotlin.math.max

@AndroidEntryPoint
class TaskListFragment : Fragment(), OnRefreshListener, Toolbar.OnMenuItemClickListener,
        MenuItem.OnActionExpandListener, SearchView.OnQueryTextListener, ActionMode.Callback,
        TaskViewHolder.ViewHolderCallbacks {
    private val repeatConfirmationReceiver = RepeatConfirmationReceiver()

    @Inject lateinit var syncAdapters: SyncAdapters
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var taskCreator: TaskCreator
    @Inject lateinit var timerPlugin: TimerPlugin
    @Inject lateinit var viewHolderFactory: ViewHolderFactory
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var device: Device
    @Inject lateinit var taskMover: TaskMover
    @Inject lateinit var taskAdapterProvider: TaskAdapterProvider
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var taskDuplicator: TaskDuplicator
    @Inject lateinit var tagDataDao: TagDataDao
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var defaultThemeColor: ThemeColor
    @Inject lateinit var colorProvider: ColorProvider
    @Inject lateinit var shortcutManager: ShortcutManager
    @Inject lateinit var taskCompleter: TaskCompleter
    @Inject lateinit var firebase: Firebase
    @Inject lateinit var repeatTaskHelper: RepeatTaskHelper
    @Inject lateinit var taskListEventBus: TaskListEventBus
    @Inject lateinit var database: Database
    @Inject lateinit var markdown: MarkdownProvider
    @Inject lateinit var theme: Theme

    private val listViewModel: TaskListViewModel by viewModels()
    private val mainViewModel: MainActivityViewModel by activityViewModels()
    private lateinit var taskAdapter: TaskAdapter
    private var recyclerAdapter: DragAndDropRecyclerAdapter? = null
    private lateinit var filter: Filter
    private lateinit var search: MenuItem
    private var mode: ActionMode? = null
    lateinit var themeColor: ThemeColor
    private var onClickMenu: () -> Unit = {}
    private lateinit var binding: FragmentTaskListBinding
    private var windowInsets: PaddingValues? = null
    private val listPickerLauncher = registerForListPickerResult {
        val selected = taskAdapter.getSelected()
        lifecycleScope.launch {
            taskMover.move(selected, it)
        }
        finishActionMode()
    }
    private val settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        (activity as? MainActivity)?.restartActivity()
    }

    private val sortRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let { data ->
                    if (data.getBooleanExtra(SortSettingsActivity.EXTRA_FORCE_RELOAD, false)) {
                        (activity as? MainActivity)?.restartActivity()
                    }
                    if (data.getBooleanExtra(SortSettingsActivity.EXTRA_CHANGED_GROUP, false)) {
                        listViewModel.clearCollapsed()
                    }
                    listViewModel.invalidate()
                }
            }
        }

    private val listSettingsRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val data = result.data ?: return@registerForActivityResult
            when (data.action) {
                ACTION_DELETED -> lifecycleScope.launch {
                    mainViewModel.setFilter(MyTasksFilter.create())
                }
                ACTION_RELOAD ->
                    IntentCompat.getParcelableExtra(data, MainActivity.OPEN_FILTER, Filter::class.java)?.let {
                        mainViewModel.setFilter(it)
                    }
            }
        }

    private fun process(event: TaskListEvent) = when (event) {
        is TaskListEvent.TaskCreated ->
            onTaskCreated(event.uuid)
        is TaskListEvent.CalendarEventCreated ->
            makeSnackbar(R.string.calendar_event_created, event.title)
                ?.setAction(R.string.action_open) { context?.openUri(event.uri) }
                ?.show()
    }

    override fun onRefresh() {
        syncAdapters.sync(true)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            val longArray = savedInstanceState.getLongArray(EXTRA_SELECTED_TASK_IDS)
            if (longArray?.isNotEmpty() == true) {
                taskAdapter.setSelected(longArray.toList())
                startActionMode()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val selectedTaskIds: List<Long> = taskAdapter.getSelected()
        outState.putLongArray(EXTRA_SELECTED_TASK_IDS, selectedTaskIds.toLongArray())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        taskListEventBus
            .onEach(this::process)
            .launchIn(viewLifecycleOwner.lifecycleScope)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if ((mainViewModel.state.value.filter as? SearchFilter)?.query?.isNotBlank() == true) {
                    lifecycleScope.launch {
                        mainViewModel.resetFilter()
                    }
                    if (search.isActionViewExpanded) {
                        search.collapseActionView()
                    }
                    Timber.d("Filtro resettato")
                } else {
                    isEnabled = false // Disabilita il callback per consentire il comportamento predefinito
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    fun setNavigationClickListener(onClick: () -> Unit) {
        onClickMenu = onClick
    }

    @SuppressLint("PrivateResource")
    fun applyInsets(windowInsets: PaddingValues) {
        if (this::binding.isInitialized) {
            applyInsetsInternal(windowInsets)
        } else {
            this.windowInsets = windowInsets
        }
    }

    private fun applyInsetsInternal(windowInsets: PaddingValues) {
        val density = resources.displayMetrics.density
        val viewLayoutDir = view?.layoutDirection ?: resources.configuration.layoutDirection
        val composeLayoutDirection =
            if (viewLayoutDir == View.LAYOUT_DIRECTION_RTL) LayoutDirection.Rtl else LayoutDirection.Ltr
        val topInset = (windowInsets.calculateTopPadding().value * density).toInt()
        val bottomInset = (windowInsets.calculateBottomPadding().value * density).toInt()
        val (startInset, endInset) =
            (windowInsets.calculateStartPadding(composeLayoutDirection).value * density).toInt().let {
            if (viewLayoutDir == LAYOUT_DIRECTION_LTR) it to 0 else 0 to it
        }
        with(binding.toolbar) {
            val actionBarHeight = TypedValue.complexToDimensionPixelSize(
                getData(requireContext(), android.R.attr.actionBarSize),
                resources.displayMetrics
            )
            val params = layoutParams
            params.height = actionBarHeight + topInset
            layoutParams = params
            updatePadding(top = topInset)
        }
        binding.taskListCoordinator.updatePadding(
            left = startInset,
            right = endInset,
        )
        binding.bottomAppBar.updatePadding(bottom = bottomInset)
        (binding.fab.layoutParams as MarginLayoutParams).bottomMargin = bottomInset / 2
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTaskListBinding.inflate(inflater, container, false)
        windowInsets?.let { applyInsetsInternal(it) }
        filter = getFilter()
        val swipeRefreshLayout: SwipeRefreshLayout
        val emptyRefreshLayout: SwipeRefreshLayout
        val recyclerView: RecyclerView
        with (binding) {
            swipeRefreshLayout = bodyStandard.swipeLayout
            emptyRefreshLayout = bodyEmpty.swipeLayoutEmpty
            recyclerView = bodyStandard.recyclerView
            fab.setOnClickListener { createNewTask() }
            fab.isVisible = filter.isWritable
        }
        themeColor = if (filter.tint != 0) colorProvider.getThemeColor(filter.tint, true) else defaultThemeColor
        (filter as? AstridOrderingFilter)?.filterOverride = null

        // set up list adapters
        taskAdapter = taskAdapterProvider.createTaskAdapter(filter)
        listViewModel.setFilter(filter)
        (recyclerView.itemAnimator as DefaultItemAnimator).supportsChangeAnimations = false
        recyclerView.layoutManager = LinearLayoutManager(context)
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                listViewModel.updateBannerState()
                listViewModel.state.collect {
                    if (it.tasks is TasksResults.Results) {
                        submitList(it.tasks.tasks)
                        if (it.tasks.tasks.isEmpty()) {
                            swipeRefreshLayout.visibility = View.GONE
                            emptyRefreshLayout.visibility = View.VISIBLE
                        } else {
                            swipeRefreshLayout.visibility = View.VISIBLE
                            emptyRefreshLayout.visibility = View.GONE
                        }
                        swipeRefreshLayout.isRefreshing = it.syncOngoing
                        emptyRefreshLayout.isRefreshing = it.syncOngoing
                    }
                }
            }
        }
        setupRefresh(swipeRefreshLayout)
        setupRefresh(emptyRefreshLayout)
        binding.toolbar.title = filter.title
        binding.toolbar.setTitleTextAppearance(requireContext(), com.google.android.material.R.style.TextAppearance_Material3_HeadlineSmall)
        binding.toolbar.setTitleTextColor(themeColor.primaryColor)
        binding.appbarlayout.addOnOffsetChangedListener { _, verticalOffset ->
            if (verticalOffset == 0 && binding.bottomAppBar.isScrolledDown) {
                binding.bottomAppBar.performShow()
            }
        }
        with (binding.fab) {
            backgroundTintList = ColorStateList.valueOf(themeColor.primaryColor)
            imageTintList = ColorStateList.valueOf(themeColor.colorOnPrimary)
        }
        with (binding.bottomAppBar) {
            setOnMenuItemClickListener(this@TaskListFragment)
            setNavigationOnClickListener {
                activity?.hideKeyboard()
                onClickMenu()
            }
        }
        setupToolbarMenu()
        setupBottomAppBarMenu()
        binding.banner.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        binding.banner.setContent {
            val context = LocalContext.current
            val mainActivityState = mainViewModel.state.collectAsStateWithLifecycle().value
            val state = listViewModel.state.collectAsStateWithLifecycle().value
            BackHandler(enabled = state.searchQuery != null && mainActivityState.task == null) {
                Timber.d("onBackPressed")
                if (search.isActionViewExpanded) {
                    search.collapseActionView()
                }
            }
            TasksTheme(
                theme = theme.themeBase.index,
                primary = theme.themeColor.primaryColor,
            ) {
                val notificationPermissions = if (AndroidUtilities.atLeastTiramisu()) {
                    rememberPermissionState(
                        Manifest.permission.POST_NOTIFICATIONS,
                        onPermissionResult = { success ->
                            if (success) {
                                NotificationSchedulerIntentService.enqueueWork(context)
                                listViewModel.dismissBanner(tookAction = true)
                            }
                        }
                    )
                } else {
                    null
                }

                AnimatedVisibility(
                    visible = state.banner != null,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    when (state.banner) {
                        is Banner.NotificationsDisabled ->
                            NotificationsDisabledBanner(
                                settings = {
                                    if (notificationPermissions?.status?.shouldShowRationale == true) {
                                        context.openAppNotificationSettings()
                                    } else {
                                        notificationPermissions?.launchPermissionRequest()
                                    }
                                },
                                dismiss = { listViewModel.dismissBanner() },
                            )

                        Banner.AlarmsDisabled ->
                            AlarmsDisabledBanner(
                                settings = { context.openReminderSettings() },
                                dismiss = { listViewModel.dismissBanner() },
                            )

                        Banner.BegForMoney ->
                            SubscriptionNagBanner(
                                subscribe = {
                                    listViewModel.dismissBanner(tookAction = true)
                                    if (TasksApplication.IS_GOOGLE_PLAY) {
                                        context.startActivity(
                                            Intent(
                                                context,
                                                PurchaseActivity::class.java
                                            )
                                        )
                                    } else {
                                        preferences.lastSubscribeRequest = currentTimeMillis()
                                        context.openUri(R.string.url_donate)
                                    }
                                },
                                dismiss = { listViewModel.dismissBanner() },
                            )

                        Banner.QuietHoursEnabled ->
                            QuietHoursBanner(
                                showSettings = {
                                    listViewModel.dismissBanner()
                                    context.startActivity(
                                        Intent(
                                            context,
                                            MainPreferences::class.java
                                        )
                                    )
                                },
                                dismiss = { listViewModel.dismissBanner() },
                            )

                        Banner.WarnGoogleTasks ->
                            SyncWarningGoogleTasks(
                                moreInfo = {
                                    listViewModel.dismissBanner()
                                    context.openUri(R.string.url_google_tasks)
                                },
                                dismiss = { listViewModel.dismissBanner() },
                            )

                        Banner.WarnMicrosoft ->
                            SyncWarningMicrosoft(
                                moreInfo = {
                                    listViewModel.dismissBanner()
                                    context.openUri(R.string.url_microsoft)
                                },
                                dismiss = { listViewModel.dismissBanner() },
                            )

                        Banner.AppUpdated ->
                            AppUpdatedBanner(
                                whatsNew = {
                                    val fragmentManager = parentFragmentManager
                                    if (fragmentManager.findFragmentByTag(FRAG_TAG_WHATS_NEW) == null) {
                                        WhatsNewDialog().show(parentFragmentManager, FRAG_TAG_WHATS_NEW)
                                    }
                                    listViewModel.dismissBanner()
                                },
                                dismiss = { listViewModel.dismissBanner() },
                            )

                        null -> {}
                    }
                }
            }
        }
        ViewCompat.requestApplyInsets(binding.toolbar)
        return binding.root
    }

    private fun submitList(tasks: SectionedDataSource) {
        if (recyclerAdapter !is DragAndDropRecyclerAdapter) {
            setAdapter(
                    DragAndDropRecyclerAdapter(
                        adapter = taskAdapter,
                        recyclerView = binding.bodyStandard.recyclerView,
                        viewHolderFactory = viewHolderFactory,
                        taskList = this,
                        tasks = tasks,
                        preferences = preferences,
                        toggleCollapsed = { listViewModel.toggleCollapsed(it) },
                    )
            )
        } else {
            recyclerAdapter?.submitList(tasks)
        }
    }

    private fun setAdapter(adapter: DragAndDropRecyclerAdapter) {
        recyclerAdapter = adapter
        binding.bodyStandard.recyclerView.adapter = adapter
        taskAdapter.setDataSource(adapter)
    }

    private fun setupToolbarMenu() {
        val toolbar = binding.toolbar
        toolbar.setOnMenuItemClickListener(this)
        toolbar.overflowIcon = getDrawable(requireContext(), R.drawable.ic_outline_settings_24px)
        val menu = toolbar.menu
        menu.clear()
        toolbar.inflateMenu(R.menu.menu_task_list_fragment_top)
        when (filter) {
            is CaldavFilter -> R.menu.menu_caldav_list_fragment
            is CustomFilter -> R.menu.menu_custom_filter
            is TagFilter -> R.menu.menu_tag_view_fragment
            is PlaceFilter -> R.menu.menu_location_list_fragment
            else -> null
        }?.let {
            toolbar.inflateMenu(it)
        }
        search = binding.toolbar.menu.findItem(R.id.menu_search).apply {
            setOnActionExpandListener(this@TaskListFragment)
            isVisible = false
        }
    }

    private fun setupBottomAppBarMenu() {
        val appBar = binding.bottomAppBar
        val menu = appBar.menu
        menu.clear()
        if (filter is PlaceFilter) {
            appBar.inflateMenu(R.menu.menu_location_actions)
        }
        appBar.inflateMenu(R.menu.menu_task_list_fragment_bottom)
        val hidden = menu.findItem(R.id.menu_show_unstarted)
        val completed = menu.findItem(R.id.menu_show_completed)
        if (!taskAdapter.supportsHiddenTasks() || !filter.supportsHiddenTasks()) {
            completed.isChecked = true
            completed.isEnabled = false
            hidden.isChecked = true
            hidden.isEnabled = false
        } else {
            hidden.isChecked = preferences.showHidden
            completed.isChecked = preferences.showCompleted
        }
        val sortMenu = menu.findItem(R.id.menu_sort)
        if (!filter.supportsSorting()) {
            sortMenu.isEnabled = false
            sortMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        if (!filter.supportsSubtasks() || taskAdapter.supportsAstridSorting()) {
            menu.findItem(R.id.menu_collapse_subtasks).isVisible = false
            menu.findItem(R.id.menu_expand_subtasks).isVisible = false
        }
        menu.findItem(R.id.menu_voice_add).isVisible = device.voiceInputAvailable() && filter.isWritable
        menu.findItem(R.id.menu_clear_completed).isVisible = filter.isWritable
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        Timber.d("onMenuItemClick($item)")
        return when (item.itemId) {
            R.id.menu_settings -> {
                settingsLauncher.launch(Intent(context, MainPreferences::class.java))
                true
            }
            R.id.menu_search -> {
                if (!search.isActionViewExpanded) {
                    search.isVisible = true
                    search.expandActionView()
                }
                true
            }
            R.id.menu_voice_add -> {
                safeStartActivityForResult(
                        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            )
                            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                            putExtra(
                                    RecognizerIntent.EXTRA_PROMPT,
                                    getString(R.string.voice_create_prompt)
                            )
                        },
                        VOICE_RECOGNITION_REQUEST_CODE
                )
                true
            }
            R.id.menu_sort -> {
                sortRequest.launch(
                    SortSettingsActivity.getIntent(
                        requireActivity(),
                        filter.supportsManualSort(),
                        filter is AstridOrderingFilter && preferences.isAstridSortEnabled,
                    )
                )
                true
            }
            R.id.menu_show_unstarted -> {
                item.isChecked = !item.isChecked
                preferences.showHidden = item.isChecked
                loadTaskListContent()
                true
            }
            R.id.menu_show_completed -> {
                item.isChecked = !item.isChecked
                preferences.showCompleted = item.isChecked
                loadTaskListContent()
                true
            }
            R.id.menu_clear_completed -> {
                lifecycleScope.launch {
                    val tasks = listViewModel.getTasksToClear()
                    val countString = requireContext().resources.getQuantityString(R.plurals.Ntasks, tasks.size, tasks.size)
                    if (tasks.isEmpty()) {
                        context?.toast(R.string.delete_multiple_tasks_confirmation, countString)
                    } else {
                        dialogBuilder
                            .newDialog(R.string.clear_completed_tasks_confirmation)
                            .setMessage(R.string.delete_tasks_warning, countString)
                            .setPositiveButton(R.string.ok) { _, _ ->
                                lifecycleScope.launch {
                                    listViewModel.markDeleted(tasks)
                                    context?.toast(
                                        R.string.delete_multiple_tasks_confirmation,
                                        countString
                                    )
                                }
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                    }
                }
                true
            }
            R.id.menu_filter_settings -> {
                listSettingsRequest.launch(
                    Intent(activity, FilterSettingsActivity::class.java)
                        .putExtra(FilterSettingsActivity.TOKEN_FILTER, filter)
                )
                true
            }
            R.id.menu_caldav_list_fragment -> {
                val filter = filter as? CaldavFilter ?: return false
                listSettingsRequest.launch(
                    Intent(activity, filter.account.listSettingsClass())
                        .putExtra(BaseCaldavCalendarSettingsActivity.EXTRA_CALDAV_ACCOUNT, filter.account)
                        .putExtra(BaseCaldavCalendarSettingsActivity.EXTRA_CALDAV_CALENDAR, filter.calendar)
                )
                true
            }
            R.id.menu_location_settings -> {
                val place = (filter as PlaceFilter).place
                listSettingsRequest.launch(
                    Intent(activity, PlaceSettingsActivity::class.java)
                            .putExtra(PlaceSettingsActivity.EXTRA_PLACE, place as Parcelable)
                )
                true
            }
            R.id.menu_tag_settings -> {
                listSettingsRequest.launch(
                    Intent(activity, TagSettingsActivity::class.java)
                            .putExtra(TagSettingsActivity.EXTRA_TAG_DATA, (filter as TagFilter).tagData)
                )
                true
            }
            R.id.menu_expand_subtasks -> {
                lifecycleScope.launch {
                    taskDao.setCollapsed(preferences, filter, false)
                    localBroadcastManager.broadcastRefresh()
                }
                true
            }
            R.id.menu_collapse_subtasks -> {
                lifecycleScope.launch {
                    taskDao.setCollapsed(preferences, filter, true)
                    localBroadcastManager.broadcastRefresh()
                }
                true
            }
            R.id.menu_open_map -> {
                (filter as PlaceFilter).place.open(context)
                true
            }
            R.id.menu_share -> {
                lifecycleScope.launch {
                    send(taskDao.fetchTasks(preferences, filter))
                }
                true
            }
            else -> onOptionsItemSelected(item)
        }
    }

    private fun createNewTask() {
        lifecycleScope.launch {
            shortcutManager.reportShortcutUsed(ShortcutManager.SHORTCUT_NEW_TASK)
            onTaskListItemClicked(addTask(""))
            firebase.addTask("fab")
        }
    }

    private suspend fun addTask(title: String): Task {
        return taskCreator.createWithValues(filter, title)
    }

    private fun setupRefresh(layout: SwipeRefreshLayout) {
        layout.setOnRefreshListener(this)
        layout.setColorSchemeColors(
                colorProvider.getPriorityColor(0),
                colorProvider.getPriorityColor(1),
                colorProvider.getPriorityColor(2),
                colorProvider.getPriorityColor(3))
    }

    override fun onResume() {
        super.onResume()
        listViewModel.invalidate()
        localBroadcastManager.registerTaskCompletedReceiver(repeatConfirmationReceiver)
    }

    private fun makeSnackbar(@StringRes res: Int, vararg args: Any?): Snackbar? {
        return makeSnackbar(getString(res, *args))
    }

    private fun makeSnackbar(text: String): Snackbar? = activity?.let {
        Snackbar.make(binding.taskListCoordinator, text, 4000)
                .setAnchorView(R.id.fab)
                .setBackgroundTint(it.getColor(R.color.dialog_background))
                .setTextColor(it.getColor(R.color.text_primary))
                .setActionTextColor(themeColor.primaryColor)
            .apply {
                val offset = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    16f,
                    context.resources.displayMetrics
                )
                view.translationY = -offset
            }
    }

    override fun onPause() {
        super.onPause()
        localBroadcastManager.unregisterReceiver(repeatConfirmationReceiver)
    }

    fun loadTaskListContent() {
        listViewModel.invalidate()
    }

    fun getFilter(): Filter {
        return requireArguments().getParcelable(EXTRA_FILTER)!!
    }

    private fun onTaskCreated(tasks: List<Task>) {
        for (task in tasks) {
            onTaskCreated(task.uuid)
        }
        syncAdapters.sync()
        loadTaskListContent()
    }

    private fun onTaskCreated(uuid: String) {
        lifecycleScope.launch {
            taskAdapter.onTaskCreated(uuid)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            VOICE_RECOGNITION_REQUEST_CODE -> if (resultCode == RESULT_OK) {
                lifecycleScope.launch {
                    val match: List<String>? = data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    if (!match.isNullOrEmpty() && match[0].isNotEmpty()) {
                        var recognizedSpeech = match[0]
                        recognizedSpeech = (recognizedSpeech.substring(0, 1)
                            .uppercase(Locale.getDefault())
                                + recognizedSpeech.substring(1).lowercase(Locale.getDefault()))
                        onTaskListItemClicked(addTask(recognizedSpeech))
                        firebase.addTask("voice")
                    }
                }
            }
            REQUEST_TAG_TASKS -> if (resultCode == RESULT_OK) {
                lifecycleScope.launch {
                    val modified = tagDataDao.applyTags(
                            taskDao
                                .fetch(data!!.getSerializableExtra(TagPickerActivity.EXTRA_TASKS) as ArrayList<Long>)
                                .filterNot { it.readOnly },
                            data.getParcelableArrayListExtra(TagPickerActivity.EXTRA_PARTIALLY_SELECTED)!!,
                            data.getParcelableArrayListExtra(TagPickerActivity.EXTRA_SELECTED)!!
                    )
                    taskDao.touch(modified)
                }
                finishActionMode()
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun onTaskListItemClicked(task: Task?) = lifecycleScope.launch {
        mainViewModel.setTask(task)
    }

    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
        search.setOnQueryTextListener(this)
        listViewModel.setSearchQuery("")
        return true
    }

    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
        search.isVisible = false
        search.setOnQueryTextListener(null)
        listViewModel.setFilter(filter)
        listViewModel.setSearchQuery(null)
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        mainViewModel.setFilter(requireContext().createSearchQuery(query.trim()))
        search.collapseActionView()
        return true
    }

    override fun onQueryTextChange(query: String): Boolean {
        listViewModel.setSearchQuery(query)
        return true
    }

    override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
        val inflater = actionMode.menuInflater
        inflater.inflate(R.menu.menu_multi_select, menu)
        if (filter.isReadOnly) {
            listOf(R.id.edit_tags, R.id.move_tasks, R.id.reschedule, R.id.copy_tasks, R.id.delete)
                .forEach { menu.findItem(it).isVisible = false }
        }
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return false
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        val selected = taskAdapter.getSelected()
        return when (item.itemId) {
            R.id.edit_tags -> {
                lifecycleScope.launch {
                    val tags = tagDataDao.getTagSelections(selected)
                    val intent = Intent(context, TagPickerActivity::class.java)
                    intent.putExtra(TagPickerActivity.EXTRA_TASKS, selected)
                    intent.putParcelableArrayListExtra(
                            TagPickerActivity.EXTRA_PARTIALLY_SELECTED,
                            ArrayList(tagDataDao.getByUuid(tags.first!!)))
                    intent.putParcelableArrayListExtra(
                            TagPickerActivity.EXTRA_SELECTED, ArrayList(tagDataDao.getByUuid(tags.second!!)))
                    startActivityForResult(intent, REQUEST_TAG_TASKS)
                }
                true
            }
            R.id.edit_priority -> {
                lifecycleScope.launch {
                    taskDao
                        .fetch(selected)
                        .filterNot { it.readOnly }
                        .takeIf { it.isNotEmpty() }
                        ?.let {
                            newPriorityPicker(it)
                                .show(parentFragmentManager, FRAG_TAG_PRIORITY_PICKER)
                        }
                }
                true
            }
            R.id.move_tasks -> {
                lifecycleScope.launch {
                    val singleFilter = taskMover.getSingleFilter(selected)
                    listPickerLauncher.launch(
                        context = requireActivity(),
                        selectedFilter = singleFilter,
                        listsOnly = true,
                    )
                }
                true
            }
            R.id.reschedule -> {
                lifecycleScope.launch {
                    taskDao
                            .fetch(selected)
                            .filterNot { it.readOnly }
                            .takeIf { it.isNotEmpty() }
                            ?.let {
                                newDateTimePicker(
                                        preferences.getBoolean(R.string.p_auto_dismiss_datetime_list_screen, false),
                                        *it.toTypedArray())
                                        .show(parentFragmentManager, FRAG_TAG_DATE_TIME_PICKER)
                            }
                }
                finishActionMode()
                true
            }
            R.id.menu_select_all -> {
                lifecycleScope.launch {
                    setSelected(taskDao.fetchTasks(preferences, filter)
                        .map(TaskContainer::id))
                }
                true
            }
            R.id.menu_share -> {
                lifecycleScope.launch {
                    selected
                        .chunkedMap {
                            taskDao.fetchTasks(
                                preferences,
                                FilterImpl(
                                    sql = QueryTemplate()
                                        .where(Task.ID.`in`(it))
                                        .toString()
                                )
                            )
                        }
                        .let { send(it) }
                }
                true
            }
            R.id.delete -> {
                dialogBuilder
                        .newDialog(R.string.delete_selected_tasks)
                        .setPositiveButton(
                                R.string.ok) { _, _ -> deleteSelectedItems(selected) }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                true
            }
            R.id.copy_tasks -> {
                dialogBuilder
                        .newDialog(R.string.copy_selected_tasks)
                        .setPositiveButton(
                                R.string.ok) { _, _ -> copySelectedItems(selected) }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                true
            }
            else -> false
        }
    }

    private fun send(tasks: List<TaskContainer>) {
        val output = tasks.joinToString("\n") { t -> Task
            "${(if (t.isCompleted) "☑" else "☐").padStart(1 + t.indent * 3, ' ')} ${t.title}"
        }
        val intent = ShareCompat
            .IntentBuilder(requireContext())
            .setType("text/plain")
            .setSubject(filter.title)
            .setText(output)
            .createChooserIntent()
        startActivity(intent)
        finishActionMode()
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        this.mode = null
        if (taskAdapter.numSelected > 0) {
            taskAdapter.clearSelections()
            recyclerAdapter?.notifyDataSetChanged()
        }
    }

    private fun showDateTimePicker(task: TaskContainer) {
        val fragmentManager = parentFragmentManager
        if (fragmentManager.findFragmentByTag(FRAG_TAG_DATE_TIME_PICKER) == null) {
            newDateTimePicker(
                preferences.getBoolean(R.string.p_auto_dismiss_datetime_list_screen, false),
                task.task)
                .show(fragmentManager, FRAG_TAG_DATE_TIME_PICKER)
        }
    }

    val isActionModeActive: Boolean
        get() = mode != null

    fun startActionMode() {
        if (mode == null) {
            mode = (activity as AppCompatActivity).startSupportActionMode(this)
            updateModeTitle()
            Flags.set(Flags.TLFP_NO_INTERCEPT_TOUCH)
        }
    }

    fun finishActionMode() {
        mode?.finish()
    }

    fun updateModeTitle() {
        if (mode != null) {
            val count = max(1, taskAdapter.numSelected)
            mode!!.title = count.toString()
        }
    }

    private fun deleteSelectedItems(tasks: List<Long>) = lifecycleScope.launch {
        finishActionMode()

        val result = withContext(NonCancellable) {
            listViewModel.markDeleted(tasks)
        }
        result.forEach {
            timerPlugin.stopTimer(it)
            taskAdapter.onTaskDeleted(it)
        }
        loadTaskListContent()
        if (tasks.contains(mainViewModel.state.value.task?.id)) {
            mainViewModel.setTask(null)
        }
        makeSnackbar(R.string.delete_multiple_tasks_confirmation, result.size.toString())?.show()
    }

    private fun setSelected(tasks: List<Long>) {
        taskAdapter.setSelected(tasks)
        updateModeTitle()
        recyclerAdapter?.notifyDataSetChanged()
    }

    private fun copySelectedItems(tasks: List<Long>) = lifecycleScope.launch {
        val duplicates = withContext(NonCancellable) {
            taskDuplicator.duplicate(tasks)
        }
        onTaskCreated(duplicates)
        setSelected(duplicates.map(Task::id))
        makeSnackbar(R.string.copy_multiple_tasks_confirmation, duplicates.size.toString())?.show()
    }

    override fun onCompletedTask(task: TaskContainer, newState: Boolean) {
        if (task.isReadOnly) {
            return
        }
        lifecycleScope.launch {
            taskCompleter.setComplete(task.task, newState)
            firebase.completeTask("task_list")
            taskAdapter.onCompletedTask(task.uuid, newState)
            loadTaskListContent()
        }
    }

    override fun onLinkClicked(vh: TaskViewHolder, url: String) =
        if (isActionModeActive) {
            recyclerAdapter?.toggle(vh)
            true
        } else {
            false
        }

    override fun onClick(taskViewHolder: TaskViewHolder) {
        if (isActionModeActive) {
            recyclerAdapter?.toggle(taskViewHolder)
        } else {
            onTaskListItemClicked(taskViewHolder.task.task)
        }
    }

    override fun onClick(filter: Filter) {
        if (!isActionModeActive) {
            mainViewModel.setFilter(filter)
        }
    }

    override fun onLongPress(taskViewHolder: TaskViewHolder): Boolean {
        if (recyclerAdapter?.dragAndDropEnabled() != true) {
            startActionMode()
        }
        if (isActionModeActive && !taskViewHolder.moving) {
            recyclerAdapter?.toggle(taskViewHolder)
        }
        return true
    }

    override fun onChangeDueDate(task: TaskContainer) {
        if (task.isReadOnly) {
            return
        }
        showDateTimePicker(task)
    }

    override fun toggleSubtasks(task: Long, collapsed: Boolean) {
        lifecycleScope.launch {
            taskDao.setCollapsed(task, collapsed)
        }
    }

    private inner class RepeatConfirmationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            lifecycleScope.launch {
                val tasks =
                    (intent.getSerializableExtra(EXTRAS_TASK_ID) as? ArrayList<Long>)
                        ?.let {
                            Timber.d("Repeating tasks: $it")
                            taskDao.fetch(it)
                        }
                        ?.filterNot { it.readOnly }
                        ?.takeIf { it.isNotEmpty() }
                        ?: return@launch
                val isRecurringCompletion =
                    tasks.size == 1 && tasks.first().let { it.isRecurring && !it.isCompleted }
                val oldDueDate = if (isRecurringCompletion) {
                    intent.getLongExtra(EXTRAS_OLD_DUE_DATE, 0)
                } else {
                    0
                }
                val undoCompletion = View.OnClickListener {
                    lifecycleScope.launch {
                        tasks
                            .partition { it.isRecurring }
                            .let { (recurring, notRecurring) ->
                                recurring.forEach { repeatTaskHelper.undoRepeat(it, oldDueDate) }
                                taskCompleter.setComplete(notRecurring, 0L)
                            }
                    }
                }
                if (isRecurringCompletion) {
                    val task = tasks.first()
                    val title = markdown.markdown(force = true).toMarkdown(task.title)
                    val text = getString(
                        R.string.repeat_snackbar,
                        title,
                        getRelativeDateTime(
                            task.dueDate,
                            context.is24HourFormat,
                            DateStyle.LONG,
                            lowercase = true
                        )
                    )
                    makeSnackbar(text)?.setAction(R.string.DLG_undo, undoCompletion)?.show()
                } else {
                    val text = if (tasks.size == 1) {
                        context.getString(R.string.snackbar_task_completed)
                    } else {
                        context.getString(R.string.snackbar_tasks_completed, tasks.size)
                    }
                    makeSnackbar(text)?.setAction(R.string.DLG_undo, undoCompletion)?.show()
                }
            }
        }
    }

    companion object {
        const val ACTION_RELOAD = "action_reload"
        const val ACTION_DELETED = "action_deleted"
        private const val EXTRA_SELECTED_TASK_IDS = "extra_selected_task_ids"
        private const val VOICE_RECOGNITION_REQUEST_CODE = 1234
        const val EXTRA_FILTER = "extra_filter"
        private const val FRAG_TAG_DATE_TIME_PICKER = "frag_tag_date_time_picker"
        private const val FRAG_TAG_PRIORITY_PICKER = "frag_tag_priority_picker"
        private const val FRAG_TAG_WHATS_NEW = "frag_tag_whats_new"
        private const val REQUEST_TAG_TASKS = 10106
    }
}
