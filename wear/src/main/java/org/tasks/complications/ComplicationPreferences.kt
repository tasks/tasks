package org.tasks.complications

import android.content.Context

private const val PREFS_NAME = "complication_preferences"
private const val KEY_FILTER_PREFIX = "filter_"
private const val KEY_FILTER_TITLE_PREFIX = "filter_title_"
private const val KEY_SORT_MODE_PREFIX = "sort_mode_"
private const val KEY_SHOW_HIDDEN_PREFIX = "show_hidden_"

fun Context.getComplicationFilter(complicationId: Int): String? =
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString("$KEY_FILTER_PREFIX$complicationId", null)

fun Context.getComplicationFilterTitle(complicationId: Int): String? =
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString("$KEY_FILTER_TITLE_PREFIX$complicationId", null)

fun Context.setComplicationFilter(complicationId: Int, filterId: String, filterTitle: String) {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString("$KEY_FILTER_PREFIX$complicationId", filterId)
        .putString("$KEY_FILTER_TITLE_PREFIX$complicationId", filterTitle)
        .commit()
}

fun Context.getComplicationSortMode(complicationId: Int): Int =
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getInt("$KEY_SORT_MODE_PREFIX$complicationId", com.todoroo.astrid.core.SortHelper.SORT_DUE)

fun Context.setComplicationSortMode(complicationId: Int, sortMode: Int) {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putInt("$KEY_SORT_MODE_PREFIX$complicationId", sortMode)
        .commit()
}

fun Context.getComplicationShowHidden(complicationId: Int): Boolean =
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean("$KEY_SHOW_HIDDEN_PREFIX$complicationId", false)

fun Context.setComplicationShowHidden(complicationId: Int, showHidden: Boolean) {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean("$KEY_SHOW_HIDDEN_PREFIX$complicationId", showHidden)
        .commit()
}

fun Context.clearComplicationFilter(complicationId: Int) {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .remove("$KEY_FILTER_PREFIX$complicationId")
        .remove("$KEY_FILTER_TITLE_PREFIX$complicationId")
        .remove("$KEY_SORT_MODE_PREFIX$complicationId")
        .remove("$KEY_SHOW_HIDDEN_PREFIX$complicationId")
        .commit()
}
