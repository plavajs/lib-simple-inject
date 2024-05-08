package com.plavajs.libs.example;


import com.plavajs.libs.simpleinject.annotation.SimpleInject;
public class OrderService {

    @SimpleInject
    private OrderRepository repository;

    public Order getOrderDetails(Integer orderId) {
        return repository.getById(orderId);
    }
}
