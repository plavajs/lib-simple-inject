package com.plavajs.libs.simpleinject;

import lombok.Getter;

import java.lang.reflect.Method;

@Getter
final class SimpleBean extends Bean {

    private final Method method;

    SimpleBean(Method method) {
        super(method.getReturnType());
        this.method = method;
        setIdentifier(method.getAnnotation(com.plavajs.libs.simpleinject.annotation.SimpleBean.class).identifier());
    }
}
