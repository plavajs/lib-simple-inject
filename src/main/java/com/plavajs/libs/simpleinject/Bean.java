package com.plavajs.libs.simpleinject;

import lombok.Getter;
import lombok.Setter;

@Getter
abstract class Bean {

    private final Class<?> type;

    @Setter
    private Object instance;

    @Setter
    private String identifier;

    public Bean(Class<?> type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Bean bean)) {
            return false;
        }

        return type.equals(bean.type) && identifier.equals(bean.identifier);
    }
}
