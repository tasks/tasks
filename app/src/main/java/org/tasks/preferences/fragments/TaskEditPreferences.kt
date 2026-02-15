package org.tasks.preferences.fragments

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import com.todoroo.andlib.utility.AndroidUtilities.atLeastOreoMR1
import com.todoroo.astrid.activity.BeastModePreferences
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.compose.settings.TaskEditScreen
import org.tasks.preferences.BasePreferences
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class TaskEditPreferences : Fragment() {

    @Inject lateinit var theme: Theme

    private val viewModel: TaskEditPreferencesViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ) = content {
        TasksSettingsTheme(
            theme = theme.themeBase.index,
            primary = theme.themeColor.primaryColor,
        ) {
            TaskEditScreen(
                showWithoutUnlockVisible = atLeastOreoMR1(),
                showLinks = viewModel.showLinks,
                backButtonSaves = viewModel.backButtonSaves,
                multilineTitle = viewModel.multilineTitle,
                showComments = viewModel.showComments,
                showWithoutUnlock = viewModel.showWithoutUnlock,
                onCustomizeEditScreen = {
                    startActivity(
                        Intent(requireContext(), BeastModePreferences::class.java)
                    )
                },
                onShowLinks = { viewModel.updateShowLinks(it) },
                onBackButtonSaves = { viewModel.updateBackButtonSaves(it) },
                onMultilineTitle = { viewModel.updateMultilineTitle(it) },
                onShowComments = { viewModel.updateShowComments(it) },
                onShowWithoutUnlock = { viewModel.updateShowWithoutUnlock(it) },
            )
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
