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
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import org.jetbrains.compose.resources.stringResource
import org.tasks.R
import org.tasks.TasksUrls
import org.tasks.compose.settings.TodoistAccountScreen
import org.tasks.compose.settings.todoistAccountActions
import org.tasks.data.entity.CaldavAccount
import org.tasks.extensions.Context.openUri
import org.tasks.preferences.BasePreferences
import org.tasks.sync.todoist.TodoistAccountSettingsHiltViewModel
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.todoist
import javax.inject.Inject

@AndroidEntryPoint
class TodoistAccountFragment : Fragment() {

    @Inject lateinit var theme: Theme

    private val viewModel: TodoistAccountSettingsHiltViewModel by viewModels()

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
            val finished by viewModel.finished.collectAsStateWithLifecycle()
            var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
            val navigateBack = { parentFragmentManager.popBackStack(); Unit }
            val fallback = stringResource(Res.string.todoist)
            val accountName = state.account?.name?.takeIf { it.isNotBlank() } ?: fallback

            LaunchedEffect(finished) {
                if (finished) {
                    navigateBack()
                }
            }

            TodoistAccountScreen(
                state = state,
                isNewAccount = false,
                accountName = accountName,
                showDiscardDialog = showDiscardDialog,
                actions = todoistAccountActions(
                    viewModel = viewModel,
                    onNavigateBack = navigateBack,
                    onDiscardDialogChange = { showDiscardDialog = it },
                    onOpenTokenHelp = { requireContext().openUri(TasksUrls.TODOIST_API_TOKEN) },
                    onDelete = { viewModel.delete(navigateBack) },
                ),
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

        fun newTodoistAccountFragment(account: CaldavAccount): Fragment {
            val fragment = TodoistAccountFragment()
            fragment.arguments = Bundle().apply {
                putParcelable(EXTRA_ACCOUNT, account)
            }
            return fragment
        }
    }
}
