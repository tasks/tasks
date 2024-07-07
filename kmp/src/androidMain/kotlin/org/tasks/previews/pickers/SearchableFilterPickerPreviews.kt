package org.tasks.previews.pickers

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.tasks.compose.components.imageVectorByName
import org.tasks.compose.pickers.SearchableFilterPicker
import org.tasks.filters.FilterImpl
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.themes.TasksIcons
import org.tasks.themes.TasksTheme

// this doesn't actually work yet b/c of multiplatform resources
// https://github.com/JetBrains/compose-multiplatform/issues/4932

@Preview(widthDp = 320)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun FilterPickerPreview() {
    TasksTheme {
        SearchableFilterPicker(
            filters = listOf(
                FilterImpl("My Tasks", icon = TasksIcons.ALL_INBOX),
                NavigationDrawerSubheader(
                    "Filters",
                    false,
                    false,
                    NavigationDrawerSubheader.SubheaderType.PREFERENCE,
                    "",
                ),
            ),
            query = "",
            onQueryChange = {},
            selected = null,
            onClick = {},
            getIcon = { imageVectorByName(label = it.icon!!)!! },
            getColor = { 0 },
        )
    }
}
