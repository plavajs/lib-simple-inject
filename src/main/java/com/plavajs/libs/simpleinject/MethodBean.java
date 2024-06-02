package com.plavajs.libs.simpleinject;

import com.plavajs.libs.simpleinject.annotation.SimpleBean;
import lombok.Getter;

import java.lang.reflect.Method;

@Getter
final class MethodBean extends Bean {

    private final Method method;

    MethodBean(Method method) {
        super(method.getReturnType());
        this.method = method;
        setIdentifier(method.getAnnotation(SimpleBean.class).identifier());
    }
}
