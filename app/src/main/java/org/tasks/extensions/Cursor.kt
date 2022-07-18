package org.tasks.extensions

import android.database.Cursor
import androidx.core.database.getLongOrNull

fun Cursor.getString(columnName: String): String? =
    getColumnIndex(columnName).takeIf { it >= 0 }?.let { getString(it) }

fun Cursor.getLongOrNull(columnName: String): Long? =
    getColumnIndex(columnName).takeIf { it >= 0 }?.let { getLongOrNull(it) }