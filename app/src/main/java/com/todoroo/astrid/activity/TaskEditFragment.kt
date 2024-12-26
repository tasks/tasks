package com.todoroo.astrid.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.todoroo.andlib.utility.AndroidUtilities.atLeastOreoMR1
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.files.FilesControlSet
import com.todoroo.astrid.repeats.RepeatControlSet
import com.todoroo.astrid.tags.TagsControlSet
import com.todoroo.astrid.timers.TimerControlSet
import com.todoroo.astrid.ui.ReminderControlSet
import com.todoroo.astrid.ui.StartDateControlSet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.analytics.Firebase
import org.tasks.calendars.CalendarPicker
import org.tasks.compose.BeastModeBanner
import org.tasks.compose.FilterSelectionActivity.Companion.launch
import org.tasks.compose.FilterSelectionActivity.Companion.registerForListPickerResult
import org.tasks.compose.edit.CommentsRow
import org.tasks.compose.edit.DescriptionRow
import org.tasks.compose.edit.DueDateRow
import org.tasks.compose.edit.InfoRow
import org.tasks.compose.edit.ListRow
import org.tasks.compose.edit.PriorityRow
import org.tasks.data.Location
import org.tasks.data.dao.UserActivityDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.databinding.TaskEditCalendarBinding
import org.tasks.databinding.TaskEditCommentBarBinding
import org.tasks.databinding.TaskEditFilesBinding
import org.tasks.databinding.TaskEditLocationBinding
import org.tasks.databinding.TaskEditRemindersBinding
import org.tasks.databinding.TaskEditRepeatBinding
import org.tasks.databinding.TaskEditStartDateBinding
import org.tasks.databinding.TaskEditSubtasksBinding
import org.tasks.databinding.TaskEditTagsBinding
import org.tasks.databinding.TaskEditTimerBinding
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.dialogs.DateTimePicker
import org.tasks.dialogs.DialogBuilder
import org.tasks.dialogs.Linkify
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.extensions.hideKeyboard
import org.tasks.files.FileHelper
import org.tasks.filters.Filter
import org.tasks.fragments.TaskEditControlSetFragmentManager
import org.tasks.fragments.TaskEditControlSetFragmentManager.Companion.TAG_CREATION
import org.tasks.fragments.TaskEditControlSetFragmentManager.Companion.TAG_DESCRIPTION
import org.tasks.fragments.TaskEditControlSetFragmentManager.Companion.TAG_DUE_DATE
import org.tasks.fragments.TaskEditControlSetFragmentManager.Companion.TAG_LIST
import org.tasks.fragments.TaskEditControlSetFragmentManager.Companion.TAG_PRIORITY
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.getRelativeDateTime
import org.tasks.markdown.MarkdownProvider
import org.tasks.notifications.NotificationManager
import org.tasks.play.PlayServices
import org.tasks.preferences.Preferences
import org.tasks.themes.TasksTheme
import org.tasks.themes.Theme
import org.tasks.ui.CalendarControlSet
import org.tasks.ui.ChipProvider
import org.tasks.ui.LocationControlSet
import org.tasks.ui.SubtaskControlSet
import org.tasks.ui.TaskEditEvent
import org.tasks.ui.TaskEditEventBus
import org.tasks.ui.TaskEditViewModel
import org.tasks.ui.TaskEditViewModel.Companion.stripCarriageReturns
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class TaskEditFragment : Fragment() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var userActivityDao: UserActivityDao
    @Inject lateinit var notificationManager: NotificationManager
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var context: Activity
    @Inject lateinit var taskEditControlSetFragmentManager: TaskEditControlSetFragmentManager
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var firebase: Firebase
    @Inject lateinit var linkify: Linkify
    @Inject lateinit var markdownProvider: MarkdownProvider
    @Inject lateinit var taskEditEventBus: TaskEditEventBus
    @Inject lateinit var locale: Locale
    @Inject lateinit var chipProvider: ChipProvider
    @Inject lateinit var playServices: PlayServices
    @Inject lateinit var theme: Theme

    private val editViewModel: TaskEditViewModel by viewModels()
    private var showKeyboard = false
    private val beastMode =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            activity?.recreate()
        }
    private val listPickerLauncher = registerForListPickerResult { filter ->
        editViewModel.selectedList.update { filter }
    }

    val task: Task?
        get() = BundleCompat.getParcelable(requireArguments(), EXTRA_TASK, Task::class.java)

    @OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (atLeastOreoMR1()) {
            activity?.setShowWhenLocked(preferences.showEditScreenWithoutUnlock)
        }
        val model = editViewModel.task
        if (!model.isNew) {
            lifecycleScope.launch {
                notificationManager.cancel(model.id)
            }
        }
        if (savedInstanceState == null) {
            showKeyboard = model.isNew && isNullOrEmpty(model.title)
        }
        val backButtonSavesTask = preferences.backButtonSavesTask()
        val view = ComposeView(context).apply {
            setContent {
                BackHandler {
                    if (backButtonSavesTask) {
                        lifecycleScope.launch {
                            save()
                        }
                    } else {
                        discardButtonClick()
                    }
                }
                TasksTheme(theme = theme.themeBase.index,) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                navigationIcon = {
                                    if (editViewModel.isReadOnly) {
                                        IconButton(onClick = { activity?.onBackPressed() }) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                                contentDescription = stringResource(R.string.back)
                                            )
                                        }
                                    } else {
                                        IconButton(onClick = { lifecycleScope.launch { save() } }) {
                                            Icon(
                                                imageVector = Icons.Outlined.Save,
                                                contentDescription = stringResource(R.string.save)
                                            )
                                        }
                                    }
                                },
                                title = {},
                                actions = {
                                    if (!editViewModel.isWritable) {
                                        return@TopAppBar
                                    }
                                    if (!editViewModel.isNew) {
                                        IconButton(onClick = { deleteButtonClick() }) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = stringResource(R.string.delete_task),
                                            )
                                        }
                                    }
                                    IconButton(onClick = { discardButtonClick() }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Clear,
                                            contentDescription = stringResource(R.string.menu_discard_changes),
                                        )
                                    }
                                },
                            )
                        },
                        bottomBar = {
                            if (preferences.getBoolean(R.string.p_show_task_edit_comments, false)) {
                                AndroidViewBinding(TaskEditCommentBarBinding::inflate)
                            }
                        },
                    ) { paddingValues ->
                        Column(
                            modifier = Modifier
                                .gesturesDisabled(editViewModel.isReadOnly)
                                .padding(paddingValues)
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                        ) {
                            TitleRow(requestFocus = showKeyboard)
                            HorizontalDivider()
                            taskEditControlSetFragmentManager.displayOrder.forEachIndexed { index, tag ->
                                if (index < taskEditControlSetFragmentManager.visibleSize) {
                                    // TODO: remove ui-viewbinding library when these are all migrated
                                    when (taskEditControlSetFragmentManager.controlSetFragments[tag]) {
                                        TAG_DUE_DATE -> DueDateRow()
                                        TAG_PRIORITY -> PriorityRow()
                                        TAG_DESCRIPTION -> DescriptionRow()
                                        TAG_LIST -> ListRow()
                                        TAG_CREATION -> CreationRow()
                                        CalendarControlSet.TAG -> AndroidViewBinding(TaskEditCalendarBinding::inflate)
                                        StartDateControlSet.TAG -> AndroidViewBinding(
                                            TaskEditStartDateBinding::inflate
                                        )
                                        ReminderControlSet.TAG -> AndroidViewBinding(
                                            TaskEditRemindersBinding::inflate
                                        )
                                        LocationControlSet.TAG -> AndroidViewBinding(TaskEditLocationBinding::inflate)
                                        FilesControlSet.TAG -> AndroidViewBinding(TaskEditFilesBinding::inflate)
                                        TimerControlSet.TAG -> AndroidViewBinding(TaskEditTimerBinding::inflate)
                                        TagsControlSet.TAG -> AndroidViewBinding(TaskEditTagsBinding::inflate)
                                        RepeatControlSet.TAG -> AndroidViewBinding(TaskEditRepeatBinding::inflate)
                                        SubtaskControlSet.TAG -> AndroidViewBinding(TaskEditSubtasksBinding::inflate)
                                        else -> throw IllegalArgumentException("Unknown row: $tag")
                                    }
                                    HorizontalDivider()
                                }
                            }
                            if (preferences.getBoolean(R.string.p_show_task_edit_comments, false)) {
                                Comments()
                            }
                            val showBeastModeHint = editViewModel.showBeastModeHint.collectAsStateWithLifecycle().value
                            val context = LocalContext.current
                            BeastModeBanner(
                                showBeastModeHint,
                                showSettings = {
                                    editViewModel.hideBeastModeHint(click = true)
                                    beastMode.launch(Intent(context, BeastModePreferences::class.java))
                                },
                                dismiss = {
                                    editViewModel.hideBeastModeHint(click = false)
                                }
                            )
                        }
                    }
                }
            }
        }
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (atLeastOreoMR1()) {
            activity?.setShowWhenLocked(false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        taskEditEventBus
            .onEach(this::process)
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private suspend fun process(event: TaskEditEvent) {
        when (event) {
            is TaskEditEvent.Discard ->
                if (event.id == editViewModel.task.id) {
                    editViewModel.discard()
                }
        }
    }

    suspend fun save(remove: Boolean = true) {
        editViewModel.save(remove)
        activity?.let { playServices.requestReview(it) }
    }

    private fun discardButtonClick() {
        activity?.hideKeyboard()
        if (editViewModel.hasChanges()) {
           dialogBuilder
                   .newDialog(R.string.discard_confirmation)
                   .setPositiveButton(R.string.keep_editing, null)
                   .setNegativeButton(R.string.discard) { _, _ -> discard() }
                   .show()
       } else {
           discard()
       }
    }

    private fun deleteButtonClick() {
        activity?.hideKeyboard()
        dialogBuilder
                .newDialog(R.string.DLG_delete_this_task_question)
                .setPositiveButton(R.string.ok) { _, _ -> delete() }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    private fun discard() = lifecycleScope.launch {
        editViewModel.discard()
    }

    private fun delete() = lifecycleScope.launch {
        editViewModel.delete()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_DATE -> {
                if (resultCode == Activity.RESULT_OK) {
                    editViewModel.setDueDate(data!!.getLongExtra(DateTimePicker.EXTRA_TIMESTAMP, 0L))
                }
            }
            REQUEST_CODE_PICK_CALENDAR -> {
                if (resultCode == Activity.RESULT_OK) {
                    editViewModel.selectedCalendar.value =
                        data!!.getStringExtra(CalendarPicker.EXTRA_CALENDAR_ID)
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    @Composable
    private fun TitleRow(
        requestFocus: Boolean,
    ) {
        val isComplete = editViewModel.completed.collectAsStateWithLifecycle().value
        val recurrence = editViewModel.recurrence.collectAsStateWithLifecycle().value
        val isRecurring = remember(recurrence) {
            !recurrence.isNullOrBlank()
        }
        org.tasks.compose.edit.TitleRow(
            text = editViewModel.title,
            onChanged = { text -> editViewModel.title = text.toString().trim { it <= ' ' } },
            linkify = if (preferences.linkify) linkify else null,
            markdownProvider = markdownProvider,
            isCompleted = isComplete,
            isRecurring = isRecurring,
            priority = editViewModel.priority.collectAsStateWithLifecycle().value,
            onComplete = {
                if (isComplete) {
                    editViewModel.completed.value = false
                } else {
                    editViewModel.completed.value = true
                    lifecycleScope.launch {
                        save()
                    }
                }
            },
            requestFocus = requestFocus,
        )
    }

    @Composable
    private fun DueDateRow() {
        val dueDate = editViewModel.dueDate.collectAsStateWithLifecycle().value
        val context = LocalContext.current
        DueDateRow(
            dueDate = if (dueDate == 0L) {
                null
            } else {
                runBlocking {
                    getRelativeDateTime(
                        dueDate,
                        context.is24HourFormat,
                        DateStyle.FULL,
                        alwaysDisplayFullDate = preferences.alwaysDisplayFullDate
                    )
                }
            },
            overdue = dueDate.isOverdue,
            onClick = {
                DateTimePicker
                    .newDateTimePicker(
                        target = this@TaskEditFragment,
                        rc = REQUEST_DATE,
                        current = editViewModel.dueDate.value,
                        autoClose = preferences.getBoolean(
                            R.string.p_auto_dismiss_datetime_edit_screen,
                            false
                        ),
                        hideNoDate = editViewModel.recurrence.value?.isNotBlank() == true,
                    )
                    .show(parentFragmentManager, FRAG_TAG_DATE_PICKER)
            }
        )
    }

    @Composable
    private fun PriorityRow() {
        PriorityRow(
            priority = editViewModel.priority.collectAsStateWithLifecycle().value,
            onChangePriority = { editViewModel.priority.value = it },
        )
    }

    @Composable
    private fun DescriptionRow() {
        DescriptionRow(
            text = editViewModel.description.stripCarriageReturns(),
            onChanged = { text -> editViewModel.description = text.toString().trim { it <= ' ' } },
            linkify = if (preferences.linkify) linkify else null,
            markdownProvider = markdownProvider,
        )
    }

    @Composable
    private fun ListRow() {
        val list = editViewModel.selectedList.collectAsStateWithLifecycle().value
        ListRow(
            list = list,
            colorProvider = { chipProvider.getColor(it) },
            onClick = {
                listPickerLauncher.launch(
                    context = context,
                    selectedFilter = list,
                    listsOnly = true
                )
            }
        )
    }

    @Composable
    fun CreationRow() {
        InfoRow(
            creationDate = editViewModel.creationDate,
            modificationDate = editViewModel.modificationDate,
            completionDate = editViewModel.completionDate,
            locale = locale,
        )
    }

    @Composable
    fun Comments() {
        CommentsRow(
            comments = userActivityDao
                .watchComments(editViewModel.task.uuid)
                .collectAsStateWithLifecycle(emptyList())
                .value,
            deleteComment = {
                lifecycleScope.launch {
                    userActivityDao.delete(it)
                }
            },
            openImage = { FileHelper.startActionView(requireActivity(), it) }
        )
    }

    companion object {
        const val EXTRA_TASK = "extra_task"
        const val EXTRA_LIST = "extra_list"
        const val EXTRA_LOCATION = "extra_location"
        const val EXTRA_TAGS = "extra_tags"
        const val EXTRA_ALARMS = "extra_alarms"

        const val FRAG_TAG_CALENDAR_PICKER = "frag_tag_calendar_picker"
        private const val FRAG_TAG_DATE_PICKER = "frag_tag_date_picker"
        const val REQUEST_CODE_PICK_CALENDAR = 70
        private const val REQUEST_DATE = 504

        val Long.isOverdue: Boolean
            get() = if (Task.hasDueTime(this)) {
                newDateTime(this).isBeforeNow
            } else {
                newDateTime(this).endOfDay().isBeforeNow
            }

        fun newTaskEditFragment(
            task: Task,
            list: Filter,
            location: Location?,
            tags: ArrayList<TagData>,
            alarms: ArrayList<Alarm>,
        ): TaskEditFragment {
            val taskEditFragment = TaskEditFragment()
            val arguments = Bundle()
            arguments.putParcelable(EXTRA_TASK, task)
            arguments.putParcelable(EXTRA_LIST, list)
            arguments.putParcelable(EXTRA_LOCATION, location)
            arguments.putParcelableArrayList(EXTRA_TAGS, tags)
            arguments.putParcelableArrayList(EXTRA_ALARMS, alarms)
            taskEditFragment.arguments = arguments
            return taskEditFragment
        }

        fun Modifier.gesturesDisabled(disabled: Boolean = true) =
            if (disabled) {
                pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(pass = PointerEventPass.Initial)
                                .changes
                                .filter { it.position == it.previousPosition }
                                .forEach { it.consume() }
                        }
                    }
                }
            } else {
                this
            }
    }
}