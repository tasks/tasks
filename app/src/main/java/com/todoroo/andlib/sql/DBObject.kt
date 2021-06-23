package com.todoroo.andlib.sql

import java.util.*

abstract class DBObject<T : DBObject<T>> internal constructor(val expression: String) : Cloneable {
    var alias: String? = null

    open fun `as`(newAlias: String): T {
        return try {
            val clone = clone() as T
            clone.alias = newAlias
            clone
        } catch (e: CloneNotSupportedException) {
            throw RuntimeException(e)
        }
    }

    protected fun hasAlias() = alias != null

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is DBObject<*>) {
            return false
        }
        return expression == other.expression && alias == other.alias
    }

    override fun hashCode() = Objects.hash(expression, alias)

    override fun toString() = alias ?: expression
}