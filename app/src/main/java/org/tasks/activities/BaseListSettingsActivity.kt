package org.tasks.activities

import android.os.Bundle
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.tasks.compose.DeleteButton
import org.tasks.compose.IconPickerActivity.Companion.launchIconPicker
import org.tasks.compose.IconPickerActivity.Companion.registerForIconPickerResult
import org.tasks.compose.settings.BaseSettingsContent
import org.tasks.dialogs.ColorPalettePicker
import org.tasks.dialogs.ColorPalettePicker.Companion.newColorPalette
import org.tasks.dialogs.ColorPickerAdapter.Palette
import org.tasks.dialogs.ColorWheelPicker
import org.tasks.extensions.addBackPressedCallback
import org.tasks.injection.ThemedInjectingAppCompatActivity
import org.tasks.themes.ColorProvider
import org.tasks.themes.ThemeColor
import javax.inject.Inject


abstract class BaseListSettingsActivity : ThemedInjectingAppCompatActivity(), ColorPalettePicker.ColorPickedCallback, ColorWheelPicker.ColorPickedCallback {
    @Inject lateinit var colorProvider: ColorProvider
    protected abstract val defaultIcon: String
    protected var selectedColor = 0
    protected var selectedIcon = mutableStateOf<String?>(null)

    protected val textState = mutableStateOf("")
    protected val errorState = mutableStateOf("")
    protected val colorState = mutableStateOf(Color.Unspecified)
    protected val showProgress = mutableStateOf(false)
    protected val promptDelete = mutableStateOf(false)
    protected val promptDiscard = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* defaultIcon is initialized in the descendant's constructor so it can not be used
           in constructor of the base class. So valid initial value for iconState is set here  */
        selectedIcon.value = defaultIcon

        if (savedInstanceState != null) {
            selectedColor = savedInstanceState.getInt(EXTRA_SELECTED_THEME)
            selectedIcon.value = savedInstanceState.getString(EXTRA_SELECTED_ICON) ?: defaultIcon
        }
        addBackPressedCallback {
            discard()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_SELECTED_THEME, selectedColor)
        outState.putString(EXTRA_SELECTED_ICON, selectedIcon.value)
    }

    protected abstract fun hasChanges(): Boolean
    protected abstract suspend fun save()
    protected abstract val isNew: Boolean
    protected abstract val toolbarTitle: String?
    protected abstract suspend fun delete()
    protected open fun discard() {
        if (hasChanges())  promptDiscard.value = true
        else finish()
    }

    protected fun clearColor() {
        onColorPicked(0)
    }

    protected fun showThemePicker() {
        newColorPalette(null, 0, selectedColor, Palette.COLORS)
                .show(supportFragmentManager, FRAG_TAG_COLOR_PICKER)
    }

    val launcher = registerForIconPickerResult { selected ->
        selectedIcon.value = selected
    }

    fun showIconPicker() {
        launcher.launchIconPicker(this, selectedIcon.value)
    }

    override fun onColorPicked(color: Int) {
        selectedColor = color
        updateTheme()
    }

    protected open fun promptDelete() { promptDelete.value = true }

    protected fun updateTheme() {

        val themeColor: ThemeColor =
            if (selectedColor == 0) this.themeColor
            else colorProvider.getThemeColor(selectedColor, true)

        colorState.value =
            if (selectedColor == 0) Color.Unspecified
            else Color((colorProvider.getThemeColor(selectedColor, true)).primaryColor)

        //iconState.intValue = (getIconResId(selectedIcon) ?: getIconResId(defaultIcon))!!

        themeColor.applyToNavigationBar(this)
    }

    /** Standard @Compose view content for descendants. Caller must wrap it to TasksTheme{} */
    @Composable
    protected fun BaseSettingsContent(
        title: String = toolbarTitle ?: "",
        requestKeyboard: Boolean = isNew,
        optionButton: @Composable () -> Unit = {
            if (!isNew) DeleteButton(toolbarTitle ?: "") { promptDelete() }
        },
        extensionContent: @Composable ColumnScope.() -> Unit = {}
    ) {
        BaseSettingsContent(
            title = title,
            color = colorState.value,
            icon = selectedIcon.value ?: defaultIcon,
            text = textState.value,
            error = errorState.value,
            requestKeyboard = requestKeyboard,
            promptDiscard = promptDiscard.value,
            showProgress = showProgress.value,
            dismissDiscardPrompt = { promptDiscard.value = false },
            setText = {
                textState.value = it
                errorState.value = ""
            },
            save = { lifecycleScope.launch { save() } },
            pickColor = { showThemePicker() },
            clearColor = { clearColor() },
            pickIcon = { showIconPicker() },
            discard = { finish() },
            optionButton = optionButton,
            extensionContent = extensionContent,
        )
    }

    companion object {
        private const val EXTRA_SELECTED_THEME = "extra_selected_theme"
        private const val EXTRA_SELECTED_ICON = "extra_selected_icon"
        private const val FRAG_TAG_COLOR_PICKER = "frag_tag_color_picker"
    }
}
