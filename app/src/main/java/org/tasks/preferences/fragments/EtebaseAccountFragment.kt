package org.tasks.preferences.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.BundleCompat
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
import org.tasks.compose.settings.EtebaseAccountScreen
import org.tasks.data.entity.CaldavAccount
import org.tasks.preferences.BasePreferences
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.etesync
import javax.inject.Inject

@AndroidEntryPoint
class EtebaseAccountFragment : Fragment() {

    @Inject lateinit var theme: Theme

    private val viewModel: EtebaseAccountSettingsHiltViewModel by viewModels()

    private val initialAccount: CaldavAccount
        get() = BundleCompat.getParcelable(requireArguments(), EXTRA_ACCOUNT, CaldavAccount::class.java)!!

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
            val state by viewModel.state.collectAsStateWithLifecycle()
            var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
            val navigateBack = { parentFragmentManager.popBackStack(); Unit }
            val etesyncFallback = stringResource(Res.string.etesync)
            val accountName = state.account?.name?.takeIf { it.isNotBlank() } ?: etesyncFallback

            EtebaseAccountScreen(
                state = state,
                isNewAccount = false,
                accountName = accountName,
                showDiscardDialog = showDiscardDialog,
                onUrlChange = viewModel::setUrl,
                onUsernameChange = viewModel::setUsername,
                onPasswordChange = viewModel::setPassword,
                onNameChange = viewModel::setDisplayName,
                onShowUrlChange = viewModel::setShowUrl,
                onSave = { viewModel.save(navigateBack) },
                onDelete = { viewModel.delete(navigateBack) },
                onNavigateBack = navigateBack,
                onDiscardDialogChange = { showDiscardDialog = it },
                onDismissSnackbar = viewModel::dismissSnackbar,
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

        fun newEtebaseAccountFragment(account: CaldavAccount): Fragment {
            val fragment = EtebaseAccountFragment()
            fragment.arguments = Bundle().apply {
                putParcelable(EXTRA_ACCOUNT, account)
            }
            return fragment
        }
    }
}
