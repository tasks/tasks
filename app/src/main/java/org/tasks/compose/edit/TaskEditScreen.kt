package org.tasks.compose.edit

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.compose.AndroidFragment
import com.todoroo.astrid.activity.TaskEditFragment.Companion.gesturesDisabled
import org.tasks.R
import org.tasks.compose.BeastModeBanner
import org.tasks.data.entity.UserActivity
import org.tasks.extensions.Context.findActivity
import org.tasks.files.FileHelper
import org.tasks.fragments.CommentBarFragment
import org.tasks.themes.TasksTheme
import org.tasks.ui.TaskEditViewModel
import org.tasks.utility.copyToClipboard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    viewState: TaskEditViewModel.ViewState,
    comments: List<UserActivity>,
    save: () -> Unit,
    discard: () -> Unit,
    onBackPressed: () -> Unit,
    delete: () -> Unit,
    openBeastModeSettings: () -> Unit,
    dismissBeastMode: () -> Unit,
    deleteComment: (UserActivity) -> Unit,
    content: @Composable (Int) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
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
                        .navigationBarsPadding()
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
            viewState.displayOrder.forEach { tag ->
                content(tag)
                HorizontalDivider()
            }
            if (viewState.showComments) {
                val context = LocalContext.current
                CommentsRow(
                    comments = comments,
                    copyCommentToClipboard = {
                        copyToClipboard(context, R.string.comment, it)
                    },
                    deleteComment = deleteComment,
                    openImage = {
                        val activity = context.findActivity() ?: return@CommentsRow
                        FileHelper.startActionView(activity, it)
                    }
                )
            }
            BeastModeBanner(
                visible = viewState.showBeastModeHint,
                showSettings = openBeastModeSettings,
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
