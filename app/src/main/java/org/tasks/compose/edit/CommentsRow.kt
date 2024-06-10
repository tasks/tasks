package org.tasks.compose.edit

import android.net.Uri
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.todoroo.andlib.utility.DateUtilities
import org.tasks.R
import org.tasks.compose.DeleteButton
import org.tasks.compose.TaskEditRow
import org.tasks.data.entity.UserActivity
import org.tasks.data.pictureUri
import java.util.Locale

@Composable
fun CommentsRow(
    comments: List<UserActivity>,
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
    deleteComment: (UserActivity) -> Unit,
    openImage: (Uri) -> Unit,
) {
    Row(verticalAlignment = Alignment.Top) {
        Column(
            modifier = Modifier.weight(1f).padding(top = 8.dp),
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
                    modifier = Modifier.clickable { openImage(it) }.size(100.dp)
                )
            }
            Text(
                text = DateUtilities.getLongDateStringWithTime(comment.created!!, Locale.getDefault()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        DeleteButton(
            onClick = {
                // TODO: add confirmation dialog
                deleteComment(comment)
            }
        )
    }
}