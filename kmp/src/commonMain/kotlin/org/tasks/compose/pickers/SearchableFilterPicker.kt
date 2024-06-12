package org.tasks.compose.pickers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PermIdentity
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.filters.FilterListItem
import org.tasks.filters.NavigationDrawerSubheader
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.search

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableFilterPicker(
    filters: List<FilterListItem>,
    query: String,
    onQueryChange: (String) -> Unit,
    active: Boolean,
    selected: Filter?,
    onClick: (FilterListItem) -> Unit,
    getIcon: @Composable (Filter) -> ImageVector,
    getColor: (Filter) -> Int,
    dismiss: () -> Unit,
) {
    val searching by remember(query) {
        derivedStateOf {
            query.isNotBlank()
        }
    }
    val filtered by remember (filters, query) {
        derivedStateOf {
            filters.filter {
                when (it) {
                    is NavigationDrawerSubheader -> true
                    is Filter -> {
                        it.title!!.contains(query, ignoreCase = true)
                    }
                    else -> throw IllegalArgumentException()
                }
            }
        }
    }
    SearchBar(
        colors = SearchBarDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            inputFieldColors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            )
        ),
        query = query,
        onQueryChange = onQueryChange,
        onSearch = {
            // TODO: close keyboard?
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
            )
        },
        placeholder = {
            Text(
                text = stringResource(Res.string.search),
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        trailingIcon = {
            if (searching) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = "Clear query",
                    )
                }
            }
        },
        active = active,
        onActiveChange = {
            when {
                it -> {}
                query.isNotBlank() -> onQueryChange("")
                else -> dismiss()
            }
        },
        content = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                filtered.forEach { filter ->
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
                                            fontWeight = FontWeight.Medium
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
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}
