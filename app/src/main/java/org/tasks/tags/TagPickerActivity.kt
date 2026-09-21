package org.tasks.tags

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.Strings
import org.tasks.compose.pickers.TagPicker
import org.tasks.data.entity.TagData
import org.tasks.extensions.addBackPressedCallback
import org.tasks.injection.ThemedInjectingAppCompatActivity
import org.tasks.themes.TasksTheme
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class TagPickerActivity : ThemedInjectingAppCompatActivity() {
    @Inject lateinit var theme: Theme

    private val viewModel: TagPickerHiltViewModel by viewModels()
    private var taskIds: ArrayList<Long>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        taskIds = intent.getSerializableExtra(EXTRA_TASKS) as ArrayList<Long>?
        if (savedInstanceState == null) {
            intent.getParcelableArrayListExtra<TagData>(EXTRA_SELECTED)?.let { selected ->
                viewModel.setSelected(
                    selected, intent.getParcelableArrayListExtra<TagData>(EXTRA_PARTIALLY_SELECTED)
                )
            }
        }

        addBackPressedCallback { handleBackPressed() }

        viewModel.search("")

        setContent {
            TasksTheme(
                theme = theme.themeBase.index,
                primary = theme.themeColor.primaryColor,
            ) {
                TagPicker(
                    viewModel = viewModel,
                    onBackClicked = { handleBackPressed() },
                    getTagIcon = viewModel::getIcon,
                    getTagColor = viewModel::getColor,
                )
            }
        }
    }

    private fun handleBackPressed() {
        if (Strings.isNullOrEmpty(viewModel.searchText.value)) {
            setResult(
                Activity.RESULT_OK,
                Intent()
                    .putExtra(EXTRA_TASKS, taskIds)
                    .putParcelableArrayListExtra(EXTRA_PARTIALLY_SELECTED, viewModel.getPartiallySelected())
                    .putParcelableArrayListExtra(EXTRA_SELECTED, viewModel.getSelected())
            )
            finish()
        } else {
            viewModel.search("")
        }
    }

    companion object {
        const val EXTRA_SELECTED = "extra_tags"
        const val EXTRA_PARTIALLY_SELECTED = "extra_partial"
        const val EXTRA_TASKS = "extra_tasks"
    }
}
