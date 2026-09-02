package org.tasks.notifications

import co.touchlab.kermit.Logger
import dev.nucleusframework.notification.AuthorizationOption
import dev.nucleusframework.notification.AuthorizationStatus
import dev.nucleusframework.notification.CategoryOption
import dev.nucleusframework.notification.DeliveredNotification
import dev.nucleusframework.notification.InterruptionLevel
import dev.nucleusframework.notification.NotificationCenter
import dev.nucleusframework.notification.NotificationCenterDelegate
import dev.nucleusframework.notification.NotificationContent
import dev.nucleusframework.notification.NotificationSettings
import dev.nucleusframework.notification.NotificationSound
import dev.nucleusframework.notification.PresentationOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import dev.nucleusframework.notification.NotificationAction as MacAction
import dev.nucleusframework.notification.NotificationCategory as MacCategory
import dev.nucleusframework.notification.NotificationRequest as MacRequest
import dev.nucleusframework.notification.NotificationResponse as MacResponse

private const val TAG = "MacNotifications"

class NucleusMacNotifications private constructor(
    private val listener: NotificationActionListener,
) : PlatformNotifications {
    override val supportsActions = true

    private val categoryLock = Mutex()

    private val promptLock = Mutex()

    @Volatile
    private var categoriesRegistered = false

    private val delegate = object : NotificationCenterDelegate {
        override fun willPresent(notification: DeliveredNotification): Set<PresentationOption> =
            PRESENT_FOREGROUND

        override fun didReceive(response: MacResponse) {
            route(
                listener = listener,
                identifier = response.actionIdentifier,
                taskId = response.notification.identifier.toLongOrNull(),
            )
        }
    }

    override suspend fun permission(): NotificationPermission {
        val settings = awaiting<NotificationSettings>(TAG, "Reading the notification settings") { answer ->
            NotificationCenter.getNotificationSettings(answer)
        } ?: return NotificationPermission.NOT_DETERMINED
        return when (settings.value.authorizationStatus) {
            AuthorizationStatus.NOT_DETERMINED -> NotificationPermission.NOT_DETERMINED
            AuthorizationStatus.DENIED -> NotificationPermission.DENIED
            AuthorizationStatus.AUTHORIZED,
            AuthorizationStatus.PROVISIONAL,
            AuthorizationStatus.EPHEMERAL,
            -> NotificationPermission.GRANTED
        }
    }

    override suspend fun requestPermission(): Boolean {
        if (!promptLock.tryLock()) {
            Logger.d(tag = TAG) { "The permission prompt is already on screen" }
            return false
        }
        val answered = try {
            awaiting<Authorization>(TAG, "The permission prompt", PROMPT_TIMEOUT_MS) { answer ->
                NotificationCenter.requestAuthorization(AUTHORIZATION_OPTIONS) { granted, error ->
                    answer(Authorization(granted, error))
                }
            }
        } finally {
            promptLock.unlock()
        }
        val answer = answered?.value ?: return false
        return when {
            answer.granted -> true

            answer.error != null -> {
                Logger.w(tag = TAG) { "Could not ask for permission (${answer.error})" }
                false
            }
            else -> {
                Logger.w(tag = TAG) {
                    "Notification permission refused. It can't be asked for again - it has to be " +
                            "turned on under Notifications in System Settings."
                }
                true
            }
        }
    }

    override suspend fun show(
        taskId: Long,
        title: String,
        body: String?,
        actions: List<NotificationAction>,
        alert: Alert,
    ): Boolean {
        registerCategories()
        val request = MacRequest(
            identifier = taskId.toString(),
            content = NotificationContent(
                title = title,
                body = body.orEmpty(),

                sound = if (alert.sound) NotificationSound.Default else null,
                categoryIdentifier = categoryFor(actions),
                interruptionLevel = if (alert == Alert.DEFAULT) {
                    InterruptionLevel.ACTIVE
                } else {
                    InterruptionLevel.PASSIVE
                },
            ),
        )
        val posted = withContext(Dispatchers.IO) {
            awaiting<String?>(TAG, "Posting $taskId") { answer ->
                NotificationCenter.add(request) { answer(it) }
            }
        } ?: return false
        val error = posted.value
        return if (error == null) {
            Logger.d(tag = TAG) { "Posted $taskId alert=$alert actions=${actions.size}" }
            true
        } else {
            Logger.w(tag = TAG) { "Not posted for $taskId ($error)" }
            false
        }
    }

    override suspend fun delivered(): DeliveredQuery {
        registerCategories()
        val answered = withContext(Dispatchers.IO) {
            awaiting<List<DeliveredNotification>>(TAG, "Reading the delivered notifications") { answer ->
                NotificationCenter.getDeliveredNotifications(answer)
            }
        } ?: return DeliveredQuery.Unknown
        val delivered = answered.value
        Logger.d(tag = TAG) { "Delivered: ${delivered.map { it.identifier }}" }
        return DeliveredQuery.Known(
            delivered.mapNotNullTo(mutableSetOf()) { it.identifier.trim().toLongOrNull() },
        )
    }

    override suspend fun dismiss(taskIds: List<Long>): Set<Long> {
        if (taskIds.isEmpty()) {
            return emptySet()
        }
        Logger.d(tag = TAG) { "Removing delivered notifications for $taskIds" }
        withContext(Dispatchers.IO) {
            NotificationCenter.removeDeliveredNotifications(taskIds.map { it.toString() })
        }

        return taskIds.toSet()
    }

    override fun close() = false

    private suspend fun registerCategories() {
        if (categoriesRegistered) {
            return
        }

        val complete = NotificationAction.labelOrNull(NotificationAction.COMPLETE)
        val snooze = NotificationAction.labelOrNull(NotificationAction.SNOOZE)
        categoryLock.withLock {
            if (categoriesRegistered) {
                return
            }
            NotificationCenter.setNotificationCategories(
                categories(
                    complete = complete ?: NotificationAction.COMPLETE.fallbackLabel,
                    snooze = snooze ?: NotificationAction.SNOOZE.fallbackLabel,
                )
            )

            categoriesRegistered = complete != null && snooze != null
        }
    }

    private class Authorization(val granted: Boolean, val error: String?)

    companion object {
        internal const val CATEGORY_ACTIONABLE = "org.tasks.reminder"
        internal const val CATEGORY_SNOOZE_ONLY = "org.tasks.reminder.snooze"

        internal fun categoryFor(actions: List<NotificationAction>): String =
            if (NotificationAction.COMPLETE in actions) CATEGORY_ACTIONABLE else CATEGORY_SNOOZE_ONLY

        private val REPORTS_DISMISSAL = setOf(CategoryOption.CUSTOM_DISMISS_ACTION)

        internal fun categories(complete: String, snooze: String): Set<MacCategory> = setOf(
            MacCategory(
                identifier = CATEGORY_ACTIONABLE,
                actions = listOf(
                    MacAction(identifier = NotificationAction.COMPLETE.key, title = complete),
                    MacAction(identifier = NotificationAction.SNOOZE.key, title = snooze),
                ),
                options = REPORTS_DISMISSAL,
            ),
            MacCategory(
                identifier = CATEGORY_SNOOZE_ONLY,
                actions = listOf(
                    MacAction(identifier = NotificationAction.SNOOZE.key, title = snooze),
                ),
                options = REPORTS_DISMISSAL,
            ),
        )

        private val AUTHORIZATION_OPTIONS =
            setOf(AuthorizationOption.ALERT, AuthorizationOption.SOUND)

        private val PRESENT_FOREGROUND = setOf(
            PresentationOption.BANNER,
            PresentationOption.LIST,
            PresentationOption.SOUND,
        )

        internal fun route(
            listener: NotificationActionListener,
            identifier: String,
            taskId: Long?,
        ) {
            if (taskId == null) {
                Logger.w(tag = TAG) { "Response with no task identifier" }
                return
            }
            when (identifier) {
                MacAction.DISMISS_ACTION_IDENTIFIER -> listener.onDismissed(taskId)
                MacAction.DEFAULT_ACTION_IDENTIFIER ->
                    listener.onAction(taskId, NotificationAction.OPEN)
                else -> NotificationAction.fromKey(identifier)
                    ?.let { listener.onAction(taskId, it) }
                    ?: Logger.d(tag = TAG) { "Ignoring response $identifier" }
            }
        }

        private const val PROMPT_TIMEOUT_MS = 5 * 60_000L

        private const val BUNDLE_CONTENTS = ".app/Contents/"

        private const val BUNDLE_LAUNCHER = ".app/Contents/MacOS/"

        internal fun someoneElsesBundle(command: String?): Boolean =
            command != null &&
                    command.contains(BUNDLE_CONTENTS) &&
                    !command.contains(BUNDLE_LAUNCHER)

        fun create(listener: NotificationActionListener): NucleusMacNotifications? = try {
            val command = ProcessHandle.current().info().command().orElse(null)
            if (someoneElsesBundle(command)) {
                Logger.i(tag = TAG) {
                    "Running on a JVM inside somebody else's app bundle ($command). " +
                            "UNUserNotificationCenter aborts the process there, so notifications " +
                            "are off. Use ./gradlew runDistributable to exercise them."
                }
                null
            } else if (!NotificationCenter.isAvailable) {
                Logger.i(tag = TAG) { "Not running from an app bundle, or the bridge did not load" }
                null
            } else {
                NucleusMacNotifications(listener)
                    .also { NotificationCenter.setDelegate(it.delegate) }
            }
        } catch (e: Throwable) {
            Logger.w(throwable = e, tag = TAG) { "UNUserNotificationCenter unavailable" }
            null
        }
    }
}
