package org.tasks.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow

@Composable
fun ChipGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    FlowRow(
        mainAxisSpacing = 4.dp,
        crossAxisSpacing = 4.dp,
        modifier = modifier,
    ) {
        content()
    }
}
