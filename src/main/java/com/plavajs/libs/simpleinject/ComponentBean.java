package com.plavajs.libs.simpleinject;

import lombok.AccessLevel;
import lombok.Getter;

@Getter(AccessLevel.PACKAGE)
public final class ComponentBean extends Bean { //TODO make only package accessible

    ComponentBean(Class<?> type) {
        super(type);
    }

}
