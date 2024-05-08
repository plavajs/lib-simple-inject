package com.plavajs.libs.simpleinject;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Bean {
    private final Class<?> type;
    private final Object instance;
    private final String beanId;
}
