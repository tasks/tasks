package org.tasks.compose

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.material.composethemeadapter.MdcTheme
import org.tasks.R
import org.tasks.Tasks

@ExperimentalAnimationApi
@Composable
fun AnimatedBanner(
    visible: Boolean,
    content: @Composable () -> Unit,
    buttons: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            content()
            Row(
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                buttons()
            }
            Divider()
        }
    }
}

@ExperimentalAnimationApi
@Composable
fun SubscriptionNagBanner(
    visible: Boolean,
    subscribe: () -> Unit,
    dismiss: () -> Unit,
) {
    AnimatedBanner(
        visible = visible,
        content = {
            Text(
                text = stringResource(
                    id = if (Tasks.IS_GENERIC) {
                        R.string.enjoying_tasks
                    } else {
                        R.string.tasks_needs_your_support
                    }
                ),
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    id = if (Tasks.IS_GENERIC) {
                        R.string.tasks_needs_your_support
                    } else {
                        R.string.support_development_subscribe
                    }
                ),
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        },
        buttons = {
            BannerTextButton(text = R.string.dismiss, dismiss)
            val res = if (Tasks.IS_GENERIC) {
                R.string.TLA_menu_donate
            } else {
                R.string.button_subscribe
            }
            BannerTextButton(text = res, subscribe)
        }
    )
}

@ExperimentalAnimationApi
@Composable
fun BeastModeBanner(
    visible: Boolean,
    showSettings: () -> Unit,
    dismiss: () -> Unit,
) {
    AnimatedBanner(
        visible = visible,
        content = {
            Text(
                text = stringResource(id = R.string.hint_customize_edit_title),
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.hint_customize_edit_body),
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        },
        buttons = {
            BannerTextButton(text = R.string.dismiss, onClick = dismiss)
            BannerTextButton(text = R.string.TLA_menu_settings, onClick = showSettings)
        }
    )
}

@Composable
fun BannerTextButton(text: Int, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = stringResource(id = text),
            style = MaterialTheme.typography.button.copy(
                color = MaterialTheme.colors.secondary
            ),
        )
    }
}

@ExperimentalAnimationApi
@Preview(showBackground = true)
@Preview(showBackground = true, backgroundColor = 0x202124, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BeastModePreview() = MdcTheme {
    BeastModeBanner(visible = true, showSettings = {}, dismiss = {})
}

@ExperimentalAnimationApi
@Preview(showBackground = true)
@Preview(showBackground = true, backgroundColor = 0x202124, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SubscriptionNagPreview() = MdcTheme {
    SubscriptionNagBanner(visible = true, subscribe = {}, dismiss = {})
}

