package org.tasks.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@Composable
fun OutlinedSpinner(
    text: String,
    options: List<String>,
    onSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    OutlinedBox(
        modifier = Modifier.clickable { expanded = !expanded }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp),
        ) {
            Text(text = text)
            Icon(
                imageVector = Icons.Outlined.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colors.onSurface,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset((-8).dp, 0.dp),
        ) {
            options.forEachIndexed { index, item ->
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onSelected(index)
                    },
                    content = {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.body2,
                        )
                    },
                )
            }
        }
    }
}
