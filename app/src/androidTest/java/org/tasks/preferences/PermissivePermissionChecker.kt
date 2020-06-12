package org.tasks.preferences

import android.content.Context

class PermissivePermissionChecker(context: Application) : PermissionChecker(context) {
    override fun canAccessCalendars() = true

    override fun canAccessAccounts() = true

    override fun canAccessLocation() = true

    override fun canAccessMic() = true
}