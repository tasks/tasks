package org.tasks.compose.edit

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.material.composethemeadapter.MdcTheme
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.FilterImpl
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

@ExperimentalComposeUiApi
@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun ListPreview() {
    MdcTheme {
        ListRow(
            list = FilterImpl("Default list", ""),
            colorProvider = { -769226 },
            onClick = {},
        )
    }
}