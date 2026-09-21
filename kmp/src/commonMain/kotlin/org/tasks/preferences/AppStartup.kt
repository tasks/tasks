package org.tasks.preferences

import org.tasks.time.DateTimeUtils2

/**
 * Records the install version and install date on first launch. Safe to call
 * on every startup: idempotent once the values have been set. Intended to run
 * before any UI is shown so that subsequent pre-UI upgrade logic can rely on
 * these values being populated.
 */
suspend fun AppPreferences.recordInstallIfNeeded(versionCode: Int) {
    if (versionCode <= 0) return
    if (getInstallVersion() == 0) {
        setInstallVersion(versionCode)
        setInstallDate(DateTimeUtils2.currentTimeMillis())
    }
    if (getDeviceInstallVersion() == 0) {
        setDeviceInstallVersion(versionCode)
    }
}
