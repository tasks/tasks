package org.tasks.preferences.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.PlatformConfiguration
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseActivity
import org.tasks.billing.PurchaseActivityViewModel.Companion.EXTRA_NAME_YOUR_PRICE
import org.tasks.billing.PurchaseActivityViewModel.Companion.EXTRA_SHOW_MORE_OPTIONS
import org.tasks.billing.PurchaseActivityViewModel.Companion.EXTRA_SOURCE
import org.tasks.compose.settings.WorksWithScreen
import org.tasks.extensions.Context.openUri
import org.tasks.preferences.BasePreferences
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class WorksWith : Fragment() {

    @Inject lateinit var theme: Theme
    @Inject lateinit var configuration: PlatformConfiguration
    @Inject lateinit var inventory: Inventory

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = content {
        TasksSettingsTheme(
            theme = theme.themeBase.index,
            primary = theme.themeColor.primaryColor,
        ) {
            WorksWithScreen(
                configuration = configuration,
                hasSubscription = inventory.hasPro,
                onLinkClick = { context?.openUri(it) },
                onMcpSettingsClick = {},
                onPricingClick = {
                    startActivity(
                        Intent(context, PurchaseActivity::class.java)
                            .putExtra(EXTRA_NAME_YOUR_PRICE, false)
                            .putExtra(EXTRA_SHOW_MORE_OPTIONS, true)
                            .putExtra(EXTRA_SOURCE, "works_with")
                    )
                },
                bottomInsets = {
                    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
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
