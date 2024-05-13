package com.plavajs.libs.simpleinject;

import com.plavajs.libs.simpleinject.annotation.SimpleBean;
import com.plavajs.libs.simpleinject.annotation.SimpleComponent;
import lombok.Getter;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;

@Getter
final class ComponentBean extends Bean {

    ComponentBean(Class<?> type) {
        super(type);
        setIdentifier(createIdentifier());
    }

    private String createIdentifier() {
        Constructor<?>[] allConstructors = getType().getDeclaredConstructors();
        List<Constructor<?>> annotatedConstructors = Arrays.stream(allConstructors)
                .filter(allConstructor -> allConstructor.isAnnotationPresent(SimpleBean.class))
                .toList();

        String identifier = "";
        if (!annotatedConstructors.isEmpty()) {
            identifier = annotatedConstructors.get(0).getAnnotation(SimpleBean.class).identifier();
        }

        return identifier.isBlank() ? getType().getAnnotation(SimpleComponent.class).identifier() : identifier;
    }

}
