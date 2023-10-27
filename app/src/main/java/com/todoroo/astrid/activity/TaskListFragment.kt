/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.speech.RecognizerIntent
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ShareCompat
import androidx.core.content.IntentCompat
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.composethemeadapter.MdcTheme
import com.google.android.material.snackbar.Snackbar
import com.todoroo.andlib.sql.Join
import com.todoroo.andlib.sql.QueryTemplate
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.adapter.TaskAdapter
import com.todoroo.astrid.adapter.TaskAdapterProvider
import com.todoroo.astrid.api.AstridApiConstants.EXTRAS_OLD_DUE_DATE
import com.todoroo.astrid.api.AstridApiConstants.EXTRAS_TASK_ID
import com.todoroo.astrid.api.AstridOrderingFilter
import com.todoroo.astrid.api.CaldavFilter
import com.todoroo.astrid.api.CustomFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.FilterImpl
import com.todoroo.astrid.api.GtasksFilter
import com.todoroo.astrid.api.TagFilter
import com.todoroo.astrid.core.BuiltInFilterExposer
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.repeats.RepeatTaskHelper
import com.todoroo.astrid.service.TaskCompleter
import com.todoroo.astrid.service.TaskCreator
import com.todoroo.astrid.service.TaskDeleter
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
import org.tasks.activities.FilterSettingsActivity
import org.tasks.activities.GoogleTaskListSettingsActivity
import org.tasks.activities.PlaceSettingsActivity
import org.tasks.activities.TagSettingsActivity
import org.tasks.analytics.Firebase
import org.tasks.caldav.BaseCaldavCalendarSettingsActivity
import org.tasks.compose.SubscriptionNagBanner
import org.tasks.compose.collectAsStateLifecycleAware
import org.tasks.data.CaldavDao
import org.tasks.data.Tag
import org.tasks.data.TagDataDao
import org.tasks.data.TaskContainer
import org.tasks.databinding.FragmentTaskListBinding
import org.tasks.db.SuspendDbUtils.chunkedMap
import org.tasks.dialogs.DateTimePicker.Companion.newDateTimePicker
import org.tasks.dialogs.DialogBuilder
import org.tasks.dialogs.FilterPicker.Companion.newFilterPicker
import org.tasks.dialogs.FilterPicker.Companion.setFilterPickerResultListener
import org.tasks.dialogs.PriorityPicker.Companion.newPriorityPicker
import org.tasks.dialogs.SortSettingsActivity
import org.tasks.extensions.Context.openUri
import org.tasks.extensions.Context.toast
import org.tasks.extensions.Fragment.safeStartActivityForResult
import org.tasks.extensions.formatNumber
import org.tasks.extensions.setOnQueryTextListener
import org.tasks.filters.PlaceFilter
import org.tasks.intents.TaskIntents
import org.tasks.preferences.Device
import org.tasks.preferences.Preferences
import org.tasks.sync.SyncAdapters
import org.tasks.tags.TagPickerActivity
import org.tasks.tasklist.DragAndDropRecyclerAdapter
import org.tasks.tasklist.TaskViewHolder
import org.tasks.tasklist.ViewHolderFactory
import org.tasks.themes.ColorProvider
import org.tasks.themes.ThemeColor
import org.tasks.ui.TaskEditEvent
import org.tasks.ui.TaskEditEventBus
import org.tasks.ui.TaskListEvent
import org.tasks.ui.TaskListEventBus
import org.tasks.ui.TaskListViewModel
import org.tasks.ui.TaskListViewModel.Companion.createSearchQuery
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject
import kotlin.math.max

@AndroidEntryPoint
class TaskListFragment : Fragment(), OnRefreshListener, Toolbar.OnMenuItemClickListener,
        MenuItem.OnActionExpandListener, SearchView.OnQueryTextListener, ActionMode.Callback,
        TaskViewHolder.ViewHolderCallbacks {
    private val repeatConfirmationReceiver = RepeatConfirmationReceiver()

    @Inject lateinit var syncAdapters: SyncAdapters
    @Inject lateinit var taskDeleter: TaskDeleter
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
    @Inject lateinit var locale: Locale
    @Inject lateinit var firebase: Firebase
    @Inject lateinit var repeatTaskHelper: RepeatTaskHelper
    @Inject lateinit var taskListEventBus: TaskListEventBus
    @Inject lateinit var taskEditEventBus: TaskEditEventBus
    
    private val listViewModel: TaskListViewModel by viewModels()
    private lateinit var taskAdapter: TaskAdapter
    private var recyclerAdapter: DragAndDropRecyclerAdapter? = null
    private lateinit var filter: Filter
    private lateinit var search: MenuItem
    private var mode: ActionMode? = null
    lateinit var themeColor: ThemeColor
    private lateinit var callbacks: TaskListFragmentCallbackHandler
    private lateinit var binding: FragmentTaskListBinding
    private val onBackPressed = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            search.collapseActionView()
        }
    }

    private val sortRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let { data ->
                    if (data.getBooleanExtra(SortSettingsActivity.EXTRA_FORCE_RELOAD, false)) {
                        activity?.recreate()
                    }
                    if (data.getBooleanExtra(SortSettingsActivity.EXTRA_CHANGED_GROUP, false)) {
                        taskAdapter.clearCollapsed()
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
                ACTION_DELETED ->
                    openFilter(BuiltInFilterExposer.getMyTasksFilter(resources))
                ACTION_RELOAD ->
                    IntentCompat.getParcelableExtra(data, MainActivity.OPEN_FILTER, Filter::class.java)?.let {
                        openFilter(it)
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

    override fun onAttach(context: Context) {
        super.onAttach(requireContext())
        callbacks = activity as TaskListFragmentCallbackHandler
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val selectedTaskIds: List<Long> = taskAdapter.getSelected()
        outState.putLongArray(EXTRA_SELECTED_TASK_IDS, selectedTaskIds.toLongArray())
        outState.putLongArray(EXTRA_COLLAPSED, taskAdapter.getCollapsed().toLongArray())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        taskListEventBus
            .onEach(this::process)
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requireActivity().onBackPressedDispatcher.addCallback(requireActivity(), onBackPressed)
    }

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTaskListBinding.inflate(inflater, container, false)
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
        taskAdapter.setCollapsed(savedInstanceState?.getLongArray(EXTRA_COLLAPSED))
        listViewModel.setFilter(filter)
        (recyclerView.itemAnimator as DefaultItemAnimator).supportsChangeAnimations = false
        recyclerView.layoutManager = LinearLayoutManager(context)
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                listViewModel.state.collect {
                    submitList(it.tasks)
                    if (it.tasks.isEmpty()) {
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
        setupRefresh(swipeRefreshLayout)
        setupRefresh(emptyRefreshLayout)
        binding.toolbar.title = filter.title
        binding.appbarlayout.addOnOffsetChangedListener { _, verticalOffset ->
            if (verticalOffset == 0 && binding.bottomAppBar.isScrolledDown) {
                binding.bottomAppBar.performShow()
            }
        }
        val toolbar = if (preferences.isTopAppBar) {
            binding.bottomAppBar.isVisible = false
            with (binding.fab) {
                layoutParams = (layoutParams as CoordinatorLayout.LayoutParams).apply {
                    setMargins(resources.getDimensionPixelSize(R.dimen.keyline_first))
                    anchorId = View.NO_ID
                    gravity = Gravity.BOTTOM or Gravity.END
                }
            }
            binding.toolbar.setNavigationIcon(R.drawable.ic_outline_menu_24px)
            binding.toolbar
        } else {
            themeColor.apply(binding.bottomAppBar)
            binding.bottomAppBar.isVisible = true
            binding.toolbar.navigationIcon = null
            binding.bottomAppBar
        }
        if (!preferences.getBoolean(R.string.p_app_bar_collapse, true)) {
            binding.bottomAppBar.hideOnScroll = false
            (binding.toolbar.layoutParams as AppBarLayout.LayoutParams).scrollFlags = 0
        }
        toolbar.setOnMenuItemClickListener(this)
        toolbar.setNavigationOnClickListener { callbacks.onNavigationIconClicked() }
        setupMenu(toolbar)
        childFragmentManager.setFilterPickerResultListener(this) {
            val selected = taskAdapter.getSelected()
            lifecycleScope.launch {
                taskMover.move(selected, it)
            }
            finishActionMode()
        }
        binding.banner.setContent {
            val showBanner = listViewModel.state.collectAsStateLifecycleAware().value.begForSubscription
            MdcTheme {
                SubscriptionNagBanner(
                    visible = showBanner,
                    subscribe = { listViewModel.dismissBanner(clickedPurchase = true) },
                    dismiss = { listViewModel.dismissBanner(clickedPurchase = false) },
                )
            }
        }
        return binding.root
    }

    private fun submitList(tasks: List<TaskContainer>) {
        if (recyclerAdapter !is DragAndDropRecyclerAdapter) {
            setAdapter(
                    DragAndDropRecyclerAdapter(
                            taskAdapter, binding.bodyStandard.recyclerView, viewHolderFactory, this, tasks, preferences))
        } else {
            recyclerAdapter?.submitList(tasks)
        }
    }

    private fun setAdapter(adapter: DragAndDropRecyclerAdapter) {
        recyclerAdapter = adapter
        binding.bodyStandard.recyclerView.adapter = adapter
        taskAdapter.setDataSource(adapter)
    }

    private fun setupMenu(appBar: Toolbar) {
        val menu = appBar.menu
        menu.clear()
        if (filter is PlaceFilter) {
            appBar.inflateMenu(R.menu.menu_location_actions)
        }
        appBar.inflateMenu(R.menu.menu_task_list_fragment_bottom)
        when (filter) {
            is CaldavFilter -> R.menu.menu_caldav_list_fragment
            is CustomFilter -> R.menu.menu_custom_filter
            is GtasksFilter -> R.menu.menu_gtasks_list_fragment
            is TagFilter -> R.menu.menu_tag_view_fragment
            is PlaceFilter -> R.menu.menu_location_list_fragment
            else -> null
        }?.let {
            appBar.inflateMenu(it)
        }
        if (appBar is BottomAppBar) {
            menu.removeItem(R.id.menu_search)
        }
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
        search = binding.toolbar.menu.findItem(R.id.menu_search).also {
            it.setOnActionExpandListener(this)
            it.setOnQueryTextListener(this)
        }
        menu.findItem(R.id.menu_clear_completed).isVisible = filter.isWritable
    }

    private fun openFilter(filter: Filter?) {
        if (filter == null) {
            startActivity(TaskIntents.getTaskListByIdIntent(context, null))
        } else {
            startActivity(TaskIntents.getTaskListIntent(context, filter))
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
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
                dialogBuilder
                        .newDialog(R.string.clear_completed_tasks_confirmation)
                        .setPositiveButton(R.string.ok) { _, _ -> clearCompleted() }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
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
                val calendar = (filter as CaldavFilter).calendar
                lifecycleScope.launch {
                    val account = caldavDao.getAccountByUuid(calendar.account!!)
                    listSettingsRequest.launch(
                        Intent(activity, account!!.listSettingsClass())
                            .putExtra(BaseCaldavCalendarSettingsActivity.EXTRA_CALDAV_ACCOUNT, account)
                            .putExtra(BaseCaldavCalendarSettingsActivity.EXTRA_CALDAV_CALENDAR, calendar)
                    )
                }
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
            R.id.menu_gtasks_list_settings -> {
                listSettingsRequest.launch(
                    Intent(activity, GoogleTaskListSettingsActivity::class.java)
                            .putExtra(GoogleTaskListSettingsActivity.EXTRA_STORE_DATA, (filter as GtasksFilter).list)
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
                (filter as PlaceFilter).openMap(context)
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

    private fun clearCompleted() = lifecycleScope.launch {
        val count = taskDeleter.clearCompleted(filter)
        context?.toast(R.string.delete_multiple_tasks_confirmation, locale.formatNumber(count))
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
                colorProvider.getPriorityColor(0, true),
                colorProvider.getPriorityColor(1, true),
                colorProvider.getPriorityColor(2, true),
                colorProvider.getPriorityColor(3, true))
    }

    override fun onResume() {
        super.onResume()
        localBroadcastManager.registerTaskCompletedReceiver(repeatConfirmationReceiver)
    }

    private fun makeSnackbar(@StringRes res: Int, vararg args: Any?): Snackbar? {
        return makeSnackbar(getString(res, *args))
    }

    private fun makeSnackbar(text: String): Snackbar? = activity?.let {
        Snackbar.make(binding.taskListCoordinator, text, 4000)
                .setAnchorView(R.id.fab)
                .setTextColor(it.getColor(R.color.snackbar_text_color))
                .setActionTextColor(it.getColor(R.color.snackbar_action_color))
                .apply {
                    view.setBackgroundColor(it.getColor(R.color.snackbar_background))
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

    private suspend fun onTaskDelete(task: Task) {
        taskEditEventBus.emit(TaskEditEvent.Discard(task.id))
        timerPlugin.stopTimer(task)
        taskAdapter.onTaskDeleted(task)
        loadTaskListContent()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            VOICE_RECOGNITION_REQUEST_CODE -> if (resultCode == Activity.RESULT_OK) {
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
        callbacks.onTaskListItemClicked(task)
    }

    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
        onBackPressed.isEnabled = true
        listViewModel.setSearchQuery("")
        if (preferences.isTopAppBar) {
            binding.toolbar.menu.forEach { it.isVisible = false }
        }
        return true
    }

    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
        onBackPressed.isEnabled = false
        listViewModel.setSearchQuery(null)
        if (preferences.isTopAppBar) {
            setupMenu(binding.toolbar)
        }
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        openFilter(requireContext().createSearchQuery(query.trim()))
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
                            newPriorityPicker(preferences.getBoolean(R.string.p_desaturate_colors, false), it)
                                .show(parentFragmentManager, FRAG_TAG_PRIORITY_PICKER)
                        }
                }
                true
            }
            R.id.move_tasks -> {
                lifecycleScope.launch {
                    val singleFilter = taskMover.getSingleFilter(selected)
                    newFilterPicker(singleFilter, true)
                        .show(childFragmentManager, FRAG_TAG_REMOTE_LIST_PICKER)
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
                                        .join(Join.left(Tag.TABLE, Tag.TASK.eq(Task.ID)))
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

    interface TaskListFragmentCallbackHandler {
        suspend fun onTaskListItemClicked(task: Task?)
        fun onNavigationIconClicked()
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
            taskDeleter.markDeleted(tasks)
        }
        result.forEach { onTaskDelete(it) }
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

    fun clearCollapsed() = taskAdapter.clearCollapsed()

    override fun onCompletedTask(task: TaskContainer, newState: Boolean) {
        if (task.isReadOnly) {
            return
        }
        lifecycleScope.launch {
            taskCompleter.setComplete(task.task, newState)
            taskAdapter.onCompletedTask(task, newState)
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
            val context = activity
            context?.startActivity(TaskIntents.getTaskListIntent(context, filter))
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
                        ?.let { taskDao.fetch(it) }
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
                    val text = getString(
                        R.string.repeat_snackbar,
                        task.title,
                        DateUtilities.getRelativeDateTime(
                            context, task.dueDate, locale, FormatStyle.LONG, true
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
        private const val EXTRA_COLLAPSED = "extra_collapsed"
        private const val VOICE_RECOGNITION_REQUEST_CODE = 1234
        private const val EXTRA_FILTER = "extra_filter"
        private const val FRAG_TAG_REMOTE_LIST_PICKER = "frag_tag_remote_list_picker"
        private const val FRAG_TAG_DATE_TIME_PICKER = "frag_tag_date_time_picker"
        private const val FRAG_TAG_PRIORITY_PICKER = "frag_tag_priority_picker"
        private const val REQUEST_TAG_TASKS = 10106
        fun newTaskListFragment(context: Context, filter: Filter?): TaskListFragment {
            val fragment = TaskListFragment()
            val bundle = Bundle()
            bundle.putParcelable(
                    EXTRA_FILTER,
                    filter ?: BuiltInFilterExposer.getMyTasksFilter(context.resources))
            fragment.arguments = bundle
            return fragment
        }
    }
}
