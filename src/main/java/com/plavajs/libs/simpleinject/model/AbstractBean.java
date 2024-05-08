package com.plavajs.libs.simpleinject.model;

import lombok.Getter;
import lombok.Setter;

@Getter
public class AbstractBean {

    private final Class<?> type;
    @Setter
    private Object instance;

    public AbstractBean(Class<?> type) {
        this.type = type;
    }
}
