package org.tasks.activities

import android.content.DialogInterface
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.dialogs.ColorPalettePicker
import org.tasks.dialogs.ColorPalettePicker.Companion.newColorPalette
import org.tasks.dialogs.ColorPickerAdapter.Palette
import org.tasks.dialogs.ColorWheelPicker
import org.tasks.dialogs.DialogBuilder
import org.tasks.dialogs.IconPickerDialog
import org.tasks.dialogs.IconPickerDialog.IconPickerCallback
import org.tasks.extensions.addBackPressedCallback
import org.tasks.injection.ThemedInjectingAppCompatActivity
import org.tasks.themes.ColorProvider
import org.tasks.themes.CustomIcons.getIconResId
import org.tasks.themes.DrawableUtil
import org.tasks.themes.ThemeColor
import javax.inject.Inject

abstract class BaseListSettingsActivity : ThemedInjectingAppCompatActivity(), IconPickerCallback, Toolbar.OnMenuItemClickListener, ColorPalettePicker.ColorPickedCallback, ColorWheelPicker.ColorPickedCallback {
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var colorProvider: ColorProvider
    protected var selectedColor = 0
    protected var selectedIcon = -1

    private lateinit var clear: View
    private lateinit var color: TextView
    private lateinit var icon: TextView
    protected lateinit var toolbar: Toolbar
    protected lateinit var colorRow: ViewGroup
    protected abstract val defaultIcon: Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = bind()
        setContentView(view)
        clear = findViewById<View>(R.id.clear).apply {
            setOnClickListener { clearColor() }
        }
        color = findViewById(R.id.color)
        colorRow = findViewById<ViewGroup>(R.id.color_row).apply {
            setOnClickListener { showThemePicker() }
        }
        icon = findViewById(R.id.icon)
        findViewById<View>(R.id.icon_row).setOnClickListener { showIconPicker() }
        toolbar = view.findViewById(R.id.toolbar)
        if (savedInstanceState != null) {
            selectedColor = savedInstanceState.getInt(EXTRA_SELECTED_THEME)
            selectedIcon = savedInstanceState.getInt(EXTRA_SELECTED_ICON)
        }
        toolbar.title = toolbarTitle
        toolbar.navigationIcon = getDrawable(R.drawable.ic_outline_save_24px)
        toolbar.setNavigationOnClickListener { lifecycleScope.launch { save() } }
        if (!isNew) {
            toolbar.inflateMenu(R.menu.menu_tag_settings)
        }
        toolbar.setOnMenuItemClickListener(this)

        addBackPressedCallback {
            discard()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_SELECTED_THEME, selectedColor)
        outState.putInt(EXTRA_SELECTED_ICON, selectedIcon)
    }

    protected abstract fun hasChanges(): Boolean
    protected abstract suspend fun save()
    protected abstract val isNew: Boolean
    protected abstract val toolbarTitle: String?
    protected abstract suspend fun delete()
    protected abstract fun bind(): View
    protected open fun discard() {
        if (!hasChanges()) {
            finish()
        } else {
            dialogBuilder
                    .newDialog(R.string.discard_changes)
                    .setPositiveButton(R.string.discard) { _, _ -> finish() }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        }
    }

    private fun clearColor() {
        onColorPicked(0)
    }

    private fun showThemePicker() {
        newColorPalette(null, 0, selectedColor, Palette.COLORS)
                .show(supportFragmentManager, FRAG_TAG_COLOR_PICKER)
    }

    private fun showIconPicker() {
        IconPickerDialog.newIconPicker(selectedIcon).show(supportFragmentManager, FRAG_TAG_ICON_PICKER)
    }

    override fun onSelected(dialogInterface: DialogInterface, icon: Int) {
        selectedIcon = icon
        dialogInterface.dismiss()
        updateTheme()
    }

    override fun onColorPicked(color: Int) {
        selectedColor = color
        updateTheme()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        if (item.itemId == R.id.delete) {
            promptDelete()
            return true
        }
        return onOptionsItemSelected(item)
    }

    protected open fun promptDelete() {
        dialogBuilder
                .newDialog(R.string.delete_tag_confirmation, toolbarTitle)
                .setPositiveButton(R.string.delete) { _, _ -> lifecycleScope.launch { delete() } }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    protected fun updateTheme() {
        val themeColor: ThemeColor
        if (selectedColor == 0) {
            themeColor = this.themeColor
            DrawableUtil.setLeftDrawable(this, color, R.drawable.ic_outline_not_interested_24px)
            DrawableUtil.getLeftDrawable(color).setTint(getColor(R.color.icon_tint_with_alpha))
            clear.visibility = View.GONE
        } else {
            themeColor = colorProvider.getThemeColor(selectedColor, true)
            DrawableUtil.setLeftDrawable(this, color, R.drawable.color_picker)
            val leftDrawable = DrawableUtil.getLeftDrawable(color)
            (if (leftDrawable is LayerDrawable) leftDrawable.getDrawable(0) else leftDrawable)
                    .setTint(themeColor.primaryColor)
            clear.visibility = View.VISIBLE
        }
        themeColor.applyToNavigationBar(this)
        val icon = getIconResId(selectedIcon) ?: getIconResId(defaultIcon)
        DrawableUtil.setLeftDrawable(this, this.icon, icon!!)
        DrawableUtil.getLeftDrawable(this.icon).setTint(getColor(R.color.icon_tint_with_alpha))
    }

    companion object {
        private const val EXTRA_SELECTED_THEME = "extra_selected_theme"
        private const val EXTRA_SELECTED_ICON = "extra_selected_icon"
        private const val FRAG_TAG_ICON_PICKER = "frag_tag_icon_picker"
        private const val FRAG_TAG_COLOR_PICKER = "frag_tag_color_picker"
    }
}