package com.todoroo.andlib.sql

open class Field(expression: String) : DBObject(expression) {
    fun eq(value: Any?): Criterion = if (value == null) {
        UnaryCriterion.isNull(this)
    } else {
        UnaryCriterion.eq(this, value)
    }

    override fun `as`(newAlias: String): Field {
        val clone = Field(expression)
        clone.alias = newAlias
        return clone
    }

    fun gt(value: Any?): Criterion = UnaryCriterion.gt(this, value)

    fun lt(value: Any?): Criterion = UnaryCriterion.lt(this, value)

    fun lte(value: Any?): Criterion = UnaryCriterion.lte(this, value)

    fun like(value: String?): Criterion = UnaryCriterion.like(this, value)

    fun `in`(query: Query?): Criterion {
        val field = this
        return object : Criterion(Operator.`in`) {
            override fun populate() = "$field IN ($query)"
        }
    }

    fun `in`(entries: List<*>): Criterion {
        val field = this
        return object : Criterion(Operator.`in`) {
            override fun populate() = "$field IN (${entries.joinToString(",")})"
        }
    }

    fun toStringInSelect(): String {
        val sb = StringBuilder(expression)
        if (hasAlias()) {
            sb.append(" AS $alias")
        } else {
            val pos = expression.indexOf('.')
            if (pos > 0 && !expression.endsWith("*")) {
                sb.append(" AS ${expression.substring(pos + 1)}")
            }
        }
        return sb.toString()
    }

    companion object {
        @JvmStatic fun field(expression: String): Field = Field(expression)
        @JvmStatic val COUNT = Field("COUNT(*)")
    }
}