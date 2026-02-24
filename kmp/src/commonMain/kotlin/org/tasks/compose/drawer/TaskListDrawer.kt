package org.tasks.compose.drawer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PermIdentity
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.components.Chevron
import org.tasks.compose.components.SearchBar
import org.tasks.compose.components.TasksIcon
import org.tasks.kmp.formatNumber
import org.tasks.kmp.org.tasks.compose.rememberImeState
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.search

@Composable
fun TaskListDrawer(
    arrangement: Arrangement.Vertical,
    filters: ImmutableList<DrawerItem>,
    onClick: (DrawerItem) -> Unit,
    onAddClick: (DrawerItem.Header) -> Unit,
    onErrorClick: () -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    searchExpanded: Boolean,
    onSearchExpandedChange: (Boolean) -> Unit,
) {
    val systemBarPadding = WindowInsets.systemBars.asPaddingValues()
    val keyboardOpen = rememberImeState().value
    val bottomInset = if (keyboardOpen) 0.dp else systemBarPadding.calculateBottomPadding()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = systemBarPadding.calculateTopPadding(),
                bottom = 88.dp + bottomInset,
            ),
            verticalArrangement = arrangement,
        ) {
            itemsIndexed(items = filters, key = { _, it -> it.key() }) { index, item ->
                val isFirst = index == 0 || item is DrawerItem.Header
                val isLast = index == filters.lastIndex ||
                        filters[index + 1] is DrawerItem.Header
                val cornerRadius = 16.dp
                val shape = when {
                    isFirst && isLast -> RoundedCornerShape(cornerRadius)
                    isFirst -> RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
                    isLast -> RoundedCornerShape(bottomStart = cornerRadius, bottomEnd = cornerRadius)
                    else -> RoundedCornerShape(0.dp)
                }
                Surface(
                    modifier = Modifier
                        .padding(
                            start = 8.dp,
                            end = 8.dp,
                            top = if (isFirst && index > 0) 8.dp else 0.dp,
                        ),
                    shape = shape,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                ) {
                    when (item) {
                        is DrawerItem.Filter -> FilterItem(
                            item = item,
                            onClick = { onClick(item) }
                        )
                        is DrawerItem.Header -> HeaderItem(
                            item = item,
                            canAdd = item.canAdd,
                            toggleCollapsed = { onClick(item) },
                            onAddClick = { onAddClick(item) },
                            onErrorClick = onErrorClick,
                        )
                    }
                }
            }
        }
        SearchFab(
            expanded = searchExpanded,
            onExpandedChange = onSearchExpandedChange,
            query = query,
            onQueryChange = onQueryChange,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    bottom = 16.dp + bottomInset,
                ),
        )
    }
}

@Composable
private fun SearchFab(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    val containerColor by animateColorAsState(
        targetValue = if (expanded) MaterialTheme.colorScheme.surfaceContainerHigh
                      else MaterialTheme.colorScheme.primary,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "containerColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (expanded) MaterialTheme.colorScheme.onSurface
                      else MaterialTheme.colorScheme.onPrimary,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "contentColor",
    )
    val elevation by animateDpAsState(
        targetValue = if (expanded) 0.dp else 6.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "elevation",
    )

    Surface(
        onClick = { if (!expanded) onExpandedChange(true) },
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = elevation,
        modifier = modifier.padding(horizontal = 16.dp),
    ) {
        AnimatedContent(
            targetState = expanded,
            transitionSpec = {
                fadeIn(spring(stiffness = Spring.StiffnessMedium))
                    .togetherWith(fadeOut(spring(stiffness = Spring.StiffnessMedium)))
                    .using(SizeTransform(clip = true) { _, _ ->
                        spring(stiffness = Spring.StiffnessMedium)
                    })
            },
            contentAlignment = Alignment.CenterEnd,
        ) { isExpanded ->
            if (isExpanded) {
                SearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    text = query,
                    onTextChange = { onQueryChange(it) },
                    placeHolder = stringResource(Res.string.search),
                    onCloseClicked = {
                        if (query.isNotEmpty()) {
                            onQueryChange("")
                        } else {
                            onExpandedChange(false)
                        }
                    },
                    onSearchClicked = {},
                )
            } else {
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = stringResource(Res.string.search),
                    )
                }
            }
        }
    }

    LaunchedEffect(expanded) {
        if (expanded) {
            delay(100)
            focusRequester.requestFocus()
        }
    }
}

@Composable
internal fun FilterItem(
    modifier: Modifier = Modifier,
    item: DrawerItem.Filter,
    onClick: () -> Unit,
) {
    MenuRow(
        modifier = modifier
            .background(
                if (item.selected)
                    MaterialTheme.colorScheme.surfaceContainerHigh
                else
                    Color.Transparent
            )
            .clickable(onClick = onClick),
        onClick = onClick,
    ) {
        TasksIcon(
            label = item.icon,
            tint = when (item.color) {
                0 -> MaterialTheme.colorScheme.onSurface
                else -> Color(color = item.color)
            }
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = item.title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
        if (item.shareCount > 0) {
            Icon(
                imageVector = when (item.shareCount) {
                    1 -> Icons.Outlined.PermIdentity
                    else -> Icons.Outlined.PeopleOutline
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Box(
            contentAlignment = Alignment.CenterEnd,
        ) {
            if (item.count > 0) {
                Text(
                    text = formatNumber(item.count),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
internal fun HeaderItem(
    modifier: Modifier = Modifier,
    item: DrawerItem.Header,
    canAdd: Boolean,
    toggleCollapsed: () -> Unit,
    onAddClick: () -> Unit,
    onErrorClick: () -> Unit,
) {
    MenuRow(
        modifier = modifier,
        padding = PaddingValues(start = 16.dp),
        onClick = if (item.hasChildren) toggleCollapsed else null,
    ) {
            val accountIcon = item.header.accountIcon
            if (accountIcon != null) {
                Image(
                    painter = painterResource(accountIcon.drawable),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    colorFilter = if (accountIcon.tinted) {
                        ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                    } else {
                        null
                    },
                )
            } else {
                TasksIcon(
                    label = item.header.icon,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                modifier = Modifier.weight(1f),
                text = item.title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.hasChildren) {
                IconButton(onClick = toggleCollapsed) {
                    Chevron(item.collapsed)
                }
            }
            if (canAdd) {
                IconButton(onClick = onAddClick) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            if (item.hasError) {
                IconButton(onClick = onErrorClick) {
                    Icon(
                        imageVector = Icons.Outlined.SyncProblem,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
    }
}

@Composable
private fun MenuRow(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = 16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .height(48.dp)
            .padding(padding)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
