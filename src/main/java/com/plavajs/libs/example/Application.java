package com.plavajs.libs.example;

import com.plavajs.libs.simpleinject.ApplicationContext;
import com.plavajs.libs.simpleinject.annotation.SimpleComponentScan;

import java.util.Map;

@SimpleComponentScan(value = "com.plavajs.libs", recursively = true)
public class Application {

    public static void main(String[] args) {
        ApplicationContext.initializeContext();
        Map<Class<?>, Object> dependencies = ApplicationContext.getDependencies();
//
//        OrderService orderService = ApplicationContext.getInstance(OrderService.class);
//        System.out.println();
        FirstClass firstClass = ApplicationContext.getInstance(FirstClass.class);
        ThirdClass thirdClass = ApplicationContext.getInstance(ThirdClass.class);
        System.out.println();
    }
}
