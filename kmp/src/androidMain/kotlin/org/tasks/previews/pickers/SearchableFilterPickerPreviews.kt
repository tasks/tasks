package org.tasks.previews.pickers

import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AllInbox
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.tasks.compose.pickers.SearchableFilterPicker
import org.tasks.themes.TasksTheme
import org.tasks.filters.FilterImpl
import org.tasks.filters.NavigationDrawerSubheader

// this doesn't actually work yet b/c of multiplatform resources
// https://github.com/JetBrains/compose-multiplatform/issues/4932

@Preview(widthDp = 320)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun FilterPickerPreview() {
    TasksTheme {
        SearchableFilterPicker(
            filters = listOf(
                FilterImpl("My Tasks", icon = 4),
                NavigationDrawerSubheader(
                    "Filters",
                    false,
                    false,
                    NavigationDrawerSubheader.SubheaderType.PREFERENCE,
                    0L,
                ),
            ),
            query = "",
            onQueryChange = {},
            selected = null,
            onClick = {},
            getIcon = { when (it.icon) {
                4 -> Icons.Outlined.AllInbox
                else -> Icons.Outlined.QuestionMark
            } },
            getColor = { 0 },
        )
    }
}
