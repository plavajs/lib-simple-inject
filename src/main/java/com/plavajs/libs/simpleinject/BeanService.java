package com.plavajs.libs.simpleinject;

import com.plavajs.libs.simpleinject.annotation.SimpleBeanIdentifier;
import com.plavajs.libs.simpleinject.annotation.SimpleInject;
import com.plavajs.libs.simpleinject.exception.CyclicDependencyException;
import com.plavajs.libs.simpleinject.exception.MissingBeanException;
import lombok.Getter;

import java.lang.reflect.*;
import java.util.*;

@Getter
abstract class BeanService<T extends Bean> {

    List<T> beans;

    BeanService() {
        beans = loadBeans();
    }

    abstract <B extends Bean> List<B> loadBeans();

    static Object createInstance(Bean bean, Set<Class<?>> cache) {
        Class<?> type = bean.getType();
        if (cache.contains(type)) {
            throw new CyclicDependencyException(String.format("Cyclic dependency: %s !", type.getName()));
        }
        cache.add(type);

        Object instance;
        if (bean instanceof SimpleBean simpleBean) {
            Method method = simpleBean.getMethod();
            Parameter[] parameters = method.getParameters();
            Object[] parameterInstances = validateCollectParameters(cache, parameters);

            try {
                instance = method.invoke(null, parameterInstances);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else {
            Constructor<?> constructor = ComponentBeanService.validateGetComponentBeanConstructor(type);
            Parameter[] parameters = constructor.getParameters();
            Object[] parameterInstances = validateCollectParameters(cache, parameters);

            try {
                instance = constructor.newInstance(parameterInstances);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        injectAnnotatedFields(instance, type);
        return instance;
    }

    static Object[] validateCollectParameters(Set<Class<?>> cache, Parameter[] parameters) {
        List<Object> parameterInstances = new ArrayList<>();
        Arrays.stream(parameters)
                .forEach(parameter -> {
                    SimpleBeanIdentifier identifierAnnotation = parameter.getAnnotation(SimpleBeanIdentifier.class);
                    String identifier = identifierAnnotation == null ? "" : identifierAnnotation.value();
                    Bean bean = ApplicationContext.validateFindBean(parameter.getType(), identifier);

                    Object parameterInstance = bean.getInstance();
                    if (parameterInstance == null) {
                        parameterInstance = createInstance(bean, cache);
                        bean.setInstance(parameterInstance);
                    }

                    parameterInstances.add(parameterInstance);
                });
        return parameterInstances.toArray();
    }

    static <T> void injectAnnotatedFields(T object, Class<?> clazz) {
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(SimpleInject.class)) {
                field.setAccessible(true);
                try {
                    Object fieldInstance = field.get(object);
                    if (fieldInstance == null) {
                        Class<?> parameterType = field.getType();
                        SimpleBeanIdentifier idAnnotation = field.getAnnotation(SimpleBeanIdentifier.class);
                        String identifier = idAnnotation == null ? "" : idAnnotation.value();
                        Object innerObject = ApplicationContext.getInstance(parameterType, identifier);

                        if (innerObject == null) {
                            String identifierMessage = identifier.isBlank() ? "a blank identifier" : String.format("identifier='%s'", identifier);
                            throw new MissingBeanException(String.format("No bean registered for class: %s and %s !",
                                    parameterType.getName(),
                                    identifierMessage));
                        }

                        field.set(object, parameterType.cast(innerObject));
                        injectAnnotatedFields(innerObject, parameterType);
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
