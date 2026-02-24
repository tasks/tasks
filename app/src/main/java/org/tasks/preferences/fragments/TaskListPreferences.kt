package org.tasks.preferences.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.compose.settings.TaskListScreen
import org.tasks.preferences.BasePreferences
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class TaskListPreferences : Fragment() {

    @Inject lateinit var theme: Theme

    private val viewModel: TaskListPreferencesViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ) = content {
        TasksSettingsTheme(
            theme = theme.themeBase.index,
            primary = theme.themeColor.primaryColor,
        ) {
            TaskListScreen(
                fontSize = viewModel.fontSize,
                rowSpacing = viewModel.rowSpacing,
                showFullTitle = viewModel.showFullTitle,
                showDescription = viewModel.showDescription,
                showFullDescription = viewModel.showFullDescription,
                showLinks = viewModel.showLinks,
                chipAppearanceSummary = viewModel.chipAppearanceSummary,
                subtaskChips = viewModel.subtaskChips,
                startDateChips = viewModel.startDateChips,
                placeChips = viewModel.placeChips,
                listChips = viewModel.listChips,
                tagChips = viewModel.tagChips,
                onFontSize = { viewModel.updateFontSize(it) },
                onRowSpacing = { viewModel.updateRowSpacing(it) },
                onShowFullTitle = { viewModel.updateShowFullTitle(it) },
                onShowDescription = { viewModel.updateShowDescription(it) },
                onShowFullDescription = { viewModel.updateShowFullDescription(it) },
                onShowLinks = { viewModel.updateShowLinks(it) },
                onChipAppearance = { viewModel.openChipAppearanceDialog() },
                onSubtaskChips = { viewModel.updateSubtaskChips(it) },
                onStartDateChips = { viewModel.updateStartDateChips(it) },
                onPlaceChips = { viewModel.updatePlaceChips(it) },
                onListChips = { viewModel.updateListChips(it) },
                onTagChips = { viewModel.updateTagChips(it) },
            )

            if (viewModel.showChipAppearanceDialog) {
                val currentValue = viewModel.getChipAppearanceCurrentValue()
                AlertDialog(
                    onDismissRequest = { viewModel.dismissChipAppearanceDialog() },
                    title = { Text(stringResource(R.string.chip_appearance)) },
                    text = {
                        Column {
                            viewModel.chipEntries.forEachIndexed { index, entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setChipAppearance(
                                                viewModel.chipValues[index]
                                            )
                                            viewModel.dismissChipAppearanceDialog()
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = viewModel.chipValues[index] == currentValue,
                                        onClick = null,
                                    )
                                    Text(
                                        text = entry,
                                        modifier = Modifier.padding(start = 8.dp),
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {},
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshState()
        val surfaceColor = theme.themeBase.getSettingsSurfaceColor(requireActivity())
        (activity as? BasePreferences)?.toolbar?.let { toolbar ->
            toolbar.setBackgroundColor(surfaceColor)
            (toolbar.parent as? View)?.setBackgroundColor(surfaceColor)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val defaultColor = ContextCompat.getColor(requireContext(), R.color.content_background)
        (activity as? BasePreferences)?.toolbar?.let { toolbar ->
            toolbar.setBackgroundColor(defaultColor)
            (toolbar.parent as? View)?.setBackgroundColor(defaultColor)
        }
    }
}
