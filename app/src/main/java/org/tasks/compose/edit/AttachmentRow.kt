package org.tasks.compose.edit

import org.tasks.themes.TasksIcons
import org.tasks.compose.components.SymbolIcon
import android.content.res.Configuration
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
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
import com.todoroo.andlib.utility.AndroidUtilities
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import org.tasks.R
import org.tasks.compose.DisabledText
import org.tasks.compose.TaskEditRow
import org.tasks.data.entity.TaskAttachment
import org.tasks.files.FileHelper
import org.tasks.themes.TasksTheme

private val SIZE = 128.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttachmentRow(
    attachments: ImmutableSet<TaskAttachment>,
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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
                                            SymbolIcon(
                                                name = TasksIcons.PLAY_CIRCLE,
                                                contentDescription = null,
                                                tint = Color.White.copy(
                                                    alpha = 0.87f
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
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f),
                                shape = RoundedCornerShape(8.dp),
                            ),
                    ) {
                        SymbolIcon(
                            name = TasksIcons.ADD,
                            contentDescription = stringResource(id = R.string.add_attachment),
                            modifier = Modifier
                                .size(48.dp)
                                .align(Alignment.Center),
                            tint = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.87f
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
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f),
                shape = RoundedCornerShape(8.dp),
            ),
    ) {
        Column(modifier = Modifier.align(Alignment.Center)) {
            SymbolIcon(
                name = when {
                    mimeType?.startsWith("image/") == true -> TasksIcons.IMAGE
                    mimeType?.startsWith("video/") == true -> TasksIcons.MOVIE
                    mimeType?.startsWith("audio/") == true -> TasksIcons.MUSIC_NOTE
                    else -> TasksIcons.DESCRIPTION
                },
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .alpha(ContentAlpha.medium),
                tint = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = 0.87f
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
    SymbolIcon(
        name = TasksIcons.CANCEL,
        contentDescription = null,
        modifier = Modifier
            .alpha(ContentAlpha.medium)
            .align(Alignment.TopEnd)
            .padding(vertical = 4.dp, horizontal = 4.dp)
            .clickable { onClick() },
        tint = color.copy(alpha = 0.87f),
    )
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun NoAttachments() {
    TasksTheme {
        AttachmentRow(
            attachments = persistentSetOf(),
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
            attachments = persistentSetOf(
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