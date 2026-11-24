package org.tasks.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.tasks.previews.PREVIEW_NIGHT_MODE
import org.tasks.themes.TasksTheme
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.upgrading_description
import tasks.kmp.generated.resources.upgrading_title

const val UPGRADING_SCREEN_DELAY_MS = 500L

private val SPINNER_SIZE = 160.dp
private val SPINNER_SPACING = 32.dp
private val EDGE_PADDING = 32.dp

@Composable
fun UpgradingScreen(
    modifier: Modifier = Modifier,
    revealAfterMs: Long = UPGRADING_SCREEN_DELAY_MS,
) {
    var visible by remember { mutableStateOf(revealAfterMs <= 0) }
    LaunchedEffect(revealAfterMs) {
        if (!visible) {
            delay(revealAfterMs)
            visible = true
        }
    }
    Surface(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(visible = visible, enter = fadeIn()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                if (maxWidth > maxHeight) {
                    WideLayout()
                } else {
                    TallLayout()
                }
            }
        }
    }
}

@Composable
private fun WideLayout() {
    Row(
        modifier = Modifier.fillMaxSize().padding(EDGE_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SPINNER_SPACING, Alignment.CenterHorizontally),
    ) {
        Spinner()
        UpgradingText(
            horizontalAlignment = Alignment.Start,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

@Composable
private fun TallLayout() {
    Layout(
        content = {
            Spinner()
            UpgradingText(
                horizontalAlignment = Alignment.CenterHorizontally,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = EDGE_PADDING),
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val spinner = measurables[0].measure(loose)
        val text = measurables[1].measure(loose)
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val spinnerY = (height - spinner.height) / 2
        val textY = minOf(
            spinnerY + spinner.height + SPINNER_SPACING.roundToPx(),
            height - text.height,
        ).coerceAtLeast(0)
        layout(width, height) {
            spinner.place((width - spinner.width) / 2, spinnerY)
            text.place((width - text.width) / 2, textY)
        }
    }
}

@Composable
private fun Spinner() {
    CircularProgressIndicator(
        modifier = Modifier.size(SPINNER_SIZE),
        strokeWidth = 8.dp,
    )
}

@Composable
private fun UpgradingText(
    horizontalAlignment: Alignment.Horizontal,
    textAlign: TextAlign,
    modifier: Modifier = Modifier,
) {
    Column(horizontalAlignment = horizontalAlignment, modifier = modifier) {
        Text(
            text = stringResource(Res.string.upgrading_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = textAlign,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.upgrading_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = textAlign,
        )
    }
}

@Preview(showBackground = true, name = "Upgrading - Light")
@Preview(showBackground = true, uiMode = PREVIEW_NIGHT_MODE, name = "Upgrading - Dark")
@Preview(showBackground = true, name = "Upgrading - Landscape phone", widthDp = 720, heightDp = 360)
@Preview(showBackground = true, name = "Upgrading - Wide", widthDp = 1280, heightDp = 900)
@Preview(showBackground = true, name = "Upgrading - Short", widthDp = 300, heightDp = 340)
@Composable
private fun UpgradingScreenPreview() {
    TasksTheme {
        UpgradingScreen(revealAfterMs = 0)
    }
}
