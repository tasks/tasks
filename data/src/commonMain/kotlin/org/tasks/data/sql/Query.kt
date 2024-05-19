package org.tasks.data.sql

import org.tasks.data.db.Table
import org.tasks.data.sql.StringBuilderExtensions.from
import org.tasks.data.sql.StringBuilderExtensions.join
import org.tasks.data.sql.StringBuilderExtensions.select
import org.tasks.data.sql.StringBuilderExtensions.where
import java.util.*

class Query private constructor(vararg fields: Field?) {
    private val criterions = ArrayList<Criterion>()
    private val fields = ArrayList<Field>().apply { addAll(fields.filterNotNull()) }
    private val joins = ArrayList<Join>()
    private var table: Table? = null
    private var queryTemplate: String? = null

    fun from(fromTable: Table?): Query {
        table = fromTable
        return this
    }

    fun join(vararg join: Join): Query {
        joins.addAll(join)
        return this
    }

    fun where(criterion: Criterion): Query {
        criterions.add(criterion)
        return this
    }

    override fun equals(other: Any?): Boolean {
        return this === other
                || !(other == null || javaClass != other.javaClass) && this.toString() == other.toString()
    }

    override fun hashCode() = toString().hashCode()

    override fun toString(): String {
        val sql = StringBuilder()
                .select(fields)
                .from(table)
                .join(joins)
        if (queryTemplate == null) {
            sql.where(criterions)
        } else {
            sql.append(queryTemplate)
        }
        return sql.toString()
    }

    fun withQueryTemplate(template: String?): Query {
        queryTemplate = template
        return this
    }

    companion object {
        @JvmStatic
        fun select(vararg fields: Field?): Query {
            return Query(*fields)
        }
    }
}