package org.tasks.compose

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun <T> Spinner(
    options: List<String>,
    values: List<T>,
    selected: T,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onSelected: (T) -> Unit,
    setExpanded: (Boolean) -> Unit,
) {
    val selectedIndex = values.indexOf(selected)
    val selectedOption = options[selectedIndex]
    Row(modifier = modifier) {
        Text(
            text = selectedOption,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Icon(
            imageVector = Icons.Outlined.ArrowDropDown,
            contentDescription = "",
            tint = MaterialTheme.colorScheme.onSurface,
        )
        DropdownMenu(expanded = expanded, onDismissRequest = {
            setExpanded(false)
        }) {
            options.forEach {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    onClick = { onSelected(values[options.indexOf(it)]) }
                )
            }
        }
    }
}