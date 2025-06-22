package org.tasks.activities

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import com.todoroo.andlib.utility.AndroidUtilities.atLeastS
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseActivity
import org.tasks.compose.DeleteButton
import org.tasks.compose.IconPickerActivity.Companion.launchIconPicker
import org.tasks.compose.IconPickerActivity.Companion.registerForIconPickerResult
import org.tasks.compose.settings.ListSettingsContent
import org.tasks.compose.settings.ListSettingsScaffold
import org.tasks.data.UUIDHelper
import org.tasks.extensions.addBackPressedCallback
import org.tasks.filters.Filter
import org.tasks.icons.OutlinedGoogleMaterial
import org.tasks.intents.TaskIntents
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.themes.ColorProvider
import org.tasks.themes.Theme
import org.tasks.themes.contentColorFor
import org.tasks.widget.RequestPinWidgetReceiver
import org.tasks.widget.RequestPinWidgetReceiver.Companion.EXTRA_COLOR
import org.tasks.widget.RequestPinWidgetReceiver.Companion.EXTRA_FILTER
import org.tasks.widget.TasksWidget
import javax.inject.Inject


abstract class BaseListSettingsActivity : AppCompatActivity() {
    @Inject lateinit var tasksTheme: Theme
    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var firebase: Firebase
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var colorProvider: ColorProvider

    protected val baseViewModel: BaseListSettingsViewModel by viewModels()

    protected abstract val defaultIcon: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        baseViewModel.setIcon(defaultIcon)

        addBackPressedCallback {
            discard()
        }
    }

    protected abstract fun hasChanges(): Boolean
    protected abstract suspend fun save()
    protected val isNew: Boolean
        get() = filter == null

    protected abstract val filter: Filter?
    protected abstract val toolbarTitle: String?
    protected abstract suspend fun delete()
    protected open fun discard() {
        if (hasChanges()) {
            baseViewModel.promptDiscard(true)
        } else {
            finish()
        }
    }

    private val launcher = registerForIconPickerResult { selected ->
        baseViewModel.setIcon(selected)
    }

    fun showIconPicker() {
        launcher.launchIconPicker(this, baseViewModel.icon)
    }

    protected open fun promptDelete() { baseViewModel.promptDelete(true) }

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
        val viewState = baseViewModel.viewState.collectAsStateWithLifecycle().value
        val color = if (viewState.color == 0) MaterialTheme.colorScheme.primary else Color(viewState.color)
        ListSettingsScaffold(
            title = title,
            color = color,
            promptDiscard = viewState.promptDiscard,
            showProgress = viewState.showProgress,
            dismissDiscardPrompt = { baseViewModel.promptDiscard(false) },
            save = { lifecycleScope.launch { save() } },
            discard = { finish() },
            actions = optionButton,
            fab = fab,
        ) {
            ListSettingsContent(
                hasPro = remember { inventory.purchasedThemes() },
                color = viewState.color,
                colors = remember { colorProvider.getThemeColors() },
                icon = viewState.icon ?: defaultIcon,
                text = viewState.title,
                error = viewState.error,
                requestKeyboard = requestKeyboard,
                isNew = isNew,
                setText = {
                    baseViewModel.setTitle(it)
                    baseViewModel.setError("")
                },
                setColor = { baseViewModel.setColor(it) },
                pickIcon = { showIconPicker() },
                addShortcutToHome = { createShortcut(color) },
                addWidgetToHome = { createWidget() },
                extensionContent = extensionContent,
                purchase = {
                    startActivity(
                        Intent(this@BaseListSettingsActivity, PurchaseActivity::class.java)
                    )
                },
            )
        }
    }

    protected fun createShortcut(color: Color) {
        filter?.let { f ->
            val filterId = defaultFilterProvider.getFilterPreferenceValue(f)
            val shortcutInfo = ShortcutInfoCompat.Builder(this, UUIDHelper.newUUID())
                .setShortLabel(baseViewModel.title.takeIf { it.isNotBlank() } ?: getString(R.string.app_name))
                .setIcon(
                    baseViewModel.icon
                        ?.let { icon ->
                            try {
                                createShortcutIcon(
                                    context = this,
                                    backgroundColor = color,
                                    icon = icon,
                                    iconColor = contentColorFor(color.toArgb()),
                                )
                            } catch (e: Exception) {
                                firebase.reportException(e)
                                null
                            }
                        }
                        ?: createShortcutIcon(this, backgroundColor = color)
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

    protected fun createWidget() {
        val filter = filter ?: return
        val appWidgetManager = getSystemService(AppWidgetManager::class.java)
        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            val provider = ComponentName(this, TasksWidget::class.java)
            val configIntent = Intent(this, RequestPinWidgetReceiver::class.java).apply {
                action = RequestPinWidgetReceiver.ACTION_CONFIGURE_WIDGET
                putExtra(EXTRA_FILTER, defaultFilterProvider.getFilterPreferenceValue(filter))
                putExtra(EXTRA_COLOR, baseViewModel.color)
            }
            val successCallback = PendingIntent.getBroadcast(
                this,
                filter.hashCode(),
                configIntent,
                if (atLeastS()) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
            )
            appWidgetManager.requestPinAppWidget(provider, null, successCallback)
            firebase.logEvent(R.string.event_create_widget, R.string.param_type to "settings_activity")
        }
    }

    companion object {
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
