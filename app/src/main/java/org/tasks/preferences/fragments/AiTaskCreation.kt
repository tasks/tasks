package org.tasks.preferences.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import dagger.hilt.android.AndroidEntryPoint
import org.jetbrains.compose.resources.getString
import org.tasks.R
import org.tasks.TasksUrls
import org.tasks.compose.AiDisclosureDialog
import org.tasks.compose.settings.AiTaskCreationScreen
import org.tasks.extensions.Context.openUri
import org.tasks.extensions.Context.toast
import org.tasks.preferences.BasePreferences
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class AiTaskCreation : Fragment() {

    @Inject lateinit var theme: Theme

    private val viewModel: AiTaskCreationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = content {
        TasksSettingsTheme(
            theme = theme.themeBase.index,
            primary = theme.themeColor.primaryColor,
        ) {
            val state by viewModel.state.collectAsState()
            val showDisclosure by viewModel.showDisclosure.collectAsState()
            val message by viewModel.message.collectAsState()

            AiTaskCreationScreen(
                state = state,
                onEnabledChange = { viewModel.onEnabledChange(it) },
                onKeyChange = { viewModel.onKeyChange(it) },
                onKeySubmit = { viewModel.onKeySubmit() },
                onModelSelected = { viewModel.onModelSelected(it) },
                onPrivacyPolicy = { context?.openUri(TasksUrls.OPENROUTER_PRIVACY) },
            )

            if (showDisclosure) {
                AiDisclosureDialog(
                    onAccept = { viewModel.onDisclosureAccepted() },
                    onDismiss = { viewModel.onDisclosureDismissed() },
                    openUrl = { context?.openUri(it) },
                )
            }

            message?.let { resource ->
                androidx.compose.runtime.LaunchedEffect(resource) {
                    context?.toast(getString(resource))
                    viewModel.messageShown()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
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
