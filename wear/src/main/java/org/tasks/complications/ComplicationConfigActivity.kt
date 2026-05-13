package org.tasks.complications

import android.app.Activity
import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import org.tasks.presentation.screens.MenuScreen
import org.tasks.presentation.screens.MenuViewModel
import org.tasks.presentation.theme.TasksTheme

class ComplicationConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val complicationId = intent.getIntExtra(
            "android.support.wearable.complications.EXTRA_CONFIG_COMPLICATION_ID",
            -1
        )
        val componentName = intent.getStringExtra(
            "android.support.wearable.complications.EXTRA_CONFIG_DATA_SOURCE_COMPONENT"
        )?.let { ComponentName.unflattenFromString(it) }

        if (complicationId == -1) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        setResult(Activity.RESULT_CANCELED)

        setContent {
            TasksTheme {
                val menuViewModel: MenuViewModel = viewModel()
                val menuItems = menuViewModel.uiItems.collectAsLazyPagingItems()
                MenuScreen(
                    items = menuItems,
                    selectFilter = { item ->
                        setComplicationFilter(complicationId, item.id, item.title)
                        if (componentName != null) {
                            ComplicationDataSourceUpdateRequester.create(this, componentName)
                                .requestUpdateAll()
                        }
                        setResult(Activity.RESULT_OK)
                        finish()
                    },
                )
            }
        }
    }
}
