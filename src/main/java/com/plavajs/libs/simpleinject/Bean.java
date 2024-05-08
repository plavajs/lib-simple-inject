package com.plavajs.libs.simpleinject;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter(AccessLevel.PACKAGE)
class Bean {

    private final Class<?> type;
    @Setter(AccessLevel.PACKAGE)
    private Object instance;

    public Bean(Class<?> type) {
        this.type = type;
    }
}
