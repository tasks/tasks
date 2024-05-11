package com.todoroo.andlib.sql

import com.todoroo.andlib.sql.StringBuilderExtensions.join
import com.todoroo.andlib.sql.StringBuilderExtensions.orderBy
import com.todoroo.andlib.sql.StringBuilderExtensions.where
import java.util.*

class QueryTemplate {
    private val criterions = ArrayList<Criterion>()
    private val joins = ArrayList<Join>()
    private val orders = ArrayList<Order>()

    fun join(vararg join: Join): QueryTemplate {
        joins.addAll(join)
        return this
    }

    fun where(criterion: Criterion): QueryTemplate {
        criterions.add(criterion)
        return this
    }

    fun orderBy(vararg order: Order): QueryTemplate {
        orders.addAll(order)
        return this
    }

    override fun toString() =
            StringBuilder()
                .join(joins)
                .where(criterions)
                .orderBy(orders)
                .toString()
}