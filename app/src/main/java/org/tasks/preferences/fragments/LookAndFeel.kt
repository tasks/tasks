package org.tasks.preferences.fragments

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.lifecycleScope
import com.todoroo.andlib.utility.AndroidUtilities.atLeastTiramisu
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.billing.PurchaseActivity
import org.tasks.billing.PurchaseActivityViewModel
import org.tasks.compose.FilterSelectionActivity.Companion.launch
import org.tasks.compose.FilterSelectionActivity.Companion.registerForFilterPickerResult
import org.tasks.compose.settings.LookAndFeelScreen
import org.tasks.dialogs.ColorPalettePicker
import org.tasks.dialogs.ColorPalettePicker.Companion.newColorPalette
import org.tasks.dialogs.ColorPickerAdapter
import org.tasks.dialogs.ColorWheelPicker
import org.tasks.dialogs.ThemePickerDialog
import org.tasks.dialogs.ThemePickerDialog.Companion.newThemePickerDialog
import org.tasks.extensions.Context.openUri
import org.tasks.locale.LocalePickerDialog
import org.tasks.preferences.BasePreferences
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import org.tasks.themes.ThemeBase
import org.tasks.themes.ThemeBase.EXTRA_THEME_OVERRIDE
import javax.inject.Inject

@AndroidEntryPoint
class LookAndFeel : Fragment() {

    @Inject lateinit var theme: Theme

    private val viewModel: LookAndFeelViewModel by viewModels()

    private val listPickerLauncher = registerForFilterPickerResult {
        viewModel.setDefaultFilter(it)
    }

    private val purchaseLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        val index = viewModel.handlePurchaseResult(result.data)
        applyBaseTheme(index)
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        parentFragmentManager.setFragmentResultListener(
            ThemePickerDialog.REQUEST_KEY, this
        ) { _, bundle ->
            val selectedIndex = bundle.getInt(
                ThemePickerDialog.EXTRA_SELECTED,
                ThemeBase.DEFAULT_BASE_THEME
            )
            when (val result = viewModel.handleThemePickerResult(selectedIndex)) {
                is LookAndFeelViewModel.ThemePickerResult.ApplyTheme -> {
                    applyBaseTheme(result.index)
                }
                is LookAndFeelViewModel.ThemePickerResult.PurchaseRequired -> {
                    purchaseLauncher.launch(
                        Intent(context, PurchaseActivity::class.java)
                            .putExtra(PurchaseActivityViewModel.EXTRA_SOURCE, "themes"),
                    )
                }
            }
        }
        parentFragmentManager.setFragmentResultListener(
            REQUEST_KEY_COLOR, this
        ) { _, bundle ->
            val color = bundle.getInt(
                ColorWheelPicker.EXTRA_SELECTED,
                theme.themeColor.primaryColor
            )
            if (viewModel.handleColorPickerResult(color)) {
                requireActivity().recreate()
            }
        }
        parentFragmentManager.setFragmentResultListener(
            REQUEST_KEY_LAUNCHER, this
        ) { _, bundle ->
            val index = bundle.getInt(ColorPalettePicker.EXTRA_SELECTED, 0)
            viewModel.handleLauncherPickerResult(requireContext(), index)
        }
        parentFragmentManager.setFragmentResultListener(
            LocalePickerDialog.REQUEST_KEY, this
        ) { _, bundle ->
            val languageTag = bundle.getString(LocalePickerDialog.EXTRA_LOCALE)
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(languageTag)
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ) = content {
        TasksSettingsTheme(
            theme = theme.themeBase.index,
            primary = theme.themeColor.primaryColor,
        ) {
            LookAndFeelScreen(
                themeName = viewModel.themeName,
                dynamicColorAvailable = viewModel.dynamicColorAvailable,
                dynamicColorEnabled = viewModel.dynamicColorEnabled,
                dynamicColorProOnly = viewModel.dynamicColorProOnly,
                themeColor = viewModel.currentThemeColor,
                launcherColor = viewModel.currentLauncherColor,
                markdownEnabled = viewModel.markdownEnabled,
                openLastViewedList = viewModel.openLastViewedList,
                defaultFilterName = viewModel.defaultFilterName,
                localeName = viewModel.localeName,
                onTheme = {
                    newThemePickerDialog(theme.themeBase.index)
                        .show(parentFragmentManager, FRAG_TAG_THEME_PICKER)
                },
                onDynamicColor = { enabled ->
                    viewModel.updateDynamicColor(enabled)
                },
                onColor = {
                    newColorPalette(
                        REQUEST_KEY_COLOR,
                        theme.themeColor.pickerColor,
                        ColorPickerAdapter.Palette.COLORS,
                    ).show(parentFragmentManager, FRAG_TAG_COLOR_PICKER)
                },
                onLauncher = {
                    newColorPalette(
                        REQUEST_KEY_LAUNCHER,
                        viewModel.currentLauncherColor,
                        ColorPickerAdapter.Palette.LAUNCHERS,
                    ).show(parentFragmentManager, FRAG_TAG_COLOR_PICKER)
                },
                onMarkdown = { enabled ->
                    viewModel.updateMarkdown(enabled)
                },
                onOpenLastViewedList = { enabled ->
                    viewModel.updateOpenLastViewedList(enabled)
                },
                onDefaultFilter = {
                    lifecycleScope.launch {
                        listPickerLauncher.launch(
                            context = requireContext(),
                            selectedFilter = viewModel.getDefaultOpenFilter(),
                        )
                    }
                },
                onLanguage = {
                    if (atLeastTiramisu()) {
                        startActivity(
                            Intent(Settings.ACTION_APP_LOCALE_SETTINGS)
                                .setData(
                                    Uri.fromParts(
                                        "package",
                                        requireContext().packageName,
                                        null,
                                    )
                                )
                        )
                    } else {
                        LocalePickerDialog.newLocalePickerDialog()
                            .show(parentFragmentManager, FRAG_TAG_LOCALE_PICKER)
                    }
                },
                onTranslations = {
                    context?.openUri(R.string.url_translations)
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshState(
            themeBaseIndex = theme.themeBase.index,
            themeColorPickerColor = theme.themeColor.pickerColor,
        )
        val surfaceColor = theme.themeBase.getSettingsSurfaceColor(requireActivity())
        (activity as? BasePreferences)?.toolbar?.let { toolbar ->
            toolbar.setBackgroundColor(surfaceColor)
            (toolbar.parent as? View)?.setBackgroundColor(surfaceColor)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val defaultColor = androidx.core.content.ContextCompat.getColor(
            requireContext(),
            R.color.content_background,
        )
        (activity as? BasePreferences)?.toolbar?.let { toolbar ->
            toolbar.setBackgroundColor(defaultColor)
            (toolbar.parent as? View)?.setBackgroundColor(defaultColor)
        }
    }

    private fun applyBaseTheme(index: Int) {
        activity?.intent?.removeExtra(EXTRA_THEME_OVERRIDE)
        val needsRecreate = viewModel.setBaseTheme(index)
        if (needsRecreate) {
            Handler().post {
                ThemeBase(index).setDefaultNightMode()
                requireActivity().recreate()
            }
        }
    }

    companion object {
        private const val REQUEST_KEY_COLOR = "color_picker_result"
        private const val REQUEST_KEY_LAUNCHER = "launcher_picker_result"
        private const val FRAG_TAG_LOCALE_PICKER = "frag_tag_locale_picker"
        private const val FRAG_TAG_THEME_PICKER = "frag_tag_theme_picker"
        private const val FRAG_TAG_COLOR_PICKER = "frag_tag_color_picker"
    }
}
