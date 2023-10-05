package org.tasks.compose.pickers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.todoroo.astrid.api.CaldavFilter
import com.todoroo.astrid.api.Filter
import org.tasks.R
import org.tasks.compose.collectAsStateLifecycleAware
import org.tasks.dialogs.FilterPickerViewModel
import org.tasks.filters.NavigationDrawerSubheader

@Composable
fun FilterPicker(
    viewModel: FilterPickerViewModel = viewModel(),
    selected: Filter?,
    onSelected: (Filter) -> Unit,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp)
    ) {
        val filters = viewModel.viewState.collectAsStateLifecycleAware().value.filters
        filters.forEach { filter ->
            when (filter) {
                is NavigationDrawerSubheader -> {
                    CollapsibleRow(
                        text = filter.title!!,
                        collapsed = filter.isCollapsed,
                        onClick = { viewModel.onClick(filter) },
                    )
                }
                is Filter -> {
                    CheckableIconRow(
                        icon = painterResource(id = viewModel.getIcon(filter)),
                        tint = Color(viewModel.getColor(filter)),
                        selected = filter == selected,
                        onClick = { onSelected(filter) },
                    ) {
                        Row(verticalAlignment = CenterVertically) {
                            Text(
                                text = filter.title!!,
                                style = MaterialTheme.typography.body2.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.weight(1f),
                            )
                            if (filter is CaldavFilter && filter.principals > 0) {
                                Icon(
                                    painter = painterResource(
                                        id = when (filter.principals) {
                                            1 -> R.drawable.ic_outline_perm_identity_24px
                                            in 2..Int.MAX_VALUE -> R.drawable.ic_outline_people_outline_24
                                            else -> 0
                                        }
                                    ),
                                    modifier = Modifier.alpha(ContentAlpha.medium),
                                    contentDescription = null,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}