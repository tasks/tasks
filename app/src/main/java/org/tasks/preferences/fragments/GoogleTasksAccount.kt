package org.tasks.preferences.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.compose.settings.GoogleTasksAccountScreen
import org.tasks.data.entity.CaldavAccount
import org.tasks.preferences.BasePreferences
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class GoogleTasksAccount : Fragment() {

    @Inject lateinit var theme: Theme

    private val viewModel: GoogleTasksAccountHiltViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        TasksSettingsTheme(
            theme = theme.themeBase.index,
            primary = theme.themeColor.primaryColor,
        ) {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val accountName = state.account?.name ?: ""

            GoogleTasksAccountScreen(
                error = state.error,
                isUnauthorized = state.isUnauthorized,
                accountName = accountName,
                onSignIn = { requestLogin() },
                onDelete = {
                    viewModel.delete { parentFragmentManager.popBackStack() }
                },
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

    private fun requestLogin() {
        activity?.startActivityForResult(
            Intent(activity, GtasksLoginActivity::class.java),
            MainSettingsComposeFragment.REQUEST_GOOGLE_TASKS
        )
    }

    companion object {
        const val EXTRA_ACCOUNT = "extra_account"

        fun newGoogleTasksAccountPreference(account: CaldavAccount): Fragment =
            GoogleTasksAccount().apply {
                arguments = Bundle().apply {
                    putParcelable(EXTRA_ACCOUNT, account)
                }
            }
    }
}
