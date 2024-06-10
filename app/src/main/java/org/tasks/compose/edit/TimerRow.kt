package org.tasks.compose.edit

import android.content.res.Configuration
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.tasks.R
import org.tasks.compose.DisabledText
import org.tasks.compose.TaskEditRow
import org.tasks.themes.TasksTheme
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import kotlin.time.Duration.Companion.seconds

@Composable
fun TimerRow(
    started: Long,
    estimated: Int,
    elapsed: Int,
    timerClicked: () -> Unit,
    onClick: () -> Unit,
) {
    TaskEditRow(
        iconRes = R.drawable.ic_outline_timer_24px,
        content = {
            var now by remember { mutableStateOf(currentTimeMillis()) }

            val newElapsed = if (started > 0) (now - started) / 1000L else 0
            val estimatedString = estimated
                .takeIf { it > 0 }
                ?.let {
                    stringResource(
                        id = R.string.TEA_timer_est,
                        DateUtils.formatElapsedTime(it.toLong())
                    )
                }
            val elapsedString =
                (newElapsed + elapsed)
                    .takeIf { it > 0 }
                    ?.let {
                        stringResource(
                            id = R.string.TEA_timer_elap,
                            DateUtils.formatElapsedTime(it)
                        )
                    }
            val text = when {
                estimatedString != null && elapsedString != null -> "$estimatedString, $elapsedString"
                estimatedString != null -> estimatedString
                elapsedString != null -> elapsedString
                else -> null
            }
            Row {
                if (text == null) {
                    DisabledText(
                        text = stringResource(id = R.string.TEA_timer_controls),
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 20.dp),
                    )
                } else {
                    Text(
                        text = text,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 20.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(
                    onClick = {
                        now = currentTimeMillis()
                        timerClicked()
                    },
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = if (started > 0) {
                            Icons.Outlined.Pause
                        } else {
                            Icons.Outlined.PlayArrow
                        },
                        modifier = Modifier.alpha(ContentAlpha.medium),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            LaunchedEffect(key1 = started) {
                while (started > 0) {
                    delay(1.seconds)
                    now = currentTimeMillis()
                }
            }
        },
        onClick = onClick
    )
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun NoTimer() {
    TasksTheme {
        TimerRow(started = 0, estimated = 0, elapsed = 0, timerClicked = {}, onClick = {})
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun RunningTimer() {
    TasksTheme {
        TimerRow(started = currentTimeMillis(), estimated = 900, elapsed = 400, timerClicked = {}, onClick = {})
    }
}