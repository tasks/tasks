package org.tasks.compose.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PermIdentity
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.components.SearchBar
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.filters.FilterListItem
import org.tasks.filters.NavigationDrawerSubheader
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.search

@Composable
fun SearchableFilterPicker(
    filters: List<FilterListItem>,
    query: String,
    onQueryChange: (String) -> Unit,
    selected: Filter?,
    onClick: (FilterListItem) -> Unit,
    getIcon: @Composable (Filter) -> ImageVector,
    getColor: (Filter) -> Int,
) {
    LazyColumn(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium
            ),
        contentPadding = PaddingValues(top = 16.dp),
    ) {
        item {
            SearchBar(
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp, bottom = 4.dp)
                    .fillMaxWidth(),
                text = query,
                onTextChange = { onQueryChange(it) },
                placeHolder = stringResource(Res.string.search),
                onCloseClicked = { onQueryChange("") },
                onSearchClicked = {
                    // TODO: close keyboard
                }
            )
        }
        items(filters) { filter ->
            when (filter) {
                is NavigationDrawerSubheader -> {
                    CollapsibleRow(
                        text = filter.title!!,
                        collapsed = filter.isCollapsed,
                        onClick = { onClick(filter) },
                    )
                }

                is Filter -> {
                    CheckableIconRow(
                        icon = getIcon(filter),
                        tint = remember(filter) { Color(getColor(filter)) },
                        selected = filter == selected,
                        onClick = { onClick(filter) },
                    ) {
                        Row(verticalAlignment = CenterVertically) {
                            Text(
                                text = filter.title!!,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                                modifier = Modifier.weight(1f),
                            )
                            if (filter is CaldavFilter && filter.principals > 0) {
                                Icon(
                                    imageVector = when (filter.principals) {
                                        in 2..Int.MAX_VALUE -> Icons.Outlined.PeopleOutline
                                        else -> Icons.Outlined.PermIdentity
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
