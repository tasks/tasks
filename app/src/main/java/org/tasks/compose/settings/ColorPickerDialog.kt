package org.tasks.compose.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import org.tasks.themes.ThemeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerDialog(
    hasPro: Boolean,
    colors: List<ThemeColor>,
    onDismiss: () -> Unit,
    onColorSelected: (ThemeColor) -> Unit,
    onColorWheelSelected: () -> Unit = {},
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                ColorPicker(
                    colors = colors,
                    onSelected = { color ->
                        onColorSelected(color)
                        onDismiss()
                    },
                    onColorWheelSelected = {
                        onColorWheelSelected()
                        onDismiss()
                    },
                    hasPro = hasPro,
                )
            }
        }
    }
}
