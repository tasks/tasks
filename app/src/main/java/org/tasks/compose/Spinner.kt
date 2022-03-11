package org.tasks.compose

import androidx.compose.foundation.layout.Row
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
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
        Text(text = selectedOption)
        Icon(imageVector = Icons.Outlined.ArrowDropDown, contentDescription = "")
        DropdownMenu(expanded = expanded, onDismissRequest = {
            setExpanded(false)
        }) {
            options.forEach {
                DropdownMenuItem(onClick = {
                    onSelected(values[options.indexOf(it)])
                }) {
                    Text(text = it)
                }
            }
        }
    }
}