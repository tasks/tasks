package org.tasks.compose

/**
 *  Composables for FilterSettingActivity
 **/

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Abc
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.os.ConfigurationCompat
import com.todoroo.astrid.core.CriterionInstance
import org.tasks.R
import org.tasks.compose.ListSettings.SettingRow
import org.tasks.compose.SwipeOut.SwipeOut
import org.tasks.extensions.formatNumber
import org.tasks.themes.TasksTheme
import java.util.Locale

@Composable
@Preview (showBackground = true)
private fun CriterionTypeSelectPreview () {
    TasksTheme {
        FilterCondition.SelectCriterionType(
            title = "Select criterion type",
            selected = 1,
            types = listOf("AND", "OR", "NOT"),
            onCancel = { /*TODO*/ }) {
        }
    }
}

@Composable
@Preview (showBackground = true)
private fun InputTextPreview () {
    TasksTheme {
        FilterCondition.InputTextOption(title = "Task name contains...", onCancel = { /*TODO*/ }
        ) {

        }
    }
}

@Composable
@Preview (showBackground = true)
private fun SwipeOutDecorationPreview () {
    TasksTheme {
        Box(modifier = Modifier
            .height(56.dp)
            .fillMaxWidth()) {
            FilterCondition.SwipeOutDecoration()
        }
    }
}

@Composable
@Preview (showBackground = true)
private fun FabPreview () {
    TasksTheme {
        FilterCondition.NewCriterionFAB(
            isExtended = remember { mutableStateOf(true) }
        ) {

        }
    }
}

object FilterCondition {
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun FilterCondition(
        items: SnapshotStateList<CriterionInstance>,
        onDelete: (Int) -> Unit,
        doSwap: (Int, Int) -> Unit,
        onClick: (String) -> Unit
    ) {

        val getIcon: (CriterionInstance) -> Int = { criterion ->
            when (criterion.type) {
                CriterionInstance.TYPE_ADD -> R.drawable.ic_call_split_24px
                CriterionInstance.TYPE_SUBTRACT -> R.drawable.ic_outline_not_interested_24px
                CriterionInstance.TYPE_INTERSECT -> R.drawable.ic_outline_add_24px
                else -> {
                    0
                }  /* assert */
            }
        }
        val listState = rememberLazyListState()
        val dragDropState = rememberDragDropState(
            lazyListState = listState,
            confirmDrag = { index -> index != 0 }
        ) { fromIndex, toIndex ->
            if (fromIndex != 0 && toIndex != 0) doSwap(fromIndex, toIndex)
        }

        Row {
            Text(
                text = stringResource(id = R.string.custom_filter_criteria),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Constants.KEYLINE_FIRST)
            )
        }
        Row {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .doDrag(dragDropState),
                userScrollEnabled = true,
                state = listState
            ) {
                itemsIndexed(
                    items = items,
                    key = { _, item -> item.id }
                ) { index, criterion ->
                    if (index == 0) {
                        FilterConditionRow(criterion, false, getIcon, onClick)
                    } else {
                        DraggableItem(
                            dragDropState = dragDropState, index = index
                        ) { dragging ->
                            SwipeOut(
                                decoration = { SwipeOutDecoration() },
                                onSwipe = { index -> onDelete(index) },
                                index = index
                            ) {
                                FilterConditionRow(criterion, dragging, getIcon, onClick)
                            }
                        }
                    }
                }
            }
        }
    } /* FilterCondition */

    @Composable
    private fun FilterConditionRow(
        criterion: CriterionInstance,
        dragging: Boolean,
        getIcon: (CriterionInstance) -> Int,
        onClick: (String) -> Unit
    ) {
        HorizontalDivider(
            color = when (criterion.type) {
                CriterionInstance.TYPE_ADD -> Color.Gray
                else -> Color.Transparent
            }
        )
        val modifier =
            if (dragging) Modifier.background(Color.LightGray)
            else Modifier
        SettingRow(
            modifier = modifier.clickable { onClick(criterion.id) },
            left = {
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                )
                {
                    if (criterion.type != CriterionInstance.TYPE_UNIVERSE) {
                        Icon(
                            modifier = Modifier.padding(Constants.KEYLINE_FIRST),
                            painter = painterResource(id = getIcon(criterion)),
                            contentDescription = null
                        )
                    }
                }
            },
            center = {
                Text(
                    text = criterion.titleFromCriterion,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp)
                )
            },
            right = {
                val context = LocalContext.current
                val locale = remember {
                    ConfigurationCompat
                        .getLocales(context.resources.configuration)
                        .get(0)
                        ?: Locale.getDefault()
                }
                Text(
                    text = locale.formatNumber(criterion.max),
                    modifier = Modifier.padding(end = Constants.KEYLINE_FIRST),
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.End
                )
            }
        )
    }

    @Composable
    fun SwipeOutDecoration() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(id = org.tasks.kmp.R.color.red_a400))
                //.background(MaterialTheme.colorScheme.secondary)
        ) {

            @Composable
            fun deleteIcon() {
                Icon(
                    modifier = Modifier.padding(horizontal = Constants.KEYLINE_FIRST),
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .height(56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                deleteIcon()
                deleteIcon()
            }
        }
    } /* end SwipeOutDecoration */

    @Composable
    fun NewCriterionFAB(
        isExtended: MutableState<Boolean>,
        onClick: () -> Unit
    ) {

        Box( // lays out over main content as a space to layout FAB
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = onClick,
                modifier = Modifier.padding(16.dp),
                shape = RoundedCornerShape(50),
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = Color.White,
            ) {
                val extended = isExtended.value

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "New Criteria",
                        modifier = Modifier.padding(
                            start = if (extended) 16.dp else 0.dp
                        )
                    )
                    if (extended)
                        Text(
                            text = LocalContext.current.getString(R.string.CFA_button_add),
                            modifier = Modifier.padding(end = 16.dp)
                        )
                }
            } /* end FloatingActionButton */
        }
    } /* end NewCriterionFAB */

    @Composable
    fun SelectCriterionType(
        title: String,
        selected: Int,
        types: List<String>,
        onCancel: () -> Unit,
        help: () -> Unit = {},
        onSelected: (Int) -> Unit
    ) {
        val selected = remember { mutableIntStateOf(selected) }

        Dialog(onDismissRequest = onCancel)
        {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
            ) {
                Column(modifier = Modifier
                    .padding(horizontal = Constants.KEYLINE_FIRST)
                    .padding(top = Constants.HALF_KEYLINE)
                ) {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(top = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(Constants.HALF_KEYLINE))
                    ToggleGroup(items = types, selected = selected)
                    Row(
                        modifier = Modifier.height(48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.CenterStart) {
                            Constants.TextButton(text = R.string.help, onClick = help)
                        }
                        Box(
                            contentAlignment = Alignment.CenterEnd,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row {
                                Constants.TextButton(text = R.string.cancel, onClick = onCancel)
                                Constants.TextButton(text = R.string.ok) { onSelected(selected.intValue) }
                            }
                        }
                    }
                }
            }
        }
    } /* end SelectCriterionType */

    @Composable
    fun ToggleGroup(
        items: List<String>,
        selected: MutableIntState = remember { mutableIntStateOf(0) }
    ) {
        assert(selected.intValue in items.indices)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            contentAlignment = Alignment.Center
        ) {
            Row {
                for (index in items.indices) {
                    val highlight = (index == selected.intValue)
                    val color =
                        if (highlight) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    OutlinedButton(
                        onClick = { selected.intValue = index },
                        border = BorderStroke(1.dp, SolidColor(color.copy(alpha = 0.5f))),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = color.copy(alpha = 0.2f),
                            contentColor = MaterialTheme.colorScheme.onBackground),
                        shape = RoundedCornerShape(Constants.HALF_KEYLINE)
                    ) {
                        Text(items[index])
                    }
                    if (index<items.size-1) Spacer(modifier = Modifier.size(2.dp))
                }
            }
        }
    } /* end ToggleGroup */


    @Composable
    fun SelectFromList(
        names: List<String>,
        title: String? = null,
        onCancel: () -> Unit,
        onSelected: (Int) -> Unit
    ) {
        Dialog(onDismissRequest = onCancel) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = Constants.KEYLINE_FIRST)
                        .padding(bottom = Constants.KEYLINE_FIRST)
                ) {
                    title?.let { title ->
                        Text(
                            text = title,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .padding(top = Constants.KEYLINE_FIRST)
                        )
                    }
                    names.forEachIndexed { index, name ->
                        Text(
                            text = name,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .padding(top = Constants.KEYLINE_FIRST)
                                .clickable { onSelected(index) }
                        )
                    }
                }
            }
        }
    } /* end SelectFromList */


    @Composable
    fun InputTextOption(
        title: String,
        onCancel: () -> Unit,
        onDone: (String) -> Unit
    ) {
        val text = remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = onCancel,
            confirmButton = {
                Constants.TextButton(
                    text = R.string.ok,
                    onClick = { onDone(text.value) })
            },
            dismissButton = { Constants.TextButton(text = R.string.cancel, onClick = onCancel) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(Modifier.height(Constants.KEYLINE_FIRST))
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = text.value,
                        label = { Text(title) },
                        onValueChange = { text.value = it },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Abc,
                                contentDescription = null
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = Constants.textFieldColors(),
                    )
                }

            }
        )
    } /* end InputTextOption */
}