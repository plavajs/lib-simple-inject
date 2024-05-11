package com.plavajs.libs.simpleinject;

import com.plavajs.libs.simpleinject.annotation.SimpleInject;
import com.plavajs.libs.simpleinject.exception.IncompatibleBeanTypeException;
import com.plavajs.libs.simpleinject.exception.MissingBeanException;
import lombok.Getter;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;

@Getter
abstract class BeanService<T extends Bean> {

    List<T> beans;

    BeanService() {
        beans = loadBeans();
    }

    abstract Object createInstance(Bean bean, Set<Class<?>> cache);

    abstract <B extends Bean> List<B> loadBeans();

    abstract <B extends Bean> void setupInstance(B bean);

    Object[] validateCollectParameters(Set<Class<?>> cache, Class<?>[] parameterTypes) {
        List<Object> parameters = new ArrayList<>();
        Arrays.stream(parameterTypes)
                .forEach(parameterType -> {
                    Bean parameterBean = ApplicationContext.validateFindBean(parameterType);
                    Object parameter = parameterBean.getInstance();
                    if (parameter == null) {
                        parameter = createInstance(parameterBean, cache);
                        parameterBean.setInstance(parameter);
                    }

                    parameters.add(parameter);
                });
        return parameters.toArray();
    }

    void validateBeanType(Bean bean) {
        Class<?> type = (Class<?>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        if (!(type.isInstance(bean))) {
            throw new IncompatibleBeanTypeException(
                    String.format("Cannot create instance for bean of type %s from bean of type %s",
                            type.getName(),
                            bean.getClass().getName()));
        }
    }

    static <T> void injectAnnotatedFields(T object, Class<?> clazz) {
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(SimpleInject.class)) {
                field.setAccessible(true);
                Class<?> parameterType = field.getType();
                Object innerObject = ApplicationContext.getInstance(parameterType);
                try {
                    if (innerObject == null) {
                        throw new MissingBeanException(String.format("No bean registered for class %s", parameterType.getName()));
                    }
                    field.set(object, parameterType.cast(innerObject));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                injectAnnotatedFields(innerObject, parameterType);
            }
        }
    }
}
