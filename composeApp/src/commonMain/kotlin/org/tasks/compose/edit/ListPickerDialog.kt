package org.tasks.compose.edit

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.tasks.compose.pickers.SearchableFilterPicker
import org.tasks.filters.Filter
import org.tasks.filters.FilterListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListPickerDialog(
    filters: List<FilterListItem>,
    query: String,
    onQueryChange: (String) -> Unit,
    selected: Filter?,
    onClick: (FilterListItem) -> Unit,
    getIcon: @Composable (Filter) -> String?,
    getColor: (Filter) -> Int,
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .padding(vertical = 32.dp)
            .heightIn(max = 500.dp),
    ) {
        SearchableFilterPicker(
            filters = filters,
            query = query,
            onQueryChange = onQueryChange,
            selected = selected,
            onClick = onClick,
            getIcon = getIcon,
            getColor = getColor,
        )
    }
}
