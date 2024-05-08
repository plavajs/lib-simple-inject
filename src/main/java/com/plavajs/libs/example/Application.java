package com.plavajs.libs.example;

import com.plavajs.libs.simpleinject.ApplicationContext;
import com.plavajs.libs.simpleinject.annotation.SimpleComponentScan;
import com.plavajs.libs.simpleinject.model.Bean;
import com.plavajs.libs.simpleinject.model.Component;

import java.util.List;

@SimpleComponentScan(value = "com.plavajs.libs", recursively = true)
public class Application {

    public static void main(String[] args) {
        ApplicationContext.initializeContext();
        List<Component> dependencies = ApplicationContext.getComponents();
        List<Bean> beans = ApplicationContext.getBeans();
//
//        OrderService orderService = ApplicationContext.getInstance(OrderService.class);
//        System.out.println();
        FirstClass firstClass = ApplicationContext.getInstance(FirstClass.class);
        ThirdClass thirdClass = ApplicationContext.getInstance(ThirdClass.class);
        System.out.println();
    }
}
