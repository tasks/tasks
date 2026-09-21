package org.tasks.billing

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.compose.settings.LinkDesktopScreen
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class LinkDesktopActivity : AppCompatActivity() {
    @Inject lateinit var theme: Theme
    @Inject lateinit var qrScanner: QrScanner
    @Inject lateinit var desktopLinkService: DesktopLinkService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TasksSettingsTheme(
                theme = theme.themeBase.index,
                primary = theme.themeColor.primaryColor,
            ) {
                LinkDesktopScreen(
                    onBack = { finish() },
                    onScan = { qrScanner.scan() },
                    onConfirm = { code -> desktopLinkService.confirmLink(code) },
                )
            }
        }
    }
}
