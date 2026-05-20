package org.tasks.compose

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import org.tasks.R

@Composable
fun ColorWheelDialog(
    initialColor: Int,
    onColorSelected: (Int) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler {
        onCancel()
    }
    val context = LocalContext.current
    var selected by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        val dialog = ColorPickerDialogBuilder
            .with(context)
            .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
            .density(7)
            .setOnColorChangedListener { which -> selected = which }
            .setOnColorSelectedListener { which -> selected = which }
            .lightnessSliderOnly()
            .setPositiveButton(R.string.ok) { _, _, _ -> onColorSelected(selected) }
            .setNegativeButton(R.string.cancel) { _, _ -> onCancel() }
            .apply {
                if (initialColor != 0) {
                    initialColor(initialColor)
                }
            }
            .build()
            .apply {
                setOnDismissListener { onDismiss() }
            }
        dialog.show()
        onDispose { dialog.dismiss() }
    }
}
