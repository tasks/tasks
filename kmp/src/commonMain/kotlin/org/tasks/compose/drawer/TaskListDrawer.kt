package org.tasks.compose.drawer

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PermIdentity
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.components.Chevron
import org.tasks.compose.components.SearchBar
import org.tasks.compose.components.TasksIcon
import org.tasks.kmp.formatNumber
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.help_and_feedback
import tasks.kmp.generated.resources.search
import tasks.kmp.generated.resources.settings
import tasks.kmp.generated.resources.subscribe
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListDrawer(
    bottomSearchBar: Boolean,
    filters: ImmutableList<DrawerItem>,
    onClick: (DrawerItem) -> Unit,
    onAddClick: (DrawerItem.Header) -> Unit,
    onErrorClick: () -> Unit,
    searchBar: @Composable RowScope.() -> Unit,
) {
    val bottomAppBarScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()
    val topAppBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier
            .nestedScroll(
                if (bottomSearchBar)
                    bottomAppBarScrollBehavior.nestedScrollConnection
                else
                    topAppBarScrollBehavior.nestedScrollConnection
            ),
        bottomBar = {
            if (bottomSearchBar) {
                BottomAppBar(
                    modifier = Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        bottomAppBarScrollBehavior.state.heightOffsetLimit =
                            -placeable.height.toFloat()
                        val height =
                            placeable.height + bottomAppBarScrollBehavior.state.heightOffset
                        layout(placeable.width, height.roundToInt().coerceAtLeast(0)) {
                            placeable.place(0, 0)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrollBehavior = bottomAppBarScrollBehavior
                ) {
                    searchBar()
                }
            }
        },
        topBar = {
            if (!bottomSearchBar) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    scrollBehavior = topAppBarScrollBehavior,
                    title = {
                        Row {
                            searchBar()
                        }
                    }
                )
            }
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                top = if (bottomSearchBar) 0.dp else contentPadding.calculateTopPadding(),
                bottom = if (bottomSearchBar)
                    maxOf(
                    WindowInsets.mandatorySystemGestures
                        .asPaddingValues()
                        .calculateBottomPadding(),
                    contentPadding.calculateBottomPadding()
                ) else
                    48.dp
            ),
            verticalArrangement = if (bottomSearchBar) Arrangement.Bottom else Arrangement.Top,
        ) {
            items(items = filters, key = { it.key() }) {
                when (it) {
                    is DrawerItem.Filter -> FilterItem(
//                        modifier = Modifier.animateItemPlacement(),
                        item = it,
                        onClick = { onClick(it) }
                    )

                    is DrawerItem.Header -> HeaderItem(
//                        modifier = Modifier.animateItemPlacement(),
                        item = it,
                        canAdd = it.canAdd,
                        toggleCollapsed = { onClick(it) },
                        onAddClick = { onAddClick(it) },
                        onErrorClick = onErrorClick,
                    )
                }
            }
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
                    MaterialTheme.colorScheme.onSurface.copy(alpha = .1f)
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
            modifier = Modifier.weight(1f),
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
            modifier = Modifier.width(48.dp),
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
    Column(
        modifier = modifier,
    ) {
        Divider(modifier = Modifier.fillMaxWidth())
        MenuRow(
            padding = PaddingValues(start = 16.dp),
            onClick = toggleCollapsed,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = item.title,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(onClick = toggleCollapsed) {
                Chevron(item.collapsed)
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

@Composable
private fun MenuRow(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = 16.dp),
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .height(48.dp)
            .padding(padding)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun RowScope.MenuSearchBar(
    begForMoney: Boolean,
    onDrawerAction: (DrawerAction) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
) {
    var hasFocus by remember { mutableStateOf(false) }
    SearchBar(
        modifier = Modifier
            .onFocusChanged { hasFocus = it.hasFocus }
            .padding(
                start = 8.dp,
                end = if (hasFocus) 8.dp else 0.dp,
                bottom = 4.dp
            )
            .weight(1f)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
        text = query,
        onTextChange = { onQueryChange(it) },
        placeHolder = stringResource(Res.string.search),
        onCloseClicked = { onQueryChange("") },
        onSearchClicked = {
            // TODO: close keyboard
        },
    )
    if (!hasFocus) {
        if (begForMoney) {
            IconButton(onClick = { onDrawerAction(DrawerAction.PURCHASE) }) {
                Icon(
                    imageVector = Icons.Outlined.AttachMoney,
                    contentDescription = stringResource(Res.string.subscribe),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        IconButton(onClick = { onDrawerAction(DrawerAction.HELP_AND_FEEDBACK) }) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                contentDescription = stringResource(Res.string.help_and_feedback),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = { onDrawerAction(DrawerAction.SETTINGS) }) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(Res.string.settings),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
