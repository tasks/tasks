package org.tasks.activities

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.lifecycleScope
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.compose.DeleteButton
import org.tasks.compose.IconPickerActivity.Companion.launchIconPicker
import org.tasks.compose.IconPickerActivity.Companion.registerForIconPickerResult
import org.tasks.compose.settings.ListSettingsContent
import org.tasks.compose.settings.ListSettingsScaffold
import org.tasks.data.UUIDHelper
import org.tasks.dialogs.ColorPalettePicker
import org.tasks.dialogs.ColorPalettePicker.Companion.newColorPalette
import org.tasks.dialogs.ColorPickerAdapter.Palette
import org.tasks.dialogs.ColorWheelPicker
import org.tasks.extensions.addBackPressedCallback
import org.tasks.filters.Filter
import org.tasks.icons.OutlinedGoogleMaterial
import org.tasks.intents.TaskIntents
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.themes.ColorProvider
import org.tasks.themes.Theme
import org.tasks.themes.ThemeColor
import org.tasks.themes.contentColorFor
import javax.inject.Inject


abstract class BaseListSettingsActivity : AppCompatActivity(), ColorPalettePicker.ColorPickedCallback, ColorWheelPicker.ColorPickedCallback {
    @Inject lateinit var tasksTheme: Theme
    @Inject lateinit var colorProvider: ColorProvider
    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var firebase: Firebase

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
    protected val isNew: Boolean
        get() = filter == null

    protected abstract val filter: Filter?
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
            if (selectedColor == 0) tasksTheme.themeColor
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
            if (!isNew) DeleteButton(toolbarTitle ?: "") { delete() }
        },
        fab: @Composable () -> Unit = {},
        extensionContent: @Composable ColumnScope.() -> Unit = {},
    ) {
        ListSettingsScaffold(
            title = title,
            theme = if (colorState.value == Color.Unspecified)
                Color(tasksTheme.themeColor.primaryColor)
            else
                colorState.value,
            promptDiscard = promptDiscard.value,
            showProgress = showProgress.value,
            dismissDiscardPrompt = { promptDiscard.value = false },
            save = { lifecycleScope.launch { save() } },
            discard = { finish() },
            actions = optionButton,
            fab = fab,
        ) {
            ListSettingsContent(
                color = colorState.value,
                icon = selectedIcon.value ?: defaultIcon,
                text = textState.value,
                error = errorState.value,
                requestKeyboard = requestKeyboard,
                isNew = isNew,
                setText = {
                    textState.value = it
                    errorState.value = ""
                },
                pickColor = { showThemePicker() },
                clearColor = { clearColor() },
                pickIcon = { showIconPicker() },
                addToHome = { createShortcut() },
                extensionContent = extensionContent,
            )
        }
    }

    protected fun createShortcut() {
        filter?.let {
            val filterId = defaultFilterProvider.getFilterPreferenceValue(it)
            val iconColor = if (colorState.value == Color.Unspecified)
                Color(tasksTheme.themeColor.primaryColor)
            else
                colorState.value
            val shortcutInfo = ShortcutInfoCompat.Builder(this, UUIDHelper.newUUID())
                .setShortLabel(title)
                .setIcon(
                    selectedIcon.value
                        ?.let { icon ->
                            try {
                                createShortcutIcon(
                                    context = this,
                                    backgroundColor = iconColor,
                                    icon = icon,
                                    iconColor = contentColorFor(iconColor.toArgb()),
                                )
                            } catch (e: Exception) {
                                firebase.reportException(e)
                                null
                            }
                        }
                        ?: createShortcutIcon(
                            this,
                            backgroundColor = iconColor
                        )
                )
                .setIntent(TaskIntents.getTaskListByIdIntent(this, filterId))
                .build()

            val pinnedShortcutCallbackIntent = ShortcutManagerCompat
                .createShortcutResultIntent(this, shortcutInfo)

            // Create callback intent
            val successCallback = PendingIntent.getBroadcast(
                this, 0,
                pinnedShortcutCallbackIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

            ShortcutManagerCompat.requestPinShortcut(
                this,
                shortcutInfo,
                successCallback.intentSender
            )

            firebase.logEvent(R.string.event_create_shortcut, R.string.param_type to "settings_activity")
        }
    }

    companion object {
        private const val EXTRA_SELECTED_THEME = "extra_selected_theme"
        private const val EXTRA_SELECTED_ICON = "extra_selected_icon"
        private const val FRAG_TAG_COLOR_PICKER = "frag_tag_color_picker"

        fun createShortcutIcon(context: Context, backgroundColor: Color): IconCompat {
            val size = context.resources.getDimensionPixelSize(android.R.dimen.app_icon_size)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

            Canvas(bitmap).apply {
                // Draw circular background
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = backgroundColor.toArgb()
                }
                drawCircle(size/2f, size/2f, size/2f, paint)

                // Draw foreground icon
                val foreground = ResourcesCompat.getDrawable(
                    context.resources,
                    org.tasks.kmp.R.drawable.ic_launcher_no_shadow_foreground,
                    null
                )
                foreground?.let {
                    it.setBounds(0, 0, size, size)
                    it.draw(this)
                }
            }

            return IconCompat.createWithBitmap(bitmap)
        }

        fun createShortcutIcon(
            context: Context,
            backgroundColor: Color,
            icon: String,
            iconColor: Color = Color.White,
            iconSizeDp: Int = 24
        ): IconCompat {
            val size = context.resources.getDimensionPixelSize(android.R.dimen.app_icon_size)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

            Canvas(bitmap).apply {
                // Draw circular background
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = backgroundColor.toArgb()
                }
                drawCircle(size/2f, size/2f, size/2f, paint)

                // Create and draw IconicsDrawable
                val drawable = IconicsDrawable(context, OutlinedGoogleMaterial.getIcon("gmo_$icon")).apply {
                    colorInt = iconColor.toArgb()
                    sizeDp = iconSizeDp
                }

                // Center the icon
                val iconSize = (size * 0.5f).toInt()
                drawable.setBounds(
                    (size - iconSize) / 2,
                    (size - iconSize) / 2,
                    (size + iconSize) / 2,
                    (size + iconSize) / 2
                )
                drawable.draw(this)
            }

            return IconCompat.createWithBitmap(bitmap)
        }
    }
}
