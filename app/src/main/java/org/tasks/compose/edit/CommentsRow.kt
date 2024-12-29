package org.tasks.compose.edit

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import coil.compose.AsyncImage
import org.tasks.R
import org.tasks.compose.DeleteButton
import org.tasks.compose.TaskEditRow
import org.tasks.data.entity.UserActivity
import org.tasks.data.pictureUri
import org.tasks.kmp.org.tasks.time.getFullDateTime

@Composable
fun CommentsRow(
    comments: List<UserActivity>,
    copyCommentToClipboard: (String) -> Unit,
    deleteComment: (UserActivity) -> Unit,
    openImage: (Uri) -> Unit,
) {
    if (comments.isEmpty()) {
        return
    }
    TaskEditRow(
        iconRes = R.drawable.ic_outline_chat_bubble_outline_24px,
        content = {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                comments.forEach {
                    Comment(
                        comment = it,
                        copyCommentToClipboard = copyCommentToClipboard,
                        deleteComment = deleteComment,
                        openImage = openImage,
                    )
                }
            }
        }
    )
}

@Composable
fun Comment(
    comment: UserActivity,
    copyCommentToClipboard: (String) -> Unit,
    deleteComment: (UserActivity) -> Unit,
    openImage: (Uri) -> Unit,
) {
    Row(verticalAlignment = Alignment.Top) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 8.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = {
                        comment.message?.let(copyCommentToClipboard)
                    })
                },
        ) {
            comment.message?.let {
                // TODO: linkify text
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            comment.pictureUri?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    modifier = Modifier
                        .clickable { openImage(it) }
                        .size(100.dp)
                )
            }
            Text(
                text = getFullDateTime(comment.created!!),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        DeleteButton(stringResource(R.string.delete_comment)) {
            deleteComment(comment)
        }
    }
}