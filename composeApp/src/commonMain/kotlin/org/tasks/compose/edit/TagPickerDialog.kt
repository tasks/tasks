package org.tasks.compose.edit

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.tasks.compose.pickers.TagPicker
import org.tasks.data.entity.TagData
import org.tasks.tags.TagPickerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagPickerDialog(
    viewModel: TagPickerViewModel,
    initialTags: List<TagData>,
    getTagIcon: (TagData) -> String,
    getTagColor: (TagData) -> Color,
    onDismiss: (List<TagData>) -> Unit,
) {
    LaunchedEffect(viewModel) {
        viewModel.setSelected(initialTags, null)
        viewModel.search("")
    }
    val commit = { onDismiss(viewModel.getSelected()) }
    BasicAlertDialog(
        onDismissRequest = commit,
        modifier = Modifier
            .padding(vertical = 32.dp)
            .fillMaxWidth()
            .heightIn(max = 500.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            TagPicker(
                viewModel = viewModel,
                onBackClicked = {
                    if (viewModel.searchText.value.isEmpty()) {
                        commit()
                    } else {
                        viewModel.search("")
                    }
                },
                getTagIcon = getTagIcon,
                getTagColor = getTagColor,
            )
        }
    }
}
