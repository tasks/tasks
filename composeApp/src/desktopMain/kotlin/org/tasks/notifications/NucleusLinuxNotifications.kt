package org.tasks.notifications

import co.touchlab.kermit.Logger
import dev.nucleusframework.freedesktop.icons.FreedesktopIcon
import dev.nucleusframework.notification.linux.CloseReason
import dev.nucleusframework.notification.linux.LinuxNotificationCenter
import dev.nucleusframework.notification.linux.LinuxNotificationListener
import dev.nucleusframework.notification.linux.NotificationHints
import dev.nucleusframework.notification.linux.Urgency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.di.appName
import org.tasks.time.monotonicMillis
import dev.nucleusframework.notification.linux.Notification as LinuxNotification
import dev.nucleusframework.notification.linux.NotificationAction as LinuxAction

private const val TAG = "LinuxNotifications"

class NucleusLinuxNotifications internal constructor(
    override val supportsActions: Boolean,
    private val supportsBodyMarkup: Boolean,
    private val listener: NotificationActionListener,
    private val elapsedRealtime: () -> Long,
) : PlatformNotifications {
    override val supportsOpen get() = supportsActions

    override val actionsSurviveRestart = false

    private val ids = LinuxNotificationIds(elapsedRealtime = elapsedRealtime)

    private val signals = object : LinuxNotificationListener {
        override fun onActionInvoked(notificationId: Int, actionKey: String) {
            val taskId = ids.actionInvoked(notificationId)
            val action = actionFor(actionKey)
            if (taskId == null || action == null) {
                Logger.d(tag = TAG) { "Ignoring action $actionKey for $notificationId" }
            } else {
                listener.onAction(taskId, action)
            }
        }

        override fun onClosed(notificationId: Int, reason: CloseReason) {
            val (taskId, acted) = ids.closed(notificationId)

            if (taskId != null && !acted && reason == CloseReason.DISMISSED) {
                listener.onDismissed(taskId)
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
        val buttons = if (supportsActions) {
            actions.map { LinuxAction(key = it.wireKey, label = NotificationAction.label(it)) }
        } else {
            emptyList()
        }
        return withContext(Dispatchers.IO) {
            val replaces = ids.idFor(taskId) ?: NOT_REPLACING
            val id = LinuxNotificationCenter.notify(
                LinuxNotification(
                    appName = appName,
                    replacesId = replaces,
                    appIcon = FreedesktopIcon.Custom(CONVEYOR_INSTALLED_ICON_NAME),
                    summary = title,
                    body = bodyFor(body, supportsBodyMarkup),
                    actions = buttons,
                    hints = NotificationHints(
                        urgency = Urgency.NORMAL,

                        desktopEntry = INSTALLED_DESKTOP_ENTRY_NAME,

                        suppressSound = if (alert.sound) null else true,
                    ),

                    expireTimeout = EXPIRE_NEVER,
                ),
            )
            if (id == NOTHING_WENT_OUT) {
                Logger.w(tag = TAG) { "Notification server refused the post for $taskId" }
                false
            } else {
                Logger.d(tag = TAG) { "Posted $taskId as $id alert=$alert actions=${actions.size}" }
                if (replaces == NOT_REPLACING && ids.counterWentBackwards(id)) {
                    val dropped = ids.serverRestarted()
                    Logger.i(tag = TAG) {
                        "Notification server restarted, dropped ${dropped.size} id(s)"
                    }
                    dropped.forEach { listener.onEvicted(it) }
                }
                ids.posted(taskId = taskId, id = id)
                true
            }
        }
    }

    override suspend fun platformId(taskId: Long): Long? = ids.idFor(taskId)?.toLong()

    override suspend fun adopt(platformIds: Map<Long, Long>) {
        val recovered = platformIds.mapValues { (_, id) -> id.toInt() }
        Logger.d(tag = TAG) { "Adopting ${recovered.size} id(s) from an earlier run" }
        ids.adopt(recovered)
    }

    override suspend fun dismiss(taskIds: List<Long>): Set<Long> {
        if (taskIds.isEmpty()) {
            return emptySet()
        }
        val closing = ids.dismissing(taskIds)
        Logger.d(tag = TAG) { "Closing $taskIds (ids $closing)" }
        withContext(Dispatchers.IO) {
            closing.values.forEach { LinuxNotificationCenter.closeNotification(it) }
        }

        val unaddressable = taskIds.filterNot { it in closing }
        if (unaddressable.isNotEmpty()) {
            Logger.d(tag = TAG) { "Nothing on screen for $unaddressable" }
        }
        return taskIds.toSet()
    }

    override fun close(): Boolean {
        closeOutstanding()
        LinuxNotificationCenter.removeListener(signals)
        return true
    }

    private fun closeOutstanding() {
        val live = ids.takeAll()
        if (live.isEmpty()) {
            return
        }
        live.forEach { LinuxNotificationCenter.closeNotification(it) }
        Logger.d(tag = TAG) { "Closed ${live.size} notification(s) on the way out" }
    }

    companion object {
        private const val INSTALLED_DESKTOP_ENTRY_NAME = "org.tasks"

        private const val CONVEYOR_INSTALLED_ICON_NAME = "tasksorg-llc-tasks-org"

        private const val EXPIRE_NEVER = 0

        private const val NOTHING_WENT_OUT = 0

        private const val NOT_REPLACING = 0

        internal const val CAPABILITY_ACTIONS = "actions"
        internal const val CAPABILITY_BODY_MARKUP = "body-markup"

        internal val NotificationAction.wireKey: String
            get() = if (this == NotificationAction.OPEN) LinuxAction.DEFAULT_KEY else key

        internal fun actionFor(key: String): NotificationAction? =
            if (key == LinuxAction.DEFAULT_KEY) NotificationAction.OPEN else NotificationAction.fromKey(key)

        internal fun bodyFor(body: String?, supportsBodyMarkup: Boolean): String =
            body.orEmpty().let { if (supportsBodyMarkup) escapeMarkup(it) else it }

        internal fun escapeMarkup(value: String): String = value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

        fun create(listener: NotificationActionListener): NucleusLinuxNotifications? {
            val capabilities = try {
                if (!LinuxNotificationCenter.isAvailable) {
                    Logger.w(tag = TAG) { "Native notification bridge unavailable" }
                    return null
                }
                LinuxNotificationCenter.getCapabilities()
            } catch (e: Throwable) {
                Logger.w(throwable = e, tag = TAG) { "Native notification bridge failed to load" }
                return null
            }
            if (capabilities.isEmpty()) {
                Logger.w(tag = TAG) { "No notification server answering" }
                return null
            }
            Logger.i(tag = TAG) { "Connected, capabilities=$capabilities" }
            return NucleusLinuxNotifications(
                supportsActions = capabilities.contains(CAPABILITY_ACTIONS),
                supportsBodyMarkup = capabilities.contains(CAPABILITY_BODY_MARKUP),
                listener = listener,
                elapsedRealtime = { monotonicMillis() },
            ).also { LinuxNotificationCenter.addListener(it.signals) }
        }
    }
}
