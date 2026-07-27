package org.tasks.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.tasks.extensions.formatNumber
import org.tasks.extensions.parseInteger
import java.util.Locale

private val InputMinWidth = 60.dp
private val InputHeight = 45.dp
private val InputHorizontalPadding = 8.dp
private val CursorWidth = 2.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlinedNumberInput(
    number: Int,
    locale: Locale,
    onTextChanged: (Int) -> Unit,
    onFocus: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val numberString = remember(number, locale) {
        number.takeIf { it > 0 }?.let { locale.formatNumber(it, grouping = false) } ?: ""
    }
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val inputWidth = remember(numberString, textStyle, density) {
        val textWidth = with(density) {
            textMeasurer.measure(numberString, textStyle).size.width.toDp()
        }
        maxOf(InputMinWidth, textWidth + InputHorizontalPadding * 2 + CursorWidth)
    }
    BasicTextField(
        value = numberString,
        onValueChange = { input ->
            if (input.isEmpty()) {
                onTextChanged(0)
            } else {
                locale.parseInteger(input)
                    ?.takeIf { it >= 0 }
                    ?.let(onTextChanged)
            }
        },
        textStyle = textStyle,
        modifier = Modifier
            .border(
                width = 1.dp,
                color = border(),
                shape = RoundedCornerShape(4.dp),
            )
            .onFocusChanged {
                if (it.hasFocus) {
                    onFocus()
                }
            }
            .width(inputWidth)
            .height(InputHeight),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
        interactionSource = interactionSource,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    ) {
        TextFieldDefaults.DecorationBox(
            value = numberString,
            innerTextField = it,
            singleLine = true,
            enabled = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            contentPadding = TextFieldDefaults.textFieldWithoutLabelPadding(
                start = InputHorizontalPadding,
                end = InputHorizontalPadding,
                top = 0.dp,
                bottom = 0.dp,
            )
        )
    }
}
