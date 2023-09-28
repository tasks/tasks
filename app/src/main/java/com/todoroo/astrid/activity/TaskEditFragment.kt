/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.os.BundleCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.Behavior.DragCallback
import com.google.android.material.composethemeadapter.MdcTheme
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
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
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.analytics.Firebase
import org.tasks.calendars.CalendarPicker
import org.tasks.compose.BeastModeBanner
import org.tasks.compose.collectAsStateLifecycleAware
import org.tasks.compose.edit.CommentsRow
import org.tasks.compose.edit.DescriptionRow
import org.tasks.compose.edit.DueDateRow
import org.tasks.compose.edit.InfoRow
import org.tasks.compose.edit.ListRow
import org.tasks.compose.edit.PriorityRow
import org.tasks.data.Alarm
import org.tasks.data.Location
import org.tasks.data.TagData
import org.tasks.data.UserActivityDao
import org.tasks.databinding.FragmentTaskEditBinding
import org.tasks.databinding.TaskEditCalendarBinding
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
import org.tasks.dialogs.FilterPicker.Companion.newFilterPicker
import org.tasks.dialogs.FilterPicker.Companion.setFilterPickerResultListener
import org.tasks.dialogs.Linkify
import org.tasks.extensions.hideKeyboard
import org.tasks.files.FileHelper
import org.tasks.fragments.TaskEditControlSetFragmentManager
import org.tasks.fragments.TaskEditControlSetFragmentManager.Companion.TAG_CREATION
import org.tasks.fragments.TaskEditControlSetFragmentManager.Companion.TAG_DESCRIPTION
import org.tasks.fragments.TaskEditControlSetFragmentManager.Companion.TAG_DUE_DATE
import org.tasks.fragments.TaskEditControlSetFragmentManager.Companion.TAG_LIST
import org.tasks.fragments.TaskEditControlSetFragmentManager.Companion.TAG_PRIORITY
import org.tasks.markdown.MarkdownProvider
import org.tasks.notifications.NotificationManager
import org.tasks.play.PlayServices
import org.tasks.preferences.Preferences
import org.tasks.ui.CalendarControlSet
import org.tasks.ui.ChipProvider
import org.tasks.ui.LocationControlSet
import org.tasks.ui.SubtaskControlSet
import org.tasks.ui.TaskEditEvent
import org.tasks.ui.TaskEditEventBus
import org.tasks.ui.TaskEditViewModel
import org.tasks.ui.TaskEditViewModel.Companion.stripCarriageReturns
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class TaskEditFragment : Fragment(), Toolbar.OnMenuItemClickListener {
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

    val editViewModel: TaskEditViewModel by viewModels()
    lateinit var binding: FragmentTaskEditBinding
    private var showKeyboard = false
    private val beastMode =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            activity?.recreate()
        }

    val task: Task?
        get() = BundleCompat.getParcelable(requireArguments(), EXTRA_TASK, Task::class.java)

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        requireActivity().onBackPressedDispatcher.addCallback(owner = viewLifecycleOwner) {
            if (preferences.backButtonSavesTask()) {
                lifecycleScope.launch {
                    save()
                }
            } else {
                discardButtonClick()
            }
        }

        binding = FragmentTaskEditBinding.inflate(inflater)
        val view: View = binding.root
        val model = editViewModel.task
        val toolbar = binding.toolbar
        toolbar.navigationIcon = AppCompatResources.getDrawable(
            context,
            if (editViewModel.isReadOnly)
                R.drawable.ic_outline_arrow_back_24px
            else
                R.drawable.ic_outline_save_24px
        )
        toolbar.setNavigationOnClickListener {
            lifecycleScope.launch {
                save()
            }
        }
        val backButtonSavesTask = preferences.backButtonSavesTask()
        toolbar.setNavigationContentDescription(
            when {
                editViewModel.isReadOnly -> R.string.back
                backButtonSavesTask -> R.string.discard
                else -> R.string.save
            }
        )
        toolbar.inflateMenu(R.menu.menu_task_edit_fragment)
        val menu = toolbar.menu
        val delete = menu.findItem(R.id.menu_delete)
        delete.isVisible = !model.isNew && editViewModel.isWritable
        delete.setShowAsAction(
                if (backButtonSavesTask) MenuItem.SHOW_AS_ACTION_NEVER else MenuItem.SHOW_AS_ACTION_IF_ROOM)
        val discard = menu.findItem(R.id.menu_discard)
        discard.isVisible = backButtonSavesTask && editViewModel.isWritable
        discard.setShowAsAction(
                if (model.isNew) MenuItem.SHOW_AS_ACTION_IF_ROOM else MenuItem.SHOW_AS_ACTION_NEVER)
        if (savedInstanceState == null) {
            showKeyboard = model.isNew && isNullOrEmpty(model.title)
        }
        val params = binding.appbarlayout.layoutParams as CoordinatorLayout.LayoutParams
        params.behavior = AppBarLayout.Behavior()
        val behavior = params.behavior as AppBarLayout.Behavior?
        behavior!!.setDragCallback(object : DragCallback() {
            override fun canDrag(appBarLayout: AppBarLayout): Boolean {
                return false
            }
        })
        toolbar.setOnMenuItemClickListener(this)
        val title = binding.title
        val textWatcher = markdownProvider.markdown(preferences.linkify).textWatcher(title)
        title.addTextChangedListener(
            onTextChanged = { _, _, _, _ ->
                editViewModel.title = title.text.toString().trim { it <= ' ' }
            },
            afterTextChanged = {
                textWatcher?.invoke(it)
            }
        )
        title.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                lifecycleScope.launch {
                    save()
                }
                true
            } else false
        }
        title.setText(model.title)
        title.setHorizontallyScrolling(false)
        title.maxLines = 5
        if (editViewModel.isReadOnly) {
            title.isFocusable = false
            title.isFocusableInTouchMode = false
            title.isCursorVisible = false
        }
        if (
            model.isNew ||
            preferences.getBoolean(R.string.p_hide_check_button, false) ||
            editViewModel.isReadOnly
        ) {
            binding.fab.visibility = View.INVISIBLE
        } else if (editViewModel.completed) {
            title.paintFlags = title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            binding.fab.setImageResource(R.drawable.ic_outline_check_box_outline_blank_24px)
        }
        binding.fab.setOnClickListener {
            if (editViewModel.completed) {
                editViewModel.completed = false
                title.paintFlags = title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.fab.setImageResource(R.drawable.ic_outline_check_box_24px)
            } else {
                editViewModel.completed = true
                lifecycleScope.launch {
                    save()
                }
            }
        }
        binding.appbarlayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            if (verticalOffset == 0) {
                title.visibility = View.VISIBLE
                binding.collapsingtoolbarlayout.isTitleEnabled = false
            } else if (abs(verticalOffset) < appBarLayout.totalScrollRange) {
                title.visibility = View.INVISIBLE
                binding.collapsingtoolbarlayout.title = title.text
                binding.collapsingtoolbarlayout.isTitleEnabled = true
            }
        }
        if (!model.isNew) {
            lifecycleScope.launch {
                notificationManager.cancel(model.id)
            }
            if (preferences.linkify) {
                linkify.linkify(title)
            }
        }
        binding.composeView.setContent {
            MdcTheme {
                Column(modifier = Modifier.gesturesDisabled(editViewModel.isReadOnly)) {
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
                            Divider(modifier = Modifier.fillMaxWidth())
                        }
                    }
                    if (preferences.getBoolean(R.string.p_show_task_edit_comments, false)) {
                        Comments()
                    }
                }
            }
        }
        childFragmentManager.setFilterPickerResultListener(this) { filter ->
            editViewModel.selectedList.update { filter }
        }
        return view
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

    @OptIn(ExperimentalAnimationApi::class)
    private fun showBeastModeHint() {
        binding.banner.setContent {
            var visible by rememberSaveable { mutableStateOf(true) }
            val context = LocalContext.current
            MdcTheme {
                BeastModeBanner(
                    visible,
                    showSettings = {
                        visible = false
                        preferences.shownBeastModeHint = true
                        beastMode.launch(Intent(context, BeastModePreferences::class.java))
                        firebase.logEvent(R.string.event_banner_beast, R.string.param_click to true)
                    },
                    dismiss = {
                        visible = false
                        preferences.shownBeastModeHint = true
                        firebase.logEvent(R.string.event_banner_beast, R.string.param_click to false)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (showKeyboard) {
            binding.title.requestFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.title, InputMethodManager.SHOW_IMPLICIT)
        }
        if (!preferences.shownBeastModeHint) {
            showBeastModeHint()
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        activity?.hideKeyboard()
        if (item.itemId == R.id.menu_delete) {
            deleteButtonClick()
            return true
        } else if (item.itemId == R.id.menu_discard) {
            discardButtonClick()
            return true
        }
        return false
    }

    suspend fun save(remove: Boolean = true) {
        editViewModel.save(remove)
        activity?.let { playServices.requestReview(it) }
    }

    private fun discardButtonClick() {
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
    private fun DueDateRow() {
        val dueDate = editViewModel.dueDate.collectAsStateLifecycleAware().value
        DueDateRow(
            dueDate = if (dueDate == 0L) {
                null
            } else {
                DateUtilities.getRelativeDateTime(
                    LocalContext.current,
                    dueDate,
                    locale,
                    FormatStyle.FULL,
                    preferences.alwaysDisplayFullDate,
                    false
                )
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
            priority = editViewModel.priority.collectAsStateLifecycleAware().value,
            onChangePriority = { editViewModel.priority.value = it },
            desaturate = preferences.desaturateDarkMode,
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
        val list = editViewModel.selectedList.collectAsStateLifecycleAware().value
        ListRow(
            list = list,
            colorProvider = { chipProvider.getColor(it) },
            onClick = {
                newFilterPicker(list, true)
                    .show(
                        childFragmentManager,
                        FRAG_TAG_GOOGLE_TASK_LIST_SELECTION
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
                .collectAsStateLifecycleAware(emptyList())
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

        private const val FRAG_TAG_GOOGLE_TASK_LIST_SELECTION =
            "frag_tag_google_task_list_selection"
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