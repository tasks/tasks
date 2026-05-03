package org.tasks.compose.drawer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PermIdentity
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.components.Chevron
import org.tasks.compose.components.SearchBar
import org.tasks.compose.components.TasksIcon
import org.tasks.kmp.formatNumber
import androidx.compose.foundation.isSystemInDarkTheme
import org.tasks.themes.ColorTone
import org.tasks.themes.tonalColor
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.search

@Composable
fun TaskListDrawer(
    drawerOpen: Boolean,
    drawerState: org.tasks.viewmodel.DrawerViewModel.State,
    onQueryChange: (String) -> Unit,
    onClick: (DrawerItem) -> Unit,
    onAddClick: (DrawerItem.Header) -> Unit,
    onErrorClick: () -> Unit,
    expanded: Boolean = true,
    listState: LazyListState = rememberLazyListState(),
) {
    var searchExpanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(drawerOpen) {
        if (!drawerOpen) {
            searchExpanded = false
            query = ""
            onQueryChange("")
        }
    }

    val arrangement = if (searchExpanded && query.isNotBlank()) {
        Arrangement.Bottom
    } else {
        Arrangement.Top
    }
    val displayedFilters = if (query.isNotBlank()) drawerState.searchItems else drawerState.drawerItems
    val systemBarPadding = WindowInsets.systemBars.asPaddingValues()
    val bottomNavPadding = systemBarPadding.calculateBottomPadding()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .consumeWindowInsets(PaddingValues(bottom = bottomNavPadding))
            .imePadding(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(
                top = maxOf(systemBarPadding.calculateTopPadding(), 8.dp),
                bottom = 88.dp + bottomNavPadding,
            ),
            verticalArrangement = arrangement,
        ) {
            itemsIndexed(items = displayedFilters, key = { _, it -> it.key() }) { index, item ->
                val isFirst = index == 0 || item is DrawerItem.Header
                val isLast = index == displayedFilters.lastIndex ||
                        displayedFilters[index + 1] is DrawerItem.Header
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
                            expanded = expanded,
                            onClick = { onClick(item) }
                        )
                        is DrawerItem.Header -> HeaderItem(
                            item = item,
                            expanded = expanded,
                            canAdd = item.canAdd,
                            toggleCollapsed = { onClick(item) },
                            onAddClick = { onAddClick(item) },
                            onErrorClick = onErrorClick,
                        )
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd),
        ) {
        SearchFab(
            expanded = searchExpanded,
            onExpandedChange = { searchExpanded = it },
            query = query,
            onQueryChange = { newQuery ->
                query = newQuery
                onQueryChange(newQuery)
            },
            modifier = Modifier
                .padding(bottom = 16.dp + bottomNavPadding),
        )
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterItem(
    modifier: Modifier = Modifier,
    item: DrawerItem.Filter,
    expanded: Boolean = true,
    onClick: () -> Unit,
) {
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()
    var hoverJob by remember { mutableStateOf<Job?>(null) }
    TooltipBox(
        positionProvider = endTooltipPositionProvider(LocalLayoutDirection.current),
        tooltip = { if (!expanded) PlainTooltip { Text(item.title) } },
        state = tooltipState,
        enableUserInput = false,
    ) {
    MenuRow(
        modifier = modifier
            .pointerInput(expanded) {
                if (expanded) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> {
                                hoverJob = scope.launch {
                                    delay(1000)
                                    tooltipState.show()
                                }
                            }
                            PointerEventType.Exit -> {
                                hoverJob?.cancel()
                                tooltipState.dismiss()
                            }
                        }
                    }
                }
            }
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
            tint = when {
                item.color == 0 -> MaterialTheme.colorScheme.onSurface
                item.adjustColor && isSystemInDarkTheme() ->
                    Color(tonalColor(item.color, ColorTone.DARK_DRAWER))
                item.adjustColor ->
                    Color(tonalColor(item.color, ColorTone.LIGHT_DRAWER))
                else -> Color(item.color)
            }
        )
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
    }
    }
}

@Composable
internal fun HeaderItem(
    modifier: Modifier = Modifier,
    item: DrawerItem.Header,
    expanded: Boolean = true,
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
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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

private fun endTooltipPositionProvider(layoutDirection: LayoutDirection) =
    object : PopupPositionProvider {
        override fun calculatePosition(
            anchorBounds: IntRect,
            windowSize: IntSize,
            layoutDirection: LayoutDirection,
            popupContentSize: IntSize,
        ): IntOffset {
            val x = if (layoutDirection == LayoutDirection.Ltr)
                anchorBounds.right
            else
                anchorBounds.left - popupContentSize.width
            val y = anchorBounds.top + (anchorBounds.height - popupContentSize.height) / 2
            return IntOffset(x, y)
        }
    }
