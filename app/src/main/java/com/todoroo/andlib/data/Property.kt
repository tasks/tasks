package com.todoroo.andlib.data

import com.todoroo.andlib.sql.Field

class Property internal constructor(val name: String, expression: String) : Field(expression) {

    constructor(table: Table, columnName: String) : this(columnName, "${table.name()}.$columnName")
}