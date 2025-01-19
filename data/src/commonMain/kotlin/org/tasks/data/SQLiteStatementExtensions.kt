package org.tasks.data

import androidx.sqlite.SQLiteStatement

fun SQLiteStatement.getTextOrNull(index: Int): String? =
    if (index == -1 || isNull(index)) null else this.getText(index)
