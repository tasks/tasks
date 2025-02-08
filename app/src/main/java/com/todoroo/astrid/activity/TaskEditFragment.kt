package com.todoroo.astrid.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.compose.AndroidFragment
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.todoroo.andlib.utility.AndroidUtilities.atLeastOreoMR1
import com.todoroo.astrid.files.FilesControlSet
import com.todoroo.astrid.repeats.RepeatControlSet
import com.todoroo.astrid.tags.TagsControlSet
import com.todoroo.astrid.timers.TimerControlSet
import com.todoroo.astrid.ui.ReminderControlSet
import com.todoroo.astrid.ui.StartDateControlSet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tasks.R
import org.tasks.calendars.CalendarPicker
import org.tasks.compose.FilterSelectionActivity.Companion.launch
import org.tasks.compose.FilterSelectionActivity.Companion.registerForListPickerResult
import org.tasks.compose.edit.DescriptionRow
import org.tasks.compose.edit.DueDateRow
import org.tasks.compose.edit.InfoRow
import org.tasks.compose.edit.ListRow
import org.tasks.compose.edit.PriorityRow
import org.tasks.compose.edit.TaskEditScreen
import org.tasks.compose.edit.TitleRow
import org.tasks.data.dao.UserActivityDao
import org.tasks.data.entity.Task
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.dialogs.DateTimePicker
import org.tasks.dialogs.DialogBuilder
import org.tasks.dialogs.Linkify
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.extensions.hideKeyboard
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
import org.tasks.ui.TaskEditViewModel.Companion.TAG_CREATION
import org.tasks.ui.TaskEditViewModel.Companion.TAG_DESCRIPTION
import org.tasks.ui.TaskEditViewModel.Companion.TAG_DUE_DATE
import org.tasks.ui.TaskEditViewModel.Companion.TAG_LIST
import org.tasks.ui.TaskEditViewModel.Companion.TAG_PRIORITY
import org.tasks.ui.TaskEditViewModel.Companion.TAG_TITLE
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class TaskEditFragment : Fragment() {
    @Inject lateinit var userActivityDao: UserActivityDao
    @Inject lateinit var notificationManager: NotificationManager
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var linkify: Linkify
    @Inject lateinit var markdownProvider: MarkdownProvider
    @Inject lateinit var taskEditEventBus: TaskEditEventBus
    @Inject lateinit var locale: Locale
    @Inject lateinit var chipProvider: ChipProvider
    @Inject lateinit var playServices: PlayServices
    @Inject lateinit var theme: Theme

    private val editViewModel: TaskEditViewModel by viewModels()
    private val beastMode =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            activity?.recreate()
        }
    private val listPickerLauncher = registerForListPickerResult { filter ->
        editViewModel.setList(filter)
    }

    val task: Task?
        get() = BundleCompat.getParcelable(requireArguments(), EXTRA_TASK, Task::class.java)

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ) = content {
        LaunchedEffect(Unit) {
            if (atLeastOreoMR1()) {
                activity?.setShowWhenLocked(preferences.showEditScreenWithoutUnlock)
            }
        }
        TasksTheme(theme = theme.themeBase.index,) {
            val viewState = editViewModel.viewState.collectAsStateWithLifecycle().value
            LaunchedEffect(viewState.isNew) {
                if (!viewState.isNew) {
                    notificationManager.cancel(viewState.task.id)
                }
            }
            TaskEditScreen(
                viewState = viewState,
                comments = userActivityDao
                    .watchComments(viewState.task.uuid)
                    .collectAsStateWithLifecycle(emptyList())
                    .value,
                save = { lifecycleScope.launch { save() } },
                discard = { discardButtonClick() },
                onBackPressed = {
                    if (viewState.backButtonSavesTask) {
                        lifecycleScope.launch {
                            save()
                        }
                    } else {
                        discardButtonClick()
                    }
                },
                delete = { deleteButtonClick() },
                openBeastModeSettings = {
                    editViewModel.hideBeastModeHint(click = true)
                    beastMode.launch(Intent(context, BeastModePreferences::class.java))
                },
                dismissBeastMode = { editViewModel.hideBeastModeHint(click = false) },
                deleteComment = {
                    lifecycleScope.launch {
                        userActivityDao.delete(it)
                    }
                },
            ) { tag ->
                val context = LocalContext.current
                when (tag) {
                    TAG_TITLE ->
                        TitleRow(
                            viewState = viewState,
                            requestFocus = viewState.showKeyboard,
                        )

                    TAG_DUE_DATE -> DueDateRow()
                    TAG_PRIORITY ->
                        PriorityRow(
                            priority = viewState.task.priority,
                            onChangePriority = { editViewModel.setPriority(it) },
                        )

                    TAG_DESCRIPTION ->
                        DescriptionRow(
                            text = viewState.task.notes,
                            onChanged = { text -> editViewModel.setDescription(text.toString().trim { it <= ' ' }) },
                            linkify = if (viewState.linkify) linkify else null,
                            markdownProvider = markdownProvider,
                        )

                    TAG_LIST ->
                        ListRow(
                            list = viewState.list,
                            colorProvider = { chipProvider.getColor(it) },
                            onClick = {
                                listPickerLauncher.launch(
                                    context = context,
                                    selectedFilter = viewState.list,
                                    listsOnly = true
                                )
                            }
                        )

                    TAG_CREATION ->
                        InfoRow(
                            creationDate = viewState.task.creationDate,
                            modificationDate = viewState.task.modificationDate,
                            completionDate = viewState.task.completionDate,
                            locale = locale,
                        )

                    CalendarControlSet.TAG -> AndroidFragment<CalendarControlSet>()
                    StartDateControlSet.TAG -> AndroidFragment<StartDateControlSet>()
                    ReminderControlSet.TAG -> AndroidFragment<ReminderControlSet>()
                    LocationControlSet.TAG -> AndroidFragment<LocationControlSet>()
                    FilesControlSet.TAG -> AndroidFragment<FilesControlSet>()
                    TimerControlSet.TAG -> AndroidFragment<TimerControlSet>()
                    TagsControlSet.TAG -> AndroidFragment<TagsControlSet>()
                    RepeatControlSet.TAG -> AndroidFragment<RepeatControlSet>()
                    SubtaskControlSet.TAG -> AndroidFragment<SubtaskControlSet>()
                    else -> throw IllegalArgumentException("Unknown row: $tag")
                }
            }
        }
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
                if (event.id == editViewModel.viewState.value.task.id) {
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
                    editViewModel.setCalendar(data!!.getStringExtra(CalendarPicker.EXTRA_CALENDAR_ID))
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    @Composable
    private fun TitleRow(
        viewState: TaskEditViewModel.ViewState,
        requestFocus: Boolean,
    ) {
        TitleRow(
            text = viewState.task.title,
            onChanged = { text -> editViewModel.setTitle(text.toString().trim { it <= ' ' }) },
            linkify = if (viewState.linkify) linkify else null,
            markdownProvider = markdownProvider,
            isCompleted = viewState.isCompleted,
            isRecurring = viewState.task.isRecurring,
            priority = viewState.task.priority,
            onComplete = {
                if (viewState.isCompleted) {
                    editViewModel.setComplete(false)
                } else {
                    editViewModel.setComplete(true)
                    lifecycleScope.launch {
                        save()
                    }
                }
            },
            requestFocus = requestFocus,
            multiline = viewState.multilineTitle,
        )
    }

    @Composable
    private fun DueDateRow() {
        val viewState = editViewModel.viewState.collectAsStateWithLifecycle().value
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
                        hideNoDate = viewState.task.isRecurring,
                    )
                    .show(parentFragmentManager, FRAG_TAG_DATE_PICKER)
            }
        )
    }

    companion object {
        const val EXTRA_TASK = "extra_task"

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