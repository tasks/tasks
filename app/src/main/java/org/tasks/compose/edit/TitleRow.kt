package org.tasks.compose.edit

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.tasks.R
import org.tasks.compose.CheckBox
import org.tasks.compose.TaskEditRow
import org.tasks.dialogs.Linkify
import org.tasks.markdown.MarkdownProvider
import org.tasks.themes.TasksTheme

@Composable
fun TitleRow(
    text: String?,
    onChanged: (CharSequence?) -> Unit,
    linkify: Linkify?,
    markdownProvider: MarkdownProvider?,
    desaturate: Boolean,
    isCompleted: Boolean,
    isRecurring: Boolean,
    priority: Int,
    onComplete: () -> Unit,
    requestFocus: Boolean,
) {
    TaskEditRow(
        icon = {
            CheckBox(
                isCompleted = isCompleted,
                isRecurring = isRecurring,
                priority = priority,
                onCompleteClick = onComplete,
                desaturate = desaturate,
                modifier = Modifier.padding(
                    start = 4.dp,
                    end = 20.dp,
                )
            )
        },
        content = {
            Column(verticalArrangement = Arrangement.Center) {
                Spacer(modifier = Modifier.height(3.dp))
                EditTextView(
                    text = text,
                    hint = stringResource(R.string.TEA_title_hint),
                    onChanged = onChanged,
                    linkify = linkify,
                    markdownProvider = markdownProvider,
                    strikethrough = isCompleted,
                    requestFocus = requestFocus,
                )
                Spacer(modifier = Modifier.height(11.dp))
            }
        },
    )
}

@ExperimentalComposeUiApi
@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun EmptyTitlePreview() {
    TasksTheme {
        TitleRow(
            text = null,
            onChanged = {},
            linkify = null,
            markdownProvider = null,
            desaturate = true,
            isCompleted = false,
            isRecurring = false,
            priority = 0,
            onComplete = {},
            requestFocus = false,
        )
    }
}

@ExperimentalComposeUiApi
@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun TitlePreview() {
    TasksTheme {
        TitleRow(
            text = """
                Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.

                Eleifend quam adipiscing vitae proin sagittis. Faucibus a pellentesque sit amet porttitor eget dolor.
            """.trimIndent(),
            onChanged = {},
            linkify = null,
            markdownProvider = null,
            desaturate = true,
            isCompleted = false,
            isRecurring = false,
            priority = 0,
            onComplete = {},
            requestFocus = false,
        )
    }
}
