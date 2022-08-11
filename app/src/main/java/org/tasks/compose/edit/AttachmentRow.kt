package org.tasks.compose.edit

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.google.android.material.composethemeadapter.MdcTheme
import org.tasks.R
import org.tasks.compose.DisabledText
import org.tasks.compose.TaskEditRow
import org.tasks.data.TaskAttachment

@Composable
fun AttachmentRow(
    attachments: List<TaskAttachment>,
    openAttachment: (TaskAttachment) -> Unit,
    deleteAttachment: (TaskAttachment) -> Unit,
    addAttachment: () -> Unit,
) {
    TaskEditRow(
        iconRes = R.drawable.ic_outline_attachment_24px,
        content = {
            Column(
                modifier = Modifier.padding(top = if (attachments.isEmpty()) 0.dp else 8.dp),
            ) {
                attachments.forEach {
                    Row(
                        modifier = Modifier
                            .clickable { openAttachment(it) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = it.name!!,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { deleteAttachment(it) }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(
                                    id = R.string.delete
                                ),
                                modifier = Modifier.alpha(ContentAlpha.medium),
                            )
                        }
                    }
                }
                DisabledText(
                    text = stringResource(id = R.string.add_attachment),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { addAttachment() }
                        .padding(
                            top = if (attachments.isEmpty()) 20.dp else 8.dp,
                            bottom = 20.dp,
                        )
                )
            }
        },
    )
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun NoAttachments() {
    MdcTheme {
        AttachmentRow(
            attachments = emptyList(),
            openAttachment = {},
            deleteAttachment = {},
            addAttachment = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun AttachmentPreview() {
    MdcTheme {
        AttachmentRow(
            attachments = listOf(
                TaskAttachment("", "file://attachment.txt".toUri(), "attachment.txt")
            ),
            openAttachment = {},
            deleteAttachment = {},
            addAttachment = {},
        )
    }
}