package org.tasks.dialogs

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.todoroo.astrid.dao.TaskDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import org.tasks.R
import org.tasks.compose.pickers.DueDateShortcuts
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.dialogs.DateTimePicker.Companion.NO_SELECTION
import org.tasks.notifications.NotificationManager
import org.tasks.preferences.Preferences
import org.tasks.themes.TasksTheme
import org.tasks.themes.Theme
import org.tasks.time.plusDays
import org.tasks.time.startOfDay
import javax.inject.Inject

@AndroidEntryPoint
class RescheduleAssistant : DialogFragment() {

    @Inject
    lateinit var activity: Activity

    @Inject
    lateinit var taskDao: TaskDao

    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var theme: Theme

    @Inject
    lateinit var preferences: Preferences

    private var selectedDay by mutableLongStateOf(NO_DAY)
    private val today = newDateTime().startOfDay()

    private val rescheduleAssistantViewModel: RescheduleAssistantViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        TasksTheme(
            theme = theme.themeBase.index,
            primary = theme.themeColor.primaryColor,
        ) {
            val rescheduleAssistantState =
                rescheduleAssistantViewModel.viewState.collectAsStateWithLifecycle().value

            val sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true
            )

            var displayedTitle by remember { mutableStateOf("") }
            var title by remember { mutableStateOf("") }
            val showControls = rescheduleAssistantState.currentTask != null

            LaunchedEffect(rescheduleAssistantState.currentTask) {
                displayedTitle =
                    rescheduleAssistantState.currentTask?.title ?: getString(R.string.reschedule_all_tasks_done)

                title = if (rescheduleAssistantState.currentTask == null) {
                    ""
                } else if (rescheduleAssistantState.isDueToday) {
                    getString(R.string.reschedule_todays_tasks)
                } else {
                    getString(R.string.reschedule_overdue_tasks)
                }

                if (!showControls) {
                    delay(2000)
                    dismiss()
                }
            }

            LaunchedEffect(Unit) {
                sheetState.show()
            }

            ModalBottomSheet(
                modifier = Modifier.statusBarsPadding(),
                sheetState = sheetState,
                onDismissRequest = { dismiss() },
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                ShowTaskWithOptions(
                    taskTitle = displayedTitle,
                    title = title,
                    showControls = showControls
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_DATE_RESCHEDULE_ASSISTANT -> {
                if (resultCode == Activity.RESULT_OK) {
                    rescheduleAssistantViewModel.reschedule(
                        data!!.getLongExtra(DateTimePicker.EXTRA_TIMESTAMP, 0L)
                    )
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    @Composable
    fun ShowTaskWithOptions(
        taskTitle: String,
        title: String,
        showControls: Boolean
    ) {
        Column(
            modifier = Modifier
                .padding(30.dp)
                .fillMaxWidth()
                .wrapContentSize(Alignment.Center)
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )

            AnimatedTitle(title = taskTitle)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .alpha(if (showControls) 1f else 0f)
                ) {
                    DueDateShortcuts(
                        today = today.millis,
                        tomorrow = remember { today.millis.plusDays(1) },
                        nextWeek = remember { today.millis.plusDays(7) },
                        selected = NO_SELECTION,
                        showNoDate = true,
                        selectedDay = { reschedule(it.startOfDay()) },
                        clearDate = { reschedule(0) },
                    )
                    ShortcutButton(
                        icon = Icons.Outlined.Menu,
                        text = "Custom",
                        onClick = {
                            DateTimePicker
                                .newDateTimePicker(
                                    target = this@RescheduleAssistant,
                                    rc = REQUEST_DATE_RESCHEDULE_ASSISTANT,
                                    current = 0,
                                    autoClose = preferences.getBoolean(
                                        R.string.p_auto_dismiss_datetime_edit_screen,
                                        false
                                    ),
                                    hideNoDate = true,
                                )
                                .show(parentFragmentManager, FRAG_TAG_DATE_PICKER_RESCHEDULE_ASSISTANT)
                        },
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .alpha(if (showControls) 1f else 0f)
                ) {
                    ShortcutButton(
                        icon = Icons.Outlined.Done,
                        text = getString(R.string.reschedule_done),
                        onClick = { rescheduleAssistantViewModel.markAsDone() },
                    )
                    ShortcutButton(
                        icon = Icons.Outlined.Delete,
                        text = getString(R.string.delete),
                        onClick = { rescheduleAssistantViewModel.delete() },
                    )
                    ShortcutButton(
                        icon = Icons.Outlined.SkipNext,
                        text = getString(R.string.reschedule_skip),
                        onClick = { rescheduleAssistantViewModel.skip() },
                    )
                }
            }
        }
    }


    @Composable
    fun AnimatedTitle(title: String, modifier: Modifier = Modifier) {
        AnimatedContent(
            targetState = title,
            transitionSpec = {
                ContentTransform(
                    targetContentEnter = slideInVertically(
                        initialOffsetY = { -it / 2 },
                        animationSpec = tween(400, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(400, easing = LinearEasing)),
                    initialContentExit = fadeOut(animationSpec = tween(0)),
                    sizeTransform = SizeTransform(clip = false)
                )
            },
            label = "AnimatedTitle"
        ) { animatedTitle ->
            Text(
                text = animatedTitle,
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }

    @Composable
    fun ShortcutButton(
        icon: ImageVector,
        text: String,
        onClick: () -> Unit,
    ) {
        val color = MaterialTheme.colorScheme.onSurface
        TextButton(
            onClick = { onClick() },
            colors = ButtonDefaults.textButtonColors(contentColor = color)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    imageVector = icon,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(color)
                )
                Text(
                    text = text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    private fun reschedule(day: Long = selectedDay) {
        selectedDay = day
        rescheduleAssistantViewModel.reschedule(selectedDay)
    }

    @Composable
    @Preview
    fun ShowTaskWithOptionsPreview() {
        ShowTaskWithOptions(
            taskTitle = "Some current task",
            title = "Reschedule overdue tasks",
            showControls = true
        )
    }

    companion object {
        const val NO_DAY = 0L
        private const val REQUEST_DATE_RESCHEDULE_ASSISTANT = 754454
        private const val FRAG_TAG_DATE_PICKER_RESCHEDULE_ASSISTANT =
            "frag_tag_date_picker_reschedule_assistant"

        fun newRescheduleAssistant(): RescheduleAssistant {
            val fragment = RescheduleAssistant()
            return fragment
        }
    }
}



