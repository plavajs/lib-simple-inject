package com.plavajs.libs.simpleinject.model;

import lombok.Getter;

@Getter
public class Component extends AbstractBean {

    public Component(Class<?> type) {
        super(type);
    }

}
