package org.tasks.data.sql


abstract class DBObject internal constructor(val expression: String) {
    var alias: String? = null

    abstract fun `as`(newAlias: String): DBObject

    protected fun hasAlias() = alias != null

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is DBObject) {
            return false
        }
        return expression == other.expression && alias == other.alias
    }

    override fun hashCode(): Int {
        var result = expression.hashCode()
        result = 31 * result + (alias?.hashCode() ?: 0)
        return result
    }

    override fun toString() = alias ?: expression
}