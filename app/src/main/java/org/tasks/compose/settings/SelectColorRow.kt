package org.tasks.compose.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.NotInterested
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import org.tasks.R
import org.tasks.compose.Constants
import org.tasks.kmp.org.tasks.compose.settings.SettingRow
import org.tasks.themes.ColorTone
import org.tasks.themes.TasksTheme
import org.tasks.themes.ThemeColor
import org.tasks.themes.tonalColor

private fun ThemeColor.toPickerColor() = PickerColor(
    originalColor = originalColor,
    primaryColor = pickerColor,
    colorOnPrimary = colorOnPrimary,
    isFree = isFree,
)

@Composable
fun SelectColorRow(
    hasPro: Boolean,
    color: Int,
    colors: List<ThemeColor>,
    purchase: () -> Unit,
    selectColor: (Int) -> Unit,
) {
    val pickerColors = remember(colors) { colors.map { it.toPickerColor() } }
    var showColorPicker by rememberSaveable { mutableStateOf(false) }
    var showColorWheel by rememberSaveable { mutableStateOf(false) }
    if (showColorPicker) {
        BackHandler {
            showColorPicker = false
        }
        ColorPickerDialog(
            hasPro = hasPro,
            colors = pickerColors,
            onDismiss = { showColorPicker = false },
            onColorSelected = {
                if (hasPro || it.isFree) {
                    selectColor(it.originalColor)
                } else {
                    purchase()
                }
            },
            onColorWheelSelected = {
                if (hasPro) {
                    showColorWheel = true
                } else {
                    purchase()
                }
                showColorPicker = false
            },
        )
    }
    if (showColorWheel) {
        BackHandler {
            showColorWheel = false
            showColorPicker = true
        }
        val context = LocalContext.current
        var selected by remember { mutableIntStateOf(0) }
        LaunchedEffect(showColorWheel) {
            if (!showColorWheel) return@LaunchedEffect
            ColorPickerDialogBuilder
                .with(context)
                .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
                .density(7)
                .setOnColorChangedListener { which ->
                    selected = which
                }
                .setOnColorSelectedListener { which ->
                    selected = which
                }
                .lightnessSliderOnly()
                .setPositiveButton(R.string.ok) { _, _, _ ->
                    selectColor(selected)
                }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    showColorPicker = true
                }
                .apply {
                    if (color != 0) {
                        initialColor(color)
                    }
                }
                .build()
                .apply {
                    setOnDismissListener {
                        showColorWheel = false
                    }
                }
                .show()
        }
    }
    SettingRow(
        modifier = Modifier.clickable(onClick = { showColorPicker = true }),
        left = {
            val isPreset = remember(color, colors) {
                colors.any { it.originalColor == color }
            }
            val isDark = isSystemInDarkTheme()
            val adjusted = remember(color, isDark, isPreset) {
                if (isPreset) {
                    tonalColor(color, if (isDark) ColorTone.DARK_TITLE else ColorTone.LIGHT_TITLE)
                } else {
                    color
                }
            }
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (color == 0) {
                    Icon(
                        imageVector = Icons.Outlined.NotInterested,
                        tint = colorResource(R.color.icon_tint_with_alpha),
                        contentDescription = null
                    )
                } else {
                    val borderColor =
                        colorResource(R.color.icon_tint_with_alpha)  // colorResource(R.color.text_tertiary)
                    Canvas(modifier = Modifier.size(24.dp)) {
                        drawCircle(color = Color(adjusted))
                        drawCircle(color = borderColor, style = Stroke(width = 4.0f))
                    }
                }
            }
        },
        center = {
            Text(
                text = stringResource(R.string.color),
                modifier = Modifier.padding(start = Constants.KEYLINE_FIRST)
            )
        },
        right = {
            if (color != 0) {
                IconButton(onClick = { selectColor(0) }) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = null
                    )
                }
            }
        }
    )
}

@Composable
@Preview(showBackground = true)
private fun ColorSelectPreview () {
    TasksTheme {
        SelectColorRow(
            hasPro = true,
            colors = emptyList(),
            purchase = {},
            color = Color.Red.toArgb(),
            selectColor = {},
        )
    }
}
