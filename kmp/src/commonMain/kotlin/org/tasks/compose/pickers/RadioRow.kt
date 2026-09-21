package org.tasks.compose.pickers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val DialogHorizontalPadding = 20.dp
private val RadioButtonVisualInset = 14.dp

val RadioRowPadding = PaddingValues(
    start = DialogHorizontalPadding - RadioButtonVisualInset,
    end = DialogHorizontalPadding,
)

@Composable
fun RadioRow(
    selected: Boolean,
    onClick: () -> Unit,
    contentPadding: PaddingValues = RadioRowPadding,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(contentPadding),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
