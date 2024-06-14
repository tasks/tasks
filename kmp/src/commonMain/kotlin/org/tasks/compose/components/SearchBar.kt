package org.tasks.compose.components

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    text: String,
    onTextChange: (String) -> Unit,
    placeHolder: String,
    onCloseClicked: () -> Unit,
    onSearchClicked: (String) -> Unit,
) {
    OutlinedTextField(
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.height(56.dp),
        value = text,
        onValueChange = {
            onTextChange(it)
        },
        placeholder = {
            Text(
                text = placeHolder,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxHeight(),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        },
        trailingIcon = {
            IconButton(onClick = { onCloseClicked() }) {
                if (text.isNotBlank()) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                onSearchClicked(text)
            }
        ),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}
