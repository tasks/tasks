package org.tasks.compose.pickers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.components.TasksIcon
import org.tasks.data.entity.TagData
import org.tasks.tags.TagPickerViewModel
import org.tasks.themes.TasksIcons
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.enter_tag_name
import tasks.kmp.generated.resources.new_tag

@Composable
fun TagPicker(
    viewModel: TagPickerViewModel,
    onBackClicked: () -> Unit,
    getTagIcon: (TagData) -> String,
    getTagColor: (TagData) -> Color,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                TagSearchBar(viewModel, onBackClicked)
            }
            Box(modifier = Modifier.weight(1f)) {
                PickerBox(
                    viewModel = viewModel,
                    getTagIcon = getTagIcon,
                    getTagColor = getTagColor,
                )
            }
        }
    }
}

@Composable
private fun TagSearchBar(
    viewModel: TagPickerViewModel,
    onBack: () -> Unit,
) {
    val searchPattern = remember { viewModel.searchText }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = null,
            modifier = Modifier
                .padding(6.dp)
                .clickable { onBack() },
            tint = MaterialTheme.colorScheme.onSurface,
        )

        TextField(
            value = searchPattern.value,
            onValueChange = { viewModel.search(it) },
            placeholder = {
                Text(
                    text = stringResource(Res.string.enter_tag_name),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            colors = TextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            textStyle = TextStyle(
                textDirection = TextDirection.Content
            ),
            modifier = Modifier.padding(start = 6.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
            )
        )
    }
}

@Composable
private fun PickerBox(
    viewModel: TagPickerViewModel,
    getTagIcon: (TagData) -> String,
    getTagColor: (TagData) -> Color,
) {
    val onClick: (TagData) -> Unit = {
        viewModel.viewModelScope.launch {
            viewModel.toggle(it, viewModel.getState(it) != ToggleableState.On)
        }
    }

    val newItem: (String) -> Unit = {
        viewModel.viewModelScope.launch { viewModel.createNew(it); viewModel.search("") }
    }

    LazyColumn {
        if (viewModel.tagToCreate.value != "") {
            item(key = -1) {
                TagRow(
                    icon = TasksIcons.ADD,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = "${stringResource(Res.string.new_tag)} \"${viewModel.tagToCreate.value}\"",
                    onClick = { newItem(viewModel.searchText.value) }
                )
            }
        }

        items(viewModel.tags.value, key = { tag -> tag.id!! }) {
            val checked = remember { mutableStateOf(viewModel.getState(it)) }
            val clickChecked: () -> Unit = { onClick(it); checked.value = viewModel.getState(it) }
            TagRow(
                icon = getTagIcon(it),
                iconColor = getTagColor(it),
                text = it.name!!,
                onClick = clickChecked
            ) {
                TriStateCheckbox(
                    modifier = Modifier.padding(6.dp),
                    state = checked.value,
                    onClick = clickChecked
                )
            }
        }
    }
}

@Composable
private fun TagRow(
    icon: String,
    iconColor: Color,
    text: String,
    onClick: () -> Unit,
    checkBox: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TasksIcon(
            modifier = Modifier.padding(6.dp),
            label = icon,
            tint = iconColor.takeOrElse { MaterialTheme.colorScheme.onSurfaceVariant }
        )
        Text(
            text,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        checkBox()
    }
}
