package com.plavajs.libs.simpleinject;

import com.plavajs.libs.simpleinject.annotation.SimpleBeanIdentifier;
import com.plavajs.libs.simpleinject.annotation.SimpleInject;
import com.plavajs.libs.simpleinject.exception.CyclicDependencyException;
import com.plavajs.libs.simpleinject.exception.MissingBeanException;
import com.plavajs.libs.simpleinject.exception.UnsupportedElementTypeException;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.lang.reflect.*;
import java.util.*;

@Log4j2
@Getter
abstract class BeanService<T extends Bean> {

    Set<T> beans = new HashSet<>();

    BeanService() {
        loadBeans();
    }

    abstract void loadBeans();

    static Object createInstance(Bean bean, Set<Class<?>> cache) {
        Class<?> type = bean.getType();
        validateCacheDependency(type, cache);
        Object instance;
        if (bean instanceof MethodBean methodBean) {
            Method method = methodBean.getMethod();
            Parameter[] parameters = method.getParameters();
            Object[] parameterInstances = validateCollectParametersInstances(parameters, new HashSet<>(cache));

            try {
                instance = method.invoke(null, parameterInstances);
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.error(e.getMessage());
                throw new RuntimeException(e);
            }
        } else {
            Constructor<?> constructor = ComponentBeanService.validateGetComponentBeanConstructor(type);
            Parameter[] parameters = constructor.getParameters();
            Object[] parameterInstances = validateCollectParametersInstances(parameters, new HashSet<>(cache));

            try {
                instance = constructor.newInstance(parameterInstances);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                log.error(e.getMessage());
                throw new RuntimeException(e);
            }
        }

        injectAnnotatedFields(instance, type, new HashSet<>(cache));
        return instance;
    }

    private static Object[] validateCollectParametersInstances(Parameter[] parameters, Set<Class<?>> cache) {
        List<Object> parameterInstances = new ArrayList<>(Arrays.stream(parameters)
                .map(parameter -> getElementInstance(parameter, cache))
                .toList());

        return parameterInstances.toArray();
    }

    private static <O> void injectAnnotatedFields(O object, Class<?> type, Set<Class<?>> cache) {
        Field[] declaredFields = type.getDeclaredFields();
        Arrays.stream(declaredFields)
                .filter(field -> field.isAnnotationPresent(SimpleInject.class))
                .forEach(field -> injectAnnotatedField(object, field, cache));
    }

    private static <O> void injectAnnotatedField(O object, Field field, Set<Class<?>> cache) {
        field.setAccessible(true);
        try {
            Object fieldInstance = field.get(object);
            if (fieldInstance == null) {
                Class<?> parameterType = field.getType();
                Object innerObject = getElementInstance(field, cache);
                field.set(object, parameterType.cast(innerObject));
            }
        } catch (IllegalAccessException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static Object getElementInstance(AnnotatedElement element, Set<Class<?>> cache) {
        Class<?> type = validateGetAnnotatedElementType(element);
        String identifier = getElementIdentifier(element);
        Bean bean = ApplicationContext.validateFindBean(type, identifier);
        Object instance = bean.getInstance();
        if (instance == null) {
            instance = createInstance(bean, cache);
            bean.setInstance(instance);

            if (instance == null) {
                String identifierMessage = identifier.isBlank() ? "a blank identifier" : String.format("identifier='%s'", identifier);
                String message = String.format("No bean registered for type: %s and %s !", type.getName(), identifierMessage);
                log.error(message);
                throw new MissingBeanException(message);
            }
        }
        return instance;
    }

    private static String getElementIdentifier(AnnotatedElement element) {
        if (element instanceof Parameter) {
            SimpleBeanIdentifier simpleBeanIdentifier = element.getAnnotation(SimpleBeanIdentifier.class);
            if (simpleBeanIdentifier != null) return simpleBeanIdentifier.value();
            return "";
        }

        if (element instanceof Field) {
            SimpleInject simpleInject = element.getAnnotation(SimpleInject.class);
            if (simpleInject != null) return simpleInject.identifier();
        }
        return "";
    }

    private static Class<?> validateGetAnnotatedElementType(AnnotatedElement element) {
        if (element instanceof Parameter parameter) {
            return parameter.getType();
        }

        if (element instanceof Field field) {
            return field.getType();
        }

        String message = String.format(
                "%s not supported. Must be %s or %s", element.getClass().getName(), Parameter.class.getName(), Field.class.getName());

        log.error(message);
        throw new UnsupportedElementTypeException(message);
    }

    private static void validateCacheDependency(Class<?> type, Set<Class<?>> cache) {
        if (cache.contains(type)) {
            String message = String.format("Cyclic dependency: %s !", type.getName());
            log.error(message);
            throw new CyclicDependencyException(message);
        }
        cache.add(type);
    }
}
