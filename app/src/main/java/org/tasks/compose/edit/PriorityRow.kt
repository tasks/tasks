package org.tasks.compose.edit

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.material.composethemeadapter.MdcTheme
import com.todoroo.astrid.data.Task
import org.tasks.R
import org.tasks.compose.TaskEditRow
import org.tasks.themes.ColorProvider.Companion.priorityColor

@Composable
fun PriorityRow(
    priority: Int,
    onChangePriority: (Int) -> Unit,
    desaturate: Boolean,
) {
    TaskEditRow(
        iconRes = R.drawable.ic_outline_flag_24px,
        content = {
            PriorityLabeled(
                selected = priority,
                onClick = { onChangePriority(it) },
                desaturate = desaturate,
            )
        },
    )
}

@Composable
fun Priority(
    selected: Int,
    onClick: (Int) -> Unit = {},
    desaturate: Boolean,
) {
    Row(horizontalArrangement = Arrangement.SpaceBetween) {
        for (i in Task.Priority.NONE downTo Task.Priority.HIGH) {
            PriorityButton(
                priority = i,
                selected = selected,
                onClick = onClick,
                desaturate = desaturate,
            )
        }
    }
}

@Composable
fun PriorityLabeled(
    selected: Int,
    onClick: (Int) -> Unit = {},
    desaturate: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(
                end = dimensionResource(id = R.dimen.keyline_first)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.TEA_importance_label),
            style = MaterialTheme.typography.body1,
            modifier = Modifier.padding(end = 16.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        Priority(selected = selected, onClick = onClick, desaturate = desaturate)
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RowScope.PriorityButton(
    @Task.Priority priority: Int,
    selected: Int,
    desaturate: Boolean,
    onClick: (Int) -> Unit,
) {
    val color = Color(
        priorityColor(
            priority = priority,
            isDarkMode = isSystemInDarkTheme(),
            desaturate = desaturate,
        )
    )
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentEnforcement provides false,
    ) {
        RadioButton(
            selected = priority == selected,
            onClick = { onClick(priority) },
            colors = RadioButtonDefaults.colors(
                selectedColor = color,
                unselectedColor = color,
            ),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 20.dp)
        )
    }
}

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PriorityPreview() {
    MdcTheme {
        PriorityRow(
            priority = Task.Priority.MEDIUM,
            onChangePriority = {},
            desaturate = true,
        )
    }
}

@ExperimentalComposeUiApi
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PriorityPreviewNoDesaturate() {
    MdcTheme {
        PriorityRow(
            priority = Task.Priority.MEDIUM,
            onChangePriority = {},
            desaturate = false,
        )
    }
}

@ExperimentalComposeUiApi
@Preview(locale = "de", widthDp = 320, showBackground = true)
@Composable
fun PriorityNarrowWidth() {
    MdcTheme {
        PriorityRow(
            priority = Task.Priority.MEDIUM,
            onChangePriority = {},
            desaturate = false,
        )
    }
}
