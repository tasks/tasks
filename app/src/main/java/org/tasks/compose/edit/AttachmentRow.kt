package org.tasks.compose.edit

import android.content.res.Configuration
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.decode.VideoFrameDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.accompanist.flowlayout.FlowRow
import com.todoroo.andlib.utility.AndroidUtilities
import org.tasks.R
import org.tasks.compose.DisabledText
import org.tasks.compose.TaskEditRow
import org.tasks.data.entity.TaskAttachment
import org.tasks.files.FileHelper
import org.tasks.themes.TasksTheme

private val SIZE = 128.dp

@Composable
fun AttachmentRow(
    attachments: List<TaskAttachment>,
    openAttachment: (TaskAttachment) -> Unit,
    deleteAttachment: (TaskAttachment) -> Unit,
    addAttachment: () -> Unit,
) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(VideoFrameDecoder.Factory())
                if (AndroidUtilities.atLeastP()) {
                    add(ImageDecoderDecoder.Factory(true))
                } else {
                    add(GifDecoder.Factory(true))
                }
                add(SvgDecoder.Factory())
            }
            .build()
    }
    val thumbnailSize = with(LocalDensity.current) { SIZE.toPx().toInt() }
    TaskEditRow(
        iconRes = R.drawable.ic_outline_attachment_24px,
        content = {
            if (attachments.isNotEmpty()) {
                FlowRow(
                    mainAxisSpacing = 8.dp,
                    crossAxisSpacing = 8.dp,
                    modifier = Modifier.padding(top = 24.dp, bottom = 24.dp, end = 16.dp)
                ) {
                    attachments.forEach {
                        val mimeType = FileHelper.getMimeType(LocalContext.current, it.uri.toUri())
                        when {
                            mimeType?.startsWith("image/") == true ||
                                    mimeType?.startsWith("video/") == true -> {
                                Box {
                                    var failed by remember { mutableStateOf(false) }
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .memoryCachePolicy(CachePolicy.ENABLED)
                                            .data(it.uri)
                                            .crossfade(true)
                                            .size(thumbnailSize)
                                            .build(),
                                        imageLoader = imageLoader,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { openAttachment(it) },
                                        onError = { failed = true }
                                    )
                                    if (failed) {
                                        NoThumbnail(
                                            filename = it.name,
                                            mimeType = mimeType,
                                            open = { openAttachment(it) },
                                            delete = { deleteAttachment(it) }
                                        )
                                    } else {
                                        if (mimeType.startsWith("video/")) {
                                            Icon(
                                                imageVector = Icons.Outlined.PlayCircle,
                                                contentDescription = null,
                                                tint = Color.White.copy(
                                                    alpha = ContentAlpha.medium
                                                ),
                                                modifier = Modifier.align(Alignment.Center),
                                            )
                                        }
                                        DeleteAttachment(
                                            onClick = { deleteAttachment(it) },
                                            color = Color.White,
                                        )
                                    }
                                }
                            }
                            else ->
                                NoThumbnail(
                                    filename = it.name,
                                    mimeType = mimeType,
                                    open = { openAttachment(it) },
                                    delete = { deleteAttachment(it) },
                                )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .height(SIZE)
                            .clickable { addAttachment() }
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.medium),
                                shape = RoundedCornerShape(8.dp),
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = stringResource(id = R.string.add_attachment),
                            modifier = Modifier
                                .size(48.dp)
                                .align(Alignment.Center),
                            tint = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = ContentAlpha.medium
                            ),
                        )
                    }
                }
            } else {
                DisabledText(
                    text = stringResource(id = R.string.add_attachment),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { addAttachment() }
                        .padding(vertical = 20.dp),
                )
            }
        },
    )
}

@Composable
fun NoThumbnail(
    filename: String,
    mimeType: String?,
    open: () -> Unit,
    delete: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(width = 100.dp, height = SIZE)
            .clickable { open() }
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.medium),
                shape = RoundedCornerShape(8.dp),
            ),
    ) {
        Column(modifier = Modifier.align(Alignment.Center)) {
            Icon(
                imageVector = when {
                    mimeType?.startsWith("image/") == true -> Icons.Outlined.Image
                    mimeType?.startsWith("video/") == true -> Icons.Outlined.Movie
                    mimeType?.startsWith("audio/") == true -> Icons.Outlined.MusicNote
                    else -> Icons.Outlined.Description
                },
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .alpha(ContentAlpha.medium),
                tint = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = ContentAlpha.medium
                ),
            )
            Text(
                text = filename,
                style = MaterialTheme.typography.bodySmall.copy(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(8.dp),
            )
        }
        DeleteAttachment(
            onClick = { delete() },
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun BoxScope.DeleteAttachment(
    onClick: () -> Unit,
    color: Color,
) {
    Icon(
        imageVector = Icons.Outlined.Cancel,
        contentDescription = null,
        modifier = Modifier
            .alpha(ContentAlpha.medium)
            .align(Alignment.TopEnd)
            .padding(vertical = 4.dp, horizontal = 4.dp)
            .clickable { onClick() },
        tint = color.copy(alpha = ContentAlpha.medium),
    )
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun NoAttachments() {
    TasksTheme {
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
    TasksTheme {
        AttachmentRow(
            attachments = listOf(
                TaskAttachment(
                    uri = "file://attachment.txt",
                    name = "attachment.txt",
                )
            ),
            openAttachment = {},
            deleteAttachment = {},
            addAttachment = {},
        )
    }
}