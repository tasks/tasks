package org.tasks.data.sql

import org.tasks.data.db.Table

class Join private constructor(private val joinTable: Table, private val joinType: JoinType, criterions: List<Criterion>) {    private val criterions = criterions.toList()

    override fun toString() = "$joinType JOIN $joinTable ON (${criterions.joinToString(" AND ")})"

    companion object {
        @JvmStatic
        fun inner(expression: Table, vararg criterions: Criterion?): Join {
            return Join(expression, JoinType.INNER, criterions.filterNotNull())
        }

        @JvmStatic
        fun left(table: Table, vararg criterions: Criterion?): Join {
            return Join(table, JoinType.LEFT, criterions.filterNotNull())
        }
    }
}