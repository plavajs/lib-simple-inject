package com.plavajs.libs.simpleinject.model;

import lombok.Getter;

import java.lang.reflect.Method;

@Getter
public class Bean extends AbstractBean {

    private final Method method;

    public Bean(Method method, Class<?> type) {
        super(type);
        this.method = method;
    }
}
