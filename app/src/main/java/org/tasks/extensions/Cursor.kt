package org.tasks.extensions

import android.database.Cursor

fun Cursor.getString(columnName: String): String? =
    getColumnIndex(columnName).takeIf { it >= 0 }?.let { getString(it) }
