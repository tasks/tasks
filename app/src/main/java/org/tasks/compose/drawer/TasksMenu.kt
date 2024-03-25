package org.tasks.compose.drawer

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.todoroo.astrid.api.Filter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.Tasks
import org.tasks.activities.NavigationDrawerCustomization
import org.tasks.billing.PurchaseActivity
import org.tasks.extensions.Context.findActivity
import org.tasks.extensions.Context.openUri
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.preferences.HelpAndFeedback
import org.tasks.preferences.MainPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksMenu(
    bottomPadding: Dp = 0.dp,
    items: ImmutableList<DrawerItem>,
    isTopAppBar: Boolean,
    begForMoney: Boolean,
    setFilter: (Filter) -> Unit,
    toggleCollapsed: (NavigationDrawerSubheader) -> Unit,
    addFilter: (NavigationDrawerSubheader) -> Unit,
    dismiss: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val skipPartiallyExpanded = remember(expanded) {
        expanded || isTopAppBar
    }
    val density = LocalDensity.current
    val sheetState = rememberSaveable(
        skipPartiallyExpanded,
        saver = SheetState.Saver(
            skipPartiallyExpanded = skipPartiallyExpanded,
            confirmValueChange = { true },
            density = density,
        )
    ) {
        SheetState(
            skipPartiallyExpanded = skipPartiallyExpanded,
            initialValue = if (skipPartiallyExpanded) SheetValue.Expanded else SheetValue.PartiallyExpanded,
            confirmValueChange = { true },
            skipHiddenState = false,
            density = density,
        )
    }
    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == SheetValue.Expanded) {
            expanded = true
        }
    }
    val context = LocalContext.current
    val settingsRequest = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        context.findActivity()?.recreate()
    }
    ModalBottomSheet(
        sheetState = sheetState,
        containerColor = MaterialTheme.colors.surface,
        onDismissRequest = { dismiss() }
    ) {
        val scope = rememberCoroutineScope()
        TaskListDrawer(
            bottomPadding = bottomPadding,
            begForMoney = begForMoney,
            filters = items,
            onClick = {
                when (it) {
                    is DrawerItem.Filter -> {
                        setFilter(it.type())
                        scope.launch(Dispatchers.Default) {
                            sheetState.hide()
                            dismiss()
                        }
                    }
                    is DrawerItem.Header -> {
                        toggleCollapsed(it.type())
                    }
                }
            },
            onAddClick = {
                scope.launch(Dispatchers.Default) {
                    sheetState.hide()
                    dismiss()
                    addFilter(it.type())
                }
            },
            onDrawerAction = {
                dismiss()
                when (it) {
                    DrawerAction.PURCHASE ->
                        if (Tasks.IS_GENERIC)
                            context.openUri(R.string.url_donate)
                        else
                            context.startActivity(Intent(context, PurchaseActivity::class.java))

                    DrawerAction.CUSTOMIZE_DRAWER ->
                        context.startActivity(
                            Intent(context, NavigationDrawerCustomization::class.java)
                        )

                    DrawerAction.SETTINGS ->
                        settingsRequest.launch(Intent(context, MainPreferences::class.java))

                    DrawerAction.HELP_AND_FEEDBACK ->
                        context.startActivity(Intent(context, HelpAndFeedback::class.java))
                }
            },
            onErrorClick = {
                context.startActivity(Intent(context, MainPreferences::class.java))
            },
        )
    }
}