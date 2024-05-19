package org.tasks.data.sql

import org.tasks.data.db.Table

object StringBuilderExtensions {
    fun StringBuilder.join(joins: List<Join>): StringBuilder {
        if (joins.isNotEmpty()) {
            append("${joins.joinToString(" ")} ")
        }
        return this
    }

    fun StringBuilder.where(criterion: List<Criterion>): StringBuilder {
        if (criterion.isNotEmpty()) {
            append("WHERE ${criterion.joinToString(" ")} ")
        }
        return this
    }

    fun StringBuilder.orderBy(orders: List<Order>): StringBuilder {
        if (orders.isNotEmpty()) {
            append("ORDER BY ${orders.joinToString(",")} ")
        }
        return this
    }

    fun StringBuilder.from(table: Table?): StringBuilder {
        if (table != null) {
            append("FROM $table ")
        }
        return this
    }

    fun StringBuilder.select(fields: List<Field>): StringBuilder {
        append("SELECT ${fields.joinToString(", ", transform = Field::toStringInSelect).ifEmpty { "*" }} ")
        return this
    }
}