package org.tasks.compose.edit

import android.content.res.Configuration
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.material.composethemeadapter.MdcTheme
import kotlinx.coroutines.delay
import org.tasks.R
import org.tasks.compose.DisabledText
import org.tasks.compose.TaskEditRow
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
            var now by remember { mutableStateOf(System.currentTimeMillis()) }

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
                    )
                }
                IconButton(
                    onClick = {
                        now = System.currentTimeMillis()
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
                        contentDescription = null
                    )
                }
            }
            LaunchedEffect(key1 = started) {
                while (started > 0) {
                    delay(1.seconds)
                    now = System.currentTimeMillis()
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
    MdcTheme {
        TimerRow(started = 0, estimated = 0, elapsed = 0, timerClicked = {}, onClick = {})
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun RunningTimer() {
    MdcTheme {
        TimerRow(started = System.currentTimeMillis(), estimated = 900, elapsed = 400, timerClicked = {}, onClick = {})
    }
}