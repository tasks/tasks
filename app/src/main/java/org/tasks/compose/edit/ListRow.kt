package org.tasks.compose.edit

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.todoroo.astrid.api.Filter
import org.tasks.R
import org.tasks.compose.ChipGroup
import org.tasks.compose.FilterChip
import org.tasks.compose.TaskEditRow

@Composable
fun ListRow(
    list: Filter?,
    colorProvider: (Int) -> Int,
    onClick: () -> Unit,
) {
    TaskEditRow(
        iconRes = R.drawable.ic_list_24px,
        content = {
            ChipGroup(modifier = Modifier.padding(vertical = 20.dp)) {
                if (list != null) {
                    FilterChip(
                        filter = list,
                        defaultIcon = R.drawable.ic_list_24px,
                        showText = true,
                        showIcon = true,
                        onClick = { onClick() },
                        colorProvider = colorProvider
                    )
                }
            }
        },
        onClick = onClick
    )
}