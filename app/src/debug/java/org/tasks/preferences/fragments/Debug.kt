package org.tasks.preferences.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import at.bitfire.cert4android.CustomCertManager.Companion.resetCertificates
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.compose.settings.DebugScreen
import org.tasks.extensions.Context.toast
import org.tasks.preferences.BasePreferences
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class Debug : Fragment() {

    @Inject lateinit var theme: Theme

    private val viewModel: DebugViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ) = content {
        TasksSettingsTheme(
            theme = theme.themeBase.index,
            primary = theme.themeColor.primaryColor,
        ) {
            DebugScreen(
                leakCanaryEnabled = viewModel.leakCanaryEnabled,
                strictModeVmEnabled = viewModel.strictModeVmEnabled,
                strictModeThreadEnabled = viewModel.strictModeThreadEnabled,
                crashOnViolationEnabled = viewModel.crashOnViolationEnabled,
                unlockProEnabled = viewModel.unlockProEnabled,
                showDebugFilters = viewModel.showDebugFilters,
                iapTitle = viewModel.iapTitle,
                onLeakCanary = { viewModel.updateLeakCanary(it) },
                onStrictModeVm = { viewModel.updateStrictModeVm(it) },
                onStrictModeThread = { viewModel.updateStrictModeThread(it) },
                onCrashOnViolation = { viewModel.updateCrashOnViolation(it) },
                onUnlockPro = { viewModel.updateUnlockPro(it) },
                onShowDebugFilters = { viewModel.updateShowDebugFilters(it) },
                onResetSsl = {
                    resetCertificates(requireContext())
                    context?.toast("SSL certificates reset")
                },
                onCrashApp = {
                    throw RuntimeException("Crashed app from debug preferences")
                },
                onRestartApp = {
                    kotlin.system.exitProcess(0)
                },
                onIap = {
                    viewModel.toggleIap(requireActivity()) {}
                },
                onClearHints = {
                    viewModel.clearHints()
                },
                onCreateTasks = {
                    viewModel.createTasks { count ->
                        Toast.makeText(context, "Created $count tasks", Toast.LENGTH_SHORT).show()
                    }
                },
            )

            if (viewModel.showRestartDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissRestartDialog() },
                    text = {
                        Text(stringResource(R.string.restart_required))
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.dismissRestartDialog()
                            kotlin.system.exitProcess(0)
                        }) {
                            Text(stringResource(R.string.restart_now))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissRestartDialog() }) {
                            Text(stringResource(R.string.restart_later))
                        }
                    },
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
