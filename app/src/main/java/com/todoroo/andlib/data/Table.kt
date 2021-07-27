package com.todoroo.andlib.data

import com.todoroo.andlib.sql.DBObject

class Table private constructor(private val name: String, alias: String?) : DBObject(name) {

    constructor(name: String) : this(name, null)

    fun column(column: String): Property = Property(this, column)

    override fun `as`(newAlias: String) = Table(name, newAlias)

    override fun toString(): String = alias?.let { "$expression AS $alias" } ?: expression

    fun name() = alias ?: name

    init {
        this.alias = alias
    }
}