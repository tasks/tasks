package org.tasks.activities

import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.compose.IconPickerActivity.Companion.launchIconPicker
import org.tasks.compose.IconPickerActivity.Companion.registerForIconPickerResult
import org.tasks.compose.components.TasksIcon
import org.tasks.dialogs.ColorPalettePicker
import org.tasks.dialogs.ColorPalettePicker.Companion.newColorPalette
import org.tasks.dialogs.ColorPickerAdapter.Palette
import org.tasks.dialogs.ColorWheelPicker
import org.tasks.dialogs.DialogBuilder
import org.tasks.extensions.addBackPressedCallback
import org.tasks.injection.ThemedInjectingAppCompatActivity
import org.tasks.themes.ColorProvider
import org.tasks.themes.DrawableUtil
import org.tasks.themes.TasksTheme
import org.tasks.themes.ThemeColor
import javax.inject.Inject

abstract class BaseListSettingsActivity : ThemedInjectingAppCompatActivity(), Toolbar.OnMenuItemClickListener, ColorPalettePicker.ColorPickedCallback, ColorWheelPicker.ColorPickedCallback {
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var colorProvider: ColorProvider
    protected abstract val defaultIcon: String
    protected var selectedColor = 0
    protected var selectedIcon = MutableStateFlow<String?>(null)

    private lateinit var clear: View
    private lateinit var color: TextView
    protected lateinit var toolbar: Toolbar
    protected lateinit var colorRow: ViewGroup

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
        findViewById<ComposeView>(R.id.icon).setContent {
            TasksTheme(theme = tasksTheme.themeBase.index) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TasksIcon(
                        label = selectedIcon.collectAsStateWithLifecycle().value ?: defaultIcon
                    )
                    Spacer(modifier = Modifier.width(34.dp))
                    Text(
                        text = "Icon",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 18.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        findViewById<View>(R.id.icon_row).setOnClickListener { showIconPicker() }
        toolbar = view.findViewById(R.id.toolbar)
        if (savedInstanceState != null) {
            selectedColor = savedInstanceState.getInt(EXTRA_SELECTED_THEME)
            selectedIcon.update { savedInstanceState.getString(EXTRA_SELECTED_ICON) }
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
        outState.putString(EXTRA_SELECTED_ICON, selectedIcon.value)
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

    val launcher = registerForIconPickerResult { selected ->
        selectedIcon.update { selected }
    }

    private fun showIconPicker() {
        launcher.launchIconPicker(this, selectedIcon.value)
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
    }

    companion object {
        private const val EXTRA_SELECTED_THEME = "extra_selected_theme"
        private const val EXTRA_SELECTED_ICON = "extra_selected_icon"
        private const val FRAG_TAG_ICON_PICKER = "frag_tag_icon_picker"
        private const val FRAG_TAG_COLOR_PICKER = "frag_tag_color_picker"
    }
}
