package org.tasks.api

import android.content.ContentValues
import android.database.MatrixCursor

internal fun ApiRows.toCursor(projection: Array<out String>?): MatrixCursor {
    val names = projection?.filter { it in columns } ?: columns
    val indices = names.map { columns.indexOf(it) }
    val cursor = MatrixCursor(names.toTypedArray(), rows.size)
    rows.forEach { row -> cursor.addRow(indices.map { row[it] }) }
    return cursor
}

internal fun ContentValues.toApiValues() = ApiValues(keySet().associateWith { get(it) })
