package org.tasks

import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.tasks.analytics.AnalyticsEvents
import org.tasks.compose.pricing.PricingMode

@Serializable
data object WelcomeDestination : NavKey

@Serializable
data object AddAccountDestination : NavKey

@Serializable
data object TaskListDestination : NavKey

@Serializable
data class TaskEditDestination(
    val taskId: Long,
    val remoteId: String,
    val listId: Long? = null,
    val tagUuid: String? = null,
    val isSubtaskDraft: Boolean = false,
) : NavKey

@Serializable
data object CaldavSignInDestination : NavKey

@Serializable
data object EtebaseSignInDestination : NavKey

@Serializable
data object SettingsDestination : NavKey

@Serializable
data object LinkDesktopDestination : NavKey

@Serializable
data class DesktopProDestination(val source: String? = null) : NavKey

@Serializable
data class PricingDestination(
    val mode: PricingMode = PricingMode.BOTH,
    val source: String = AnalyticsEvents.SOURCE_SETTINGS,
) : NavKey

internal val FloatingToolbarBottomMargin = 24.dp
