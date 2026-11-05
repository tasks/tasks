package org.tasks.compose.pricing

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.tasks.themes.TestTags

private const val SWEEP_MILLIS = 1100

internal const val PRICE_SAMPLE = "000000"
internal const val PRICE_NOTE_SAMPLE = "0000000000"

@Composable
internal fun PriceShimmer(
    sample: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
) {
    val transition = rememberInfiniteTransition(label = "price shimmer")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SWEEP_MILLIS, easing = LinearEasing),
        ),
        label = "sweep",
    )
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
    Text(
        text = sample,
        style = style,
        fontWeight = fontWeight,
        color = Color.Transparent,
        modifier = modifier
            .clearAndSetSemantics { testTag = TestTags.PRICE_PLACEHOLDER }
            .clip(RoundedCornerShape(4.dp))
            .drawBehind {
                val band = size.width
                val startX = sweep * (size.width + band) - band
                drawRect(
                    Brush.horizontalGradient(
                        colors = listOf(base, highlight, base),
                        startX = startX,
                        endX = startX + band,
                    )
                )
            },
    )
}
