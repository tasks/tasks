package org.tasks.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.Font
import org.tasks.icons.MaterialSymbols
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.material_symbols_outlined

@Composable
fun SymbolIcon(
    name: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    filled: Boolean = false,
) {
    val semantics = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        }
    } else {
        Modifier
    }
    Box(modifier.then(semantics).size(24.dp).symbol(name, tint, filled))
}

@Composable
fun TasksIcon(
    modifier: Modifier = Modifier,
    label: String?,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = 24.dp,
) {
    val name = label?.iconName
    val semantics = if (label != null && iconExists(name)) {
        Modifier.semantics {
            contentDescription = label
            role = Role.Image
        }
    } else {
        Modifier
    }
    Box(modifier.then(semantics).size(size).symbol(name, tint, filled = false))
}

@Composable
private fun Modifier.symbol(name: String?, tint: Color, filled: Boolean): Modifier {
    val glyph = name?.let { MaterialSymbols.glyph(it) } ?: return this
    val fontFamily = FontFamily(
        Font(
            Res.font.material_symbols_outlined,
            variationSettings = FontVariation.Settings(FontVariation.Setting("FILL", if (filled) 1f else 0f)),
        )
    )
    val mirrored = LocalLayoutDirection.current == LayoutDirection.Rtl && MaterialSymbols.isMirrored(name)
    val textMeasurer = rememberTextMeasurer()
    return drawWithCache {
        val em = size.minDimension
        val layout = textMeasurer.measure(glyph, TextStyle(fontFamily = fontFamily, fontSize = em.toSp()))
        val topLeft = Offset((size.width - em) / 2, (size.height - em) / 2 + em - layout.firstBaseline)
        onDrawBehind {
            if (mirrored) {
                scale(scaleX = -1f, scaleY = 1f) { drawText(layout, color = tint, topLeft = topLeft) }
            } else {
                drawText(layout, color = tint, topLeft = topLeft)
            }
        }
    }
}

fun iconExists(label: String?): Boolean = label != null && MaterialSymbols.codepoint(label.iconName) != null

val String.iconName: String
    get() = removePrefix(LEGACY_ICON_PREFIX)

private const val LEGACY_ICON_PREFIX = "gmo_"
