package org.tasks.notifications

import co.touchlab.kermit.Logger
import dev.nucleusframework.notification.windows.AdaptiveText
import dev.nucleusframework.notification.windows.DismissalReason
import dev.nucleusframework.notification.windows.HistoryEntry
import dev.nucleusframework.notification.windows.ShortcutPolicy
import dev.nucleusframework.notification.windows.ToastActions
import dev.nucleusframework.notification.windows.ToastAudio
import dev.nucleusframework.notification.windows.ToastBindingGeneric
import dev.nucleusframework.notification.windows.ToastButton
import dev.nucleusframework.notification.windows.ToastContent
import dev.nucleusframework.notification.windows.ToastNotificationListener
import dev.nucleusframework.notification.windows.ToastVisual
import dev.nucleusframework.notification.windows.WindowsNotificationCenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.di.appName

private const val TAG = "WindowsNotifications"

class NucleusWindowsNotifications private constructor(
    private val listener: NotificationActionListener,
) : PlatformNotifications {
    override val supportsActions = true

    override val actionsSurviveRestart = false

    private val signals = object : ToastNotificationListener {
        override fun onActivated(
            tag: String,
            group: String,
            arguments: String,
            userInputs: Map<String, String>,
        ) {
            val taskId = taskIdFrom(tag)

            val action = NotificationAction.fromKey(arguments)
            if (taskId == null || action == null) {
                Logger.d(tag = TAG) { "Ignoring activation $arguments for $tag" }
            } else {
                listener.onAction(taskId, action)
            }
        }

        override fun onDismissed(tag: String, group: String, reason: DismissalReason) {
            val taskId = taskIdFrom(tag) ?: return

            if (reason == DismissalReason.USER_CANCELED) {
                listener.onDismissed(taskId)
            }
        }

        override fun onFailed(tag: String, group: String, errorCode: Int) {
            Logger.w(tag = TAG) { "Toast failed for $tag (0x${errorCode.toString(HEX)})" }
        }
    }

    override suspend fun show(
        taskId: Long,
        title: String,
        body: String?,
        actions: List<NotificationAction>,
        alert: Alert,
    ): Boolean {
        val buttons = actions
            .filter { it != NotificationAction.OPEN }
            .map { ToastButton(content = NotificationAction.label(it), arguments = it.key) }
        val content = ToastContent(
            visual = ToastVisual(
                binding = ToastBindingGeneric(
                    children = buildList {
                        add(AdaptiveText(dropCharactersXmlCannotCarry(title)))
                        body?.takeIf { it.isNotBlank() }?.let { add(AdaptiveText(dropCharactersXmlCannotCarry(it))) }
                    },
                ),
            ),
            actions = buttons.takeIf { it.isNotEmpty() }?.let { ToastActions(buttons = it) },

            audio = if (alert.sound) null else ToastAudio(silent = true),

            launch = NotificationAction.OPEN.key,
        )
        return withContext(Dispatchers.IO) {
            val answered = awaiting<String?>(TAG, "Posting $taskId") { answer ->
                WindowsNotificationCenter.show(
                    content = content,
                    tag = taskId.toString(),
                    group = GROUP,
                    suppressPopup = alert == Alert.SUPPRESSED,
                ) { error -> answer(error) }
            } ?: return@withContext false
            val error = answered.value
            if (error == null) {
                Logger.d(tag = TAG) { "Toast shown for $taskId alert=$alert actions=${actions.size}" }
                true
            } else {
                Logger.w(tag = TAG) { "Toast not posted for $taskId ($error)" }
                false
            }
        }
    }

    override suspend fun dismiss(taskIds: List<Long>): Set<Long> {
        if (taskIds.isEmpty()) {
            return emptySet()
        }
        Logger.d(tag = TAG) { "Removing toasts for $taskIds" }
        withContext(Dispatchers.IO) {
            taskIds.forEach { WindowsNotificationCenter.remove(tag = it.toString(), group = GROUP) }
        }
        return taskIds.toSet()
    }

    override suspend fun delivered(): DeliveredQuery = withContext(Dispatchers.IO) {
        val answered = awaiting<Pair<List<HistoryEntry>, String?>>(
            TAG,
            "Reading the toast history",
        ) { answer ->
            WindowsNotificationCenter.getHistory { entries, error -> answer(entries to error) }
        } ?: return@withContext DeliveredQuery.Unknown
        val (entries, error) = answered.value
        if (error != null) {
            Logger.w(tag = TAG) { "Could not read toast history ($error)" }
            DeliveredQuery.Unknown
        } else {
            Logger.d(tag = TAG) { "Toast history: ${entries.map { "${it.group}/${it.tag}" }}" }
            DeliveredQuery.Known(
                entries.filter { postedByUs(it.group) }.mapNotNullTo(mutableSetOf()) { taskIdFrom(it.tag) },
            )
        }
    }

    override fun close(): Boolean {
        WindowsNotificationCenter.removeListener(signals)
        Logger.d(tag = TAG) { "Clearing group $GROUP on the way out" }
        WindowsNotificationCenter.removeGroup(GROUP)
        WindowsNotificationCenter.uninitialize()
        return true
    }

    companion object {
        private const val GROUP = "tasks"

        private const val HEX = 16

        private const val AUMID_PROPERTY = "tasks.aumid"

        private const val PACKAGED_AUMID_PROPERTY = "app.windows.userModelID"

        private const val EXECUTABLE_TYPE_PROPERTY = "nucleus.executable.type"

        private const val APPX = "appx"

        private const val INSTALLED_PACKAGE_DIR = "\\WindowsApps\\"

        internal fun installedAsAPackage(property: (String) -> String?): Boolean =
            property("app.dir")?.contains(INSTALLED_PACKAGE_DIR, ignoreCase = true) == true

        internal fun packagedMode(property: (String) -> String?): Boolean =
            property(EXECUTABLE_TYPE_PROPERTY)?.equals(APPX, ignoreCase = true)
                ?: installedAsAPackage(property)

        internal fun aumid(packaged: Boolean, property: (String) -> String?): String? =
            property(AUMID_PROPERTY)?.takeIf { it.isNotBlank() }
                ?: PACKAGED_AUMID_PROPERTY
                    .takeUnless { packaged }
                    ?.let { property(it)?.takeIf { value -> value.isNotBlank() } }

        internal fun postedByUs(group: String): Boolean = group == GROUP || group.isBlank()

        internal fun taskIdFrom(tag: String): Long? = tag.trim().toLongOrNull()

        internal fun dropCharactersXmlCannotCarry(value: String): String {
            val safe = StringBuilder(value.length)
            var i = 0
            while (i < value.length) {
                val c = value[i]
                when {
                    c == '\t' || c == '\n' || c == '\r' -> safe.append(c)
                    c < ' ' -> {}

                    c.isHighSurrogate() -> {
                        val next = value.getOrNull(i + 1)
                        if (next != null && next.isLowSurrogate()) {
                            safe.append(c).append(next)
                            i++
                        }
                    }

                    c.isLowSurrogate() -> {}

                    c == '\uFFFE' || c == '\uFFFF' -> {}
                    else -> safe.append(c)
                }
                i++
            }
            return safe.toString()
        }

        fun create(listener: NotificationActionListener): NucleusWindowsNotifications? = try {
            val packaged = packagedMode { System.getProperty(it) }
            val aumid = aumid(packaged) { System.getProperty(it) }
            Logger.i(tag = TAG) {
                "app.dir=${System.getProperty("app.dir")} packaged=$packaged " +
                        "aumid=${aumid ?: "<package-identity>"}"
            }
            if (packaged && System.getProperty(EXECUTABLE_TYPE_PROPERTY) == null) {
                System.setProperty(EXECUTABLE_TYPE_PROPERTY, APPX)
            }
            if (!WindowsNotificationCenter.isAvailable) {
                Logger.w(tag = TAG) { "Native toast bridge unavailable" }
                null
            } else if (!WindowsNotificationCenter.initialize(
                    aumid = aumid,
                    appName = appName,
                    shortcutPolicy = ShortcutPolicy.IGNORE,
                )
            ) {
                Logger.w(tag = TAG) { "Toast subsystem would not initialize" }
                null
            } else {
                NucleusWindowsNotifications(listener)
                    .also { WindowsNotificationCenter.addListener(it.signals) }
            }
        } catch (e: Throwable) {
            Logger.w(throwable = e, tag = TAG) { "Toast notifications unavailable" }
            null
        }
    }
}
