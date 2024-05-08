package com.plavajs.libs.example;

import java.util.HashMap;
import java.util.Map;

public class OrderRepository {
    Map<Integer, Order> orderIdToOrderMap = new HashMap<>();

    public OrderRepository() {
        orderIdToOrderMap.put(1, new Order(1, "Special Order", 10.0));
        orderIdToOrderMap.put(2, new Order(2, "Extra special Order", 20.0));
        orderIdToOrderMap.put(3, new Order(3, "Important Order", 30.0));
        orderIdToOrderMap.put(4, new Order(4, "Unusual Order", 40.0));
    }

    public Order getById(Integer orderId) {
        return orderIdToOrderMap.get(orderId);
    }
}
