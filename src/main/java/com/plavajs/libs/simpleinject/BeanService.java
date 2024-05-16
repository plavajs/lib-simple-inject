package com.plavajs.libs.simpleinject;

import com.plavajs.libs.simpleinject.annotation.SimpleBeanIdentifier;
import com.plavajs.libs.simpleinject.annotation.SimpleInject;
import com.plavajs.libs.simpleinject.exception.CyclicDependencyException;
import com.plavajs.libs.simpleinject.exception.MissingBeanException;
import com.plavajs.libs.simpleinject.exception.UnsupportedElementTypeException;
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
        validateCacheDependency(type, cache);
        Object instance;
        if (bean instanceof SimpleBean simpleBean) {
            Method method = simpleBean.getMethod();
            Parameter[] parameters = method.getParameters();
            Object[] parameterInstances = validateCollectParametersInstances(parameters, cache);

            try {
                instance = method.invoke(null, parameterInstances);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else {
            Constructor<?> constructor = ComponentBeanService.validateGetComponentBeanConstructor(type);
            Parameter[] parameters = constructor.getParameters();
            Object[] parameterInstances = validateCollectParametersInstances(parameters, cache);

            try {
                instance = constructor.newInstance(parameterInstances);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        injectAnnotatedFields(instance, type, new HashSet<>());
        return instance;
    }

    static Object[] validateCollectParametersInstances(Parameter[] parameters, Set<Class<?>> cache) {
        List<Object> parameterInstances = new ArrayList<>(Arrays.stream(parameters)
                .map(parameter -> getAnnotatedElementInstance(parameter, cache))
                .toList());

        return parameterInstances.toArray();
    }

    static <T> void injectAnnotatedFields(T object, Class<?> type, Set<Class<?>> cache) {
        validateCacheDependency(type, cache);
        Field[] declaredFields = type.getDeclaredFields();
        Arrays.stream(declaredFields)
                .filter(field -> field.isAnnotationPresent(SimpleInject.class))
                .forEach(field -> injectAnnotatedField(object, field, cache));
    }

    private static <T> void injectAnnotatedField(T object, Field field, Set<Class<?>> cache) {
        field.setAccessible(true);
        try {
            Object fieldInstance = field.get(object);
            if (fieldInstance == null) {
                Class<?> parameterType = field.getType();
                Object innerObject = getAnnotatedElementInstance(field, cache);
                field.set(object, parameterType.cast(innerObject));
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object getAnnotatedElementInstance(AnnotatedElement element, Set<Class<?>> cache) {
        Class<?> type = validateGetAnnotatedElementType(element);

        SimpleBeanIdentifier identifierAnnotation = element.getAnnotation(SimpleBeanIdentifier.class);
        String identifier = identifierAnnotation == null ? "" : identifierAnnotation.value();
        Bean bean = ApplicationContext.validateFindBean(type, identifier);
        Object instance = bean.getInstance();
        if (instance == null) {
            instance = createInstance(bean, cache);
            bean.setInstance(instance);

            if (instance == null) {
                String identifierMessage = identifier.isBlank() ? "a blank identifier" : String.format("identifier='%s'", identifier);
                throw new MissingBeanException(String.format("No bean registered for class: %s and %s !",
                        type.getName(),
                        identifierMessage));
            }
        }

        return instance;
    }

    private static Class<?> validateGetAnnotatedElementType(AnnotatedElement element) {
        Class<?> type;
        if (element instanceof Parameter parameter) {
            type = parameter.getType();
        } else if (element instanceof Field field) {
            type = field.getType();
        } else {
            throw new UnsupportedElementTypeException(String.format("%s not supported. Must be %s or %s", element.getClass().getName(),
                    Parameter.class.getName(),
                    Field.class.getName()));
        }
        return type;
    }

    private static void validateCacheDependency(Class<?> type, Set<Class<?>> cache) {
        if (cache.contains(type)) {
            throw new CyclicDependencyException(String.format("Cyclic dependency: %s !", type.getName()));
        }
        cache.add(type);
    }
}
