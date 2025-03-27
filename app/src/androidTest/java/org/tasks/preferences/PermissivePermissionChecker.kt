package org.tasks.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

class PermissivePermissionChecker(@ApplicationContext context: Context) : PermissionChecker(context) {
    override fun canAccessCalendars() = true

    override fun canAccessForegroundLocation() = true

    override fun canAccessBackgroundLocation() = true
}