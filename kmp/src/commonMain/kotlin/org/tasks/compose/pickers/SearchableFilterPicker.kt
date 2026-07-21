package org.tasks.compose.pickers

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.components.SearchBar
import org.tasks.compose.drawer.DrawerItem
import org.tasks.compose.drawer.FilterItem
import org.tasks.compose.drawer.HeaderItem
import org.tasks.compose.drawer.SignInItem
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.filters.FilterListItem
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.filters.SignInPrompt
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.search

@Composable
fun SearchableFilterPicker(
    filters: List<FilterListItem>,
    query: String,
    onQueryChange: (String) -> Unit,
    selected: Filter?,
    onClick: (FilterListItem) -> Unit,
    getIcon: @Composable (Filter) -> String?,
    getColor: (Filter) -> Int,
    onAddClick: (NavigationDrawerSubheader) -> Unit = {},
    onSignIn: () -> Unit = {},
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        val displayedFilters = remember(filters) {
            filters.filterNot {
                it is NavigationDrawerSubheader &&
                        it.childCount == 0 &&
                        it.subheaderType == NavigationDrawerSubheader.SubheaderType.PREFERENCE
            }
        }
        val cornerRadius = 16.dp
        val islandSpacing = 8.dp
        LazyColumn(
            contentPadding = PaddingValues(
                top = 16.dp,
                start = 8.dp,
                end = 8.dp,
                bottom = islandSpacing
            ),
        ) {
            item(key = "search") {
                SearchBar(
                    modifier = Modifier
                        .padding(bottom = islandSpacing)
                        .fillMaxWidth(),
                    text = query,
                    onTextChange = { onQueryChange(it) },
                    placeHolder = stringResource(Res.string.search),
                    onCloseClicked = { onQueryChange("") },
                    onSearchClicked = {}
                )
            }
            itemsIndexed(
                items = displayedFilters,
                key = { _, item ->
                    when (item) {
                        is NavigationDrawerSubheader -> "header_${item.subheaderType}_${item.id}"
                        is SignInPrompt -> "sign_in"
                        is Filter -> "filter_${item.title}_${item.sql}"
                        else -> item.hashCode()
                    }
                },
            ) { index, item ->
                val isFirst = index == 0 ||
                        item is NavigationDrawerSubheader ||
                        item is SignInPrompt
                val isLast = index == displayedFilters.lastIndex ||
                        displayedFilters[index + 1] is NavigationDrawerSubheader ||
                        displayedFilters[index + 1] is SignInPrompt
                val shape = when {
                    isFirst && isLast -> RoundedCornerShape(cornerRadius)
                    isFirst -> RoundedCornerShape(
                        topStart = cornerRadius,
                        topEnd = cornerRadius,
                    )

                    isLast -> RoundedCornerShape(
                        bottomStart = cornerRadius,
                        bottomEnd = cornerRadius,
                    )

                    else -> RoundedCornerShape(0.dp)
                }
                Surface(
                    modifier = Modifier.padding(
                        top = if (isFirst && index > 0) islandSpacing else 0.dp,
                    ),
                    shape = shape,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                ) {
                    when (item) {
                        is NavigationDrawerSubheader -> {
                            val drawerHeader = remember(item) {
                                DrawerItem.Header(
                                    title = item.title ?: "",
                                    collapsed = item.isCollapsed,
                                    hasError = item.error,
                                    canAdd = item.addIntentRc != 0,
                                    hasChildren = item.childCount > 0,
                                    openTaskApp = item.openTaskApp,
                                    header = item,
                                )
                            }
                            HeaderItem(
                                item = drawerHeader,
                                toggleCollapsed = { onClick(item) },
                                onAddClick = { onAddClick(item) },
                                onErrorClick = {},
                            )
                        }

                        is SignInPrompt -> SignInItem(onClick = onSignIn)

                        is Filter -> {
                            val icon = getIcon(item)
                            val drawerFilter = remember(item, selected, icon) {
                                DrawerItem.Filter(
                                    title = item.title,
                                    icon = icon,
                                    color = getColor(item),
                                    count = 0,
                                    selected = selected?.let { item.areItemsTheSame(it) } ?: false,
                                    shareCount = if (item is CaldavFilter) item.principals else 0,
                                    filter = item,
                                )
                            }
                            FilterItem(
                                item = drawerFilter,
                                onClick = { onClick(item) },
                            )
                        }
                    }
                }
            }
        }
    }
}

