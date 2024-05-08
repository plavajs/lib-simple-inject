package com.plavajs.libs.simpleinject;

import lombok.AccessLevel;
import lombok.Getter;

import java.lang.reflect.Method;

@Getter(AccessLevel.PACKAGE)
public final class SimpleBean extends Bean { //TODO make only package accessible

    private final Method method;

    SimpleBean(Method method, Class<?> type) {
        super(type);
        this.method = method;
    }
}
