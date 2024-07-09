package org.tasks.compose.pickers

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.components.AnimatedBanner
import org.tasks.compose.components.Chevron
import org.tasks.compose.components.SearchBar
import org.tasks.compose.components.TasksIcon
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.requires_pro_subscription
import tasks.kmp.generated.resources.search
import tasks.kmp.generated.resources.search_no_results
import tasks.kmp.generated.resources.subscribe
import tasks.kmp.generated.resources.subscription_required_description

private val gridSize = 48.dp
private val verticalSpacing = 8.dp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun IconPicker(
    icons: ImmutableMap<String, ImmutableList<Icon>>,
    searchResults: ImmutableList<Icon>,
    collapsed: ImmutableSet<String>,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelected: (Icon) -> Unit,
    toggleCollapsed: (String, Boolean) -> Unit,
    hasPro: Boolean,
    subscribe: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val numColumns = maxOf(1, (maxWidth / gridSize).toInt())
        val width = numColumns * gridSize
        val horizontalPadding = (maxWidth - width) / 2
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 48.dp),
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium
                ),
            contentPadding = PaddingValues(horizontalPadding),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SearchBar(
                    modifier = Modifier.fillMaxWidth(),
                    text = query,
                    onTextChange = { onQueryChange(it) },
                    placeHolder = stringResource(Res.string.search),
                    onCloseClicked = { onQueryChange("") },
                    onSearchClicked = {
                        // TODO: close keyboard
                    }
                )
            }
            if (!hasPro) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AnimatedBanner(
                        true,
                        title = stringResource(Res.string.requires_pro_subscription),
                        body = stringResource(Res.string.subscription_required_description),
                        action = stringResource(Res.string.subscribe),
                        onAction = { subscribe() },
                        onDismiss = {},
                    )
                }
            }
            if (query.isNotBlank()) {
                if (searchResults.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(Res.string.search_no_results),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                } else {
                    iconGrid(
                        icons = searchResults,
                        onSelected = { if (hasPro) onSelected(it) else subscribe() },
                    )
                }
            } else {
                icons.forEach { (category, items) ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable { toggleCollapsed(category, !collapsed.contains(category)) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = if (category == "av") {
                                    category.uppercase()
                                } else {
                                    category.uppercaseFirstLetter()
                                },
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Chevron(remember(collapsed, category) { collapsed.contains(category) })
                        }
                    }
                    if (!collapsed.contains(category)) {
                        iconGrid(
                            icons = items,
                            onSelected = { if (hasPro) onSelected(it) else subscribe() },
                        )
                    }
                }
            }
        }
    }
}

fun LazyGridScope.iconGrid(
    icons: ImmutableList<Icon>,
    onSelected: (Icon) -> Unit,
) {
    items(icons, key = { it.name }) { icon ->
        IconButton(onClick = { onSelected(icon) }) {
            TasksIcon(label = icon.name)
        }
    }
}
