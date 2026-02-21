package org.tasks.compose.components

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp

val SEARCH_BAR_HEIGHT = 56.dp

@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    text: String,
    onTextChange: (String) -> Unit,
    placeHolder: String,
    onCloseClicked: () -> Unit,
    onSearchClicked: (String) -> Unit,
) {
    var canFocus by remember { mutableStateOf(false) }
    TextField(
        modifier = modifier
            .height(SEARCH_BAR_HEIGHT)
            .focusProperties {
                this.canFocus = canFocus
            },
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
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            textDirection = TextDirection.Content
        ),
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
    LaunchedEffect(Unit) {
        canFocus = true
    }
}
