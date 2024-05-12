package org.tasks.data.sql

import org.tasks.data.sql.OrderType.ASC
import org.tasks.data.sql.OrderType.DESC

class Order private constructor(private val expression: Any, val orderType: OrderType = ASC) {
    private val secondaryExpressions = ArrayList<Order>()

    fun addSecondaryExpression(secondary: Order): Order {
        secondaryExpressions.add(secondary)
        return this
    }

    override fun toString() =
            "$expression $orderType${secondaryExpressions.takeIf { it.isNotEmpty() }?.joinToString(", ", ", ") ?: ""}"

    fun reverse() = Order(expression, if (orderType === ASC) DESC else ASC)

    companion object {
        @JvmStatic
        fun asc(expression: Any) = Order(expression)

        @JvmStatic
        fun desc(expression: Any) = Order(expression, DESC)
    }
}