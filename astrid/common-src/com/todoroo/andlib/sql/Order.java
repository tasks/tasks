package com.todoroo.andlib.sql;

import static com.todoroo.andlib.sql.Constants.SPACE;

public class Order {
    private final Object expression;
    private final OrderType orderType;

    private Order(Object expression) {
        this(expression, OrderType.ASC);
    }

    private Order(Object expression, OrderType orderType) {
        this.expression = expression;
        this.orderType = orderType;
    }

    public static Order asc(Object expression) {
        return new Order(expression);
    }

    public static Order desc(Object expression) {
        return new Order(expression, OrderType.DESC);
    }

    @Override
    public String toString() {
        return expression + SPACE + orderType;
    }
}
