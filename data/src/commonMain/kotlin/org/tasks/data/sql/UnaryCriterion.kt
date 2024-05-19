package org.tasks.data.sql

open class UnaryCriterion private constructor(private val expression: Field, operator: Operator, private val value: Any?) : Criterion(operator) {
    override fun populate() = "$expression${populateOperator()}${afterPopulateOperator()}"

    open fun populateOperator() = "$operator"

    private fun afterPopulateOperator(): Any = if (value is String) {
        "'${sanitize(value)}'"
    } else {
        value ?: ""
    }

    companion object {
        fun eq(expression: Field, value: Any?): Criterion = UnaryCriterion(expression, Operator.eq, value)

        /** Sanitize the given input for SQL  */
        fun sanitize(input: String): String = input.replace("'", "''")

        fun gt(field: Field, value: Any?): Criterion = UnaryCriterion(field, Operator.gt, value)

        fun lt(field: Field, value: Any?): Criterion = UnaryCriterion(field, Operator.lt, value)

        fun lte(field: Field, value: Any?): Criterion = UnaryCriterion(field, Operator.lte, value)

        fun isNotNull(field: Field): Criterion {
            return object : UnaryCriterion(field, Operator.isNotNull, null) {
                override fun populateOperator() = " $operator"
            }
        }

        fun isNull(field: Field): Criterion {
            return object : UnaryCriterion(field, Operator.isNull, null) {
                override fun populateOperator() = " $operator"
            }
        }

        fun like(field: Field, value: String?): Criterion {
            return object : UnaryCriterion(field, Operator.like, value) {
                override fun populateOperator() = " $operator "
            }
        }
    }
}