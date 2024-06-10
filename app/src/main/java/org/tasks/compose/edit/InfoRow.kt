package org.tasks.compose.edit

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.tasks.R
import org.tasks.compose.TaskEditRow
import org.tasks.themes.TasksTheme
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun InfoRow(
    creationDate: Long?,
    modificationDate: Long?,
    completionDate: Long?,
    locale: Locale = Locale.getDefault(),
) {
    TaskEditRow(
        iconRes = R.drawable.ic_outline_info_24px,
        content = {
            Column(modifier = Modifier.padding(vertical = 20.dp)) {
                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", locale)
                creationDate?.let {
                    Text(
                        text = stringResource(
                            id = R.string.sort_created_group,
                            formatter.format(it)
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                modificationDate?.let {
                    Text(
                        text = stringResource(
                            id = R.string.sort_modified_group,
                            formatter.format(it)
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                completionDate?.takeIf { it > 0 }?.let {
                    Text(
                        text = stringResource(
                            id = R.string.sort_completion_group,
                            formatter.format(it)
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
    )
}

@ExperimentalComposeUiApi
@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun InfoPreview() {
    TasksTheme {
        InfoRow(
            creationDate = 1658727180000,
            modificationDate = 1658813557000,
            completionDate = 1658813557000,
            locale = Locale.US,
        )
    }
}