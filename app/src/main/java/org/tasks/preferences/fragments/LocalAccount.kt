package org.tasks.preferences.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import org.jetbrains.compose.resources.stringResource
import org.tasks.R
import org.tasks.compose.settings.LocalAccountScreen
import org.tasks.data.entity.CaldavAccount
import org.tasks.preferences.BasePreferences
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.local_lists
import javax.inject.Inject

@AndroidEntryPoint
class LocalAccount : Fragment() {

    @Inject lateinit var theme: Theme

    private val viewModel: LocalAccountViewModel by viewModels()

    private val initialAccount: CaldavAccount
        get() = requireArguments().getParcelable(EXTRA_ACCOUNT)!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        LaunchedEffect(Unit) {
            viewModel.setAccount(initialAccount)
        }
        TasksSettingsTheme(
            theme = theme.themeBase.index,
            primary = theme.themeColor.primaryColor,
        ) {
            val displayName = viewModel.displayName.collectAsStateWithLifecycle().value
            val nameError = viewModel.nameError.collectAsStateWithLifecycle().value
            val taskCount = viewModel.taskCount.collectAsStateWithLifecycle().value
            val account = viewModel.account.collectAsStateWithLifecycle().value
            val hasChanges = viewModel.hasChanges.collectAsStateWithLifecycle().value
            var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
            val navigateBack = { parentFragmentManager.popBackStack(); Unit }

            LocalAccountScreen(
                displayName = displayName,
                nameError = nameError,
                taskCount = taskCount,
                accountName = account?.name?.takeIf { it.isNotBlank() }
                    ?: stringResource(Res.string.local_lists),
                hasChanges = hasChanges,
                showDiscardDialog = showDiscardDialog,
                onNameChange = viewModel::setDisplayName,
                onSave = { viewModel.save(navigateBack) },
                onDelete = { viewModel.delete(navigateBack) },
                onNavigateBack = navigateBack,
                onDiscardDialogChange = { showDiscardDialog = it },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        val surfaceColor = theme.themeBase.getSettingsSurfaceColor(requireActivity())
        (activity as? BasePreferences)?.toolbar?.let { toolbar ->
            toolbar.setBackgroundColor(surfaceColor)
            (toolbar.parent as? android.view.View)?.setBackgroundColor(surfaceColor)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val defaultColor = ContextCompat.getColor(requireContext(), R.color.content_background)
        (activity as? BasePreferences)?.toolbar?.let { toolbar ->
            toolbar.setBackgroundColor(defaultColor)
            (toolbar.parent as? android.view.View)?.setBackgroundColor(defaultColor)
        }
    }

    companion object {
        private const val EXTRA_ACCOUNT = "extra_account"

        fun newLocalAccountPreference(account: CaldavAccount): Fragment {
            val fragment = LocalAccount()
            fragment.arguments = Bundle().apply {
                putParcelable(EXTRA_ACCOUNT, account)
            }
            return fragment
        }
    }
}
