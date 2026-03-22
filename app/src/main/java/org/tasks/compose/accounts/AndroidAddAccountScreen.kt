package org.tasks.compose.accounts

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import org.tasks.PlatformConfiguration
import org.tasks.themes.TasksSettingsTheme

@Composable
fun AddAccountScreenWrapper(
    configuration: PlatformConfiguration,
    hasTasksAccount: Boolean,
    hasPro: Boolean,
    needsConsent: Boolean,
    onBack: () -> Unit,
    signIn: (Platform) -> Unit,
    openUrl: (Platform) -> Unit,
    openLegalUrl: (String) -> Unit,
    onConsent: suspend () -> Unit = {},
    onNameYourPriceInfo: () -> Unit = {},
) {
    BackHandler(onBack = onBack)
    AddAccountScreen(
        configuration = configuration,
        hasTasksAccount = hasTasksAccount,
        hasPro = hasPro,
        needsConsent = needsConsent,
        onBack = onBack,
        signIn = signIn,
        openUrl = openUrl,
        openLegalUrl = openLegalUrl,
        onConsent = onConsent,
        onNameYourPriceInfo = onNameYourPriceInfo,
    )
}

@PreviewLightDark
@PreviewScreenSizes
@PreviewFontScale
@Composable
fun AddAccountPreview() {
    TasksSettingsTheme {
        AddAccountScreen(
            configuration = PlatformConfiguration(
                supportsTasksOrg = true,
                supportsCaldav = true,
                supportsGoogleTasks = true,
                supportsMicrosoft = true,
                supportsOpenTasks = true,
                supportsEteSync = true,
                supportsInAppPurchase = true,
            ),
            hasTasksAccount = false,
            hasPro = false,
            needsConsent = false,
            onBack = {},
            signIn = {},
            openUrl = {},
            openLegalUrl = {},
        )
    }
}
