package com.thoughtworks.sql;

import static com.thoughtworks.sql.Constants.SPACE;

public class Order {
    private final Field expression;
    private final OrderType orderType;

    private Order(Field expression) {
        this(expression, OrderType.ASC);
    }

    private Order(Field expression, OrderType orderType) {
        this.expression = expression;
        this.orderType = orderType;
    }

    public static Order asc(Field expression) {
        return new Order(expression);
    }

    public static Order desc(Field expression) {
        return new Order(expression, OrderType.DESC);
    }

    @Override
    public String toString() {
        return expression + SPACE + orderType;
    }
}
