package org.tasks.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import org.tasks.extensions.formatNumber
import org.tasks.extensions.parseInteger
import java.util.Locale

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun OutlinedNumberInput(
    number: Int,
    onTextChanged: (Int) -> Unit,
    onFocus: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val context = LocalContext.current
    val locale = remember {
        ConfigurationCompat
            .getLocales(context.resources.configuration)
            .get(0)
            ?: Locale.getDefault()
    }
    val numberString = remember(number) {
        number.takeIf { it > 0 }?.let { locale.formatNumber(it) } ?: ""
    }
    BasicTextField(
        value = numberString,
        onValueChange = {
            val newValue = locale
                .parseInteger(it)
                ?: 0
            onTextChanged(newValue)
        },
        textStyle = MaterialTheme.typography.body1.copy(
            color = MaterialTheme.colors.onSurface,
            textAlign = TextAlign.Center,
        ),
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
            .width(60.dp)
            .height(45.dp)
            .fillMaxWidth(),
        cursorBrush = SolidColor(MaterialTheme.colors.onBackground),
        interactionSource = interactionSource,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    ) {
        TextFieldDefaults.TextFieldDecorationBox(
            value = number.toString(),
            innerTextField = it,
            singleLine = true,
            enabled = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            // keep horizontal paddings but change the vertical
            contentPadding = TextFieldDefaults.textFieldWithoutLabelPadding(
                top = 0.dp, bottom = 0.dp
            )
        )
    }
}
