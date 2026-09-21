package org.tasks.viewmodel

import androidx.datastore.preferences.core.booleanPreferencesKey
import org.tasks.billing.PurchaseState
import org.tasks.data.dao.CaldavDao
import org.tasks.filters.Filter
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.filters.getIcon
import org.tasks.kmp.org.tasks.themes.ColorProvider
import org.tasks.preferences.TasksPreferences

suspend fun toggleCollapsed(
    subheader: NavigationDrawerSubheader,
    caldavDao: CaldavDao,
    tasksPreferences: TasksPreferences,
) {
    val collapsed = !subheader.isCollapsed
    when (subheader.subheaderType) {
        NavigationDrawerSubheader.SubheaderType.PREFERENCE ->
            tasksPreferences.set(booleanPreferencesKey(subheader.id), collapsed)
        NavigationDrawerSubheader.SubheaderType.CALDAV,
        NavigationDrawerSubheader.SubheaderType.TASKS ->
            caldavDao.setCollapsed(subheader.id, collapsed)
    }
}

fun Filter.resolveIcon(purchaseState: PurchaseState): String? =
    getIcon(purchaseState)

fun resolveColor(tint: Int, isDark: Boolean, purchaseState: PurchaseState): Int? {
    if (tint == 0) return null
    if (!ColorProvider.isFreeColor(tint) && !purchaseState.purchasedThemes()) return null
    return ColorProvider.getColor(tint, isDark, adjust = true)
}
