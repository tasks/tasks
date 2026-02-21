package org.tasks.data

import androidx.room.ColumnInfo
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_UNKNOWN
import org.tasks.data.entity.CaldavCalendar.Companion.INVITE_UNKNOWN

data class PrincipalWithAccess(
    val id: Long = 0,
    val list: Long = 0,
    val invite: Int = INVITE_UNKNOWN,
    val access: Int = ACCESS_UNKNOWN,
    val href: String = "",
    @ColumnInfo(name = "display_name") val displayName: String? = null,
) {
    val inviteStatus get() = invite
    val email: String?
        get() = href.takeIf { it.startsWith("mailto:") }?.removePrefix("mailto:")
    val name: String
        get() = displayName
            ?: href
                .replace(MAILTO, "")
                .replaceFirst(LAST_SEGMENT, "$1")

    companion object {
        private val MAILTO = "^mailto:".toRegex()
        private val LAST_SEGMENT = ".*/([^/]+).*".toRegex()
    }
}
