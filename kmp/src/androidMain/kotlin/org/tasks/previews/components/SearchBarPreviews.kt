package org.tasks.previews.components

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.tasks.compose.components.SearchBar
import org.tasks.themes.TasksTheme

@Preview(widthDp = 320, showBackground = true)
@Preview(widthDp = 320, showBackground = false, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SearchBarPreviewPlaceholder() {
    TasksTheme {
        SearchBar(
            text = "",
            onTextChange = {},
            placeHolder = "Search",
            onCloseClicked = {},
            onSearchClicked = {},
        )
    }
}

@Preview(widthDp = 320, showBackground = true)
@Preview(widthDp = 320, showBackground = false, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SearchBarPreviewSearching() {
    TasksTheme {
        SearchBar(
            text = "Testing",
            onTextChange = {},
            placeHolder = "Search",
            onCloseClicked = {},
            onSearchClicked = {},
        )
    }
}
