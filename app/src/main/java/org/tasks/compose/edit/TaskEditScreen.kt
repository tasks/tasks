package org.tasks.compose.edit

import android.content.Intent
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.IntentCompat.getParcelableExtra
import androidx.fragment.compose.AndroidFragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.todoroo.andlib.utility.AndroidUtilities.atLeastOreoMR1
import com.todoroo.astrid.activity.BeastModePreferences
import com.todoroo.astrid.files.FilesControlSet
import com.todoroo.astrid.repeats.RepeatControlSet
import com.todoroo.astrid.tags.TagsControlSet
import com.todoroo.astrid.timers.TimerControlSet
import com.todoroo.astrid.ui.ReminderControlSet
import com.todoroo.astrid.ui.StartDateControlSet
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.compose.BeastModeBanner
import org.tasks.compose.FilterSelectionActivity.Companion.EXTRA_FILTER
import org.tasks.compose.FilterSelectionActivity.Companion.launch
import org.tasks.data.entity.UserActivity
import org.tasks.dialogs.Linkify
import org.tasks.extensions.Context.findActivity
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.files.FileHelper
import org.tasks.filters.CaldavFilter
import org.tasks.fragments.CommentBarFragment
import org.tasks.kmp.org.tasks.extensions.gesturesDisabled
import org.tasks.kmp.org.tasks.taskedit.TaskEditViewState
import org.tasks.markdown.MarkdownProvider
import org.tasks.themes.TasksTheme
import org.tasks.ui.CalendarControlSet
import org.tasks.ui.LocationControlSet
import org.tasks.ui.SubtaskControlSet
import org.tasks.ui.TaskEditViewModel
import org.tasks.ui.TaskEditViewModel.Companion.TAG_CREATION
import org.tasks.ui.TaskEditViewModel.Companion.TAG_DESCRIPTION
import org.tasks.ui.TaskEditViewModel.Companion.TAG_DUE_DATE
import org.tasks.ui.TaskEditViewModel.Companion.TAG_LIST
import org.tasks.ui.TaskEditViewModel.Companion.TAG_PRIORITY
import org.tasks.ui.TaskEditViewModel.Companion.TAG_TITLE
import org.tasks.utility.copyToClipboard
import timber.log.Timber
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    editViewModel: TaskEditViewModel,
    viewState: TaskEditViewState,
    comments: List<UserActivity>,
    save: () -> Unit,
    discard: () -> Unit,
    delete: () -> Unit,
    dismissBeastMode: () -> Unit,
    deleteComment: (UserActivity) -> Unit,
    onClickDueDate: () -> Unit,
    markdownProvider: MarkdownProvider,
    linkify: Linkify?,
    locale: Locale,
    colorProvider: (Int) -> Int,
) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        if (atLeastOreoMR1() && viewState.showEditScreenWithoutUnlock) {
            activity?.setShowWhenLocked(true)
        }

        onDispose {
            if (atLeastOreoMR1()) {
                activity?.setShowWhenLocked(false)
            }
        }
    }
    val onBackPressed = {
        if (viewState.backButtonSavesTask) {
            save()
        } else {
            discard()
        }
    }
    BackHandler {
        Timber.d("onBackPressed")
        onBackPressed()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    if (viewState.isReadOnly) {
                        IconButton(onClick = { onBackPressed() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    } else {
                        IconButton(onClick = { save() }) {
                            Icon(
                                imageVector = Icons.Outlined.Save,
                                contentDescription = stringResource(R.string.save)
                            )
                        }
                    }
                },
                title = {},
                actions = {
                    if (viewState.isReadOnly) {
                        return@TopAppBar
                    }
                    if (!viewState.isNew) {
                        IconButton(onClick = { delete() }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.delete_task),
                            )
                        }
                    }
                    if (viewState.backButtonSavesTask) {
                        IconButton(onClick = { discard() }) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = stringResource(R.string.menu_discard_changes),
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (viewState.showComments && !viewState.isReadOnly) {
                AndroidFragment<CommentBarFragment>(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WindowInsets.navigationBars.asPaddingValues())
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .gesturesDisabled(viewState.isReadOnly)
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            val scope = rememberCoroutineScope()
            viewState.displayOrder.forEach { tag ->
                when (tag) {
                    TAG_TITLE -> {
                        TitleRow(
                            text = viewState.task.title,
                            onChanged = { text: CharSequence? ->
                                editViewModel.setTitle(text.toString().trim { it <= ' ' })
                            },
                            linkify = linkify,
                            markdownProvider = markdownProvider,
                            isCompleted = viewState.isCompleted,
                            isRecurring = viewState.task.isRecurring,
                            priority = viewState.task.priority,
                            onComplete = {
                                if (viewState.isCompleted) {
                                    editViewModel.setComplete(false)
                                } else {
                                    editViewModel.setComplete(true)
                                    scope.launch {
                                        save()
                                    }
                                }
                            },
                            requestFocus = viewState.showKeyboard,
                            multiline = viewState.multilineTitle,
                        )
                    }

                    TAG_DUE_DATE -> DueDateRow(
                        dueDate = editViewModel.dueDate.collectAsStateWithLifecycle().value,
                        is24HourFormat = context.is24HourFormat,
                        alwaysDisplayFullDate = viewState.alwaysDisplayFullDate,
                        onClick = onClickDueDate,
                    )
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

                    TAG_LIST -> {
                        val listPickerLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartActivityForResult()
                        ) { it ->
                            it.data
                                ?.let { getParcelableExtra(it, EXTRA_FILTER, CaldavFilter::class.java) }
                                ?.let { editViewModel.setList(it) }
                        }
                        ListRow(
                            list = viewState.list,
                            colorProvider = colorProvider,
                            onClick = {
                                listPickerLauncher.launch(
                                    context = context,
                                    selectedFilter = viewState.list,
                                    listsOnly = true
                                )
                            }
                        )
                    }

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
                HorizontalDivider()
            }
            if (viewState.showComments) {
                CommentsRow(
                    comments = comments,
                    copyCommentToClipboard = { copyToClipboard(context, R.string.comment, it) },
                    deleteComment = deleteComment,
                    openImage = { FileHelper.startActionView(context, it) },
                )
            }
            val beastMode = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                context.findActivity()?.recreate()
            }
            BeastModeBanner(
                visible = viewState.showBeastModeHint,
                showSettings = {
                    editViewModel.hideBeastModeHint(click = true)
                    beastMode.launch(Intent(context, BeastModePreferences::class.java))
                },
                dismiss = dismissBeastMode,
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun TaskEditScreenPreview() {
    TasksTheme {
//        TaskEditScreen(
//
//        )
    }
}
