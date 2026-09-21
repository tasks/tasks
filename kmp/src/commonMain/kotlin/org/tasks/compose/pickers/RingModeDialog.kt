package org.tasks.compose.pickers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.CustomDialog
import org.tasks.compose.edit.ringModeString

const val RING_ONCE = 0
const val RING_FIVE_TIMES = 1
const val RING_NONSTOP = 2

val RING_MODES = listOf(RING_ONCE, RING_FIVE_TIMES, RING_NONSTOP)

@Composable
fun RingModeDialog(
    visible: Boolean,
    selected: Int,
    onSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    CustomDialog(visible = visible, onDismiss = onDismiss) {
        Column(modifier = Modifier.selectableGroup()) {
            RING_MODES.forEach { mode ->
                Row(
                    verticalAlignment = CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = mode == selected,
                            role = Role.RadioButton,
                            onClick = { onSelected(mode) },
                        )
                        .padding(horizontal = 8.dp),
                ) {
                    RadioButton(
                        selected = mode == selected,
                        onClick = null,
                    )
                    Text(
                        text = stringResource(ringModeString(mode)),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                    )
                }
            }
        }
    }
}
