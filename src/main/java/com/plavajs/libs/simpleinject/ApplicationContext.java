package com.plavajs.libs.simpleinject;

import com.plavajs.libs.simpleinject.annotation.*;
import com.plavajs.libs.simpleinject.exception.CyclicDependenciesException;
import com.plavajs.libs.simpleinject.exception.DuplicitBeanException;
import com.plavajs.libs.simpleinject.exception.MissingBeanException;
import com.plavajs.libs.simpleinject.exception.MissingPublicConstructorException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApplicationContext {

    @Getter
    private static final Map<Class<?>, Object> dependencies = new HashMap<>();

    @Getter
    private static final List<Bean> beans = new ArrayList<>();

    public static void initializeContext() {
        ClassScanner.loadAllClasses();
        registerBeans();
        registerComponents();
    }

    public static  <T> T getInstance(Class<T> clazz) {
        Object object = dependencies.get(clazz);
        if (object != null) {
            T instance = clazz.cast(object);
            injectAnnotatedFields(instance, clazz);
            return instance;
        }

        throw new MissingBeanException(String.format("No bean found for class %s", clazz.getName()));
    }


    private static void registerBeans() {
        Set<Class<?>> configurationClasses = ClassScanner.findClassesAnnotatedWith(SimpleConfiguration.class);

        configurationClasses.stream()
                .flatMap(clazz -> Arrays.stream(clazz.getDeclaredMethods()))
                .map(ApplicationContext::createNewBean)
                .forEach(beans::add);
    }

    private static Bean createNewBean(Method method) {
        Class<?> returnType = method.getReturnType();
        String annotationValue = method.getAnnotation(SimpleBean.class).value();
        String beanId = annotationValue.isBlank() ? method.getName() : annotationValue;
        Object[] arguments = new Object[0];
        Bean bean;
        try {
            bean = new Bean(returnType, method.invoke(arguments), beanId);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        validateNewBean(bean);
        return bean;
    }

    private static void validateNewBean(Bean bean) {
        beans.forEach(existingBean -> {
            if (existingBean.getType().equals(bean.getType()) && existingBean.getBeanId().equals(bean.getBeanId())) {
                throw new DuplicitBeanException(
                        String.format("There is already a bean for type %s with ID %s registered",
                                bean.getType().getName(), bean.getBeanId()));
            }
        });
    }

    private static String getBeanIdFromMethodName(Class<?> clazz) {
        char[] beanIdChars = clazz.getSimpleName().toCharArray();
        beanIdChars[0] = Character.toLowerCase(beanIdChars[0]);
        return new String(beanIdChars);
    }

    private static void registerComponents() {
        Set<Class<?>> annotatedClasses = ClassScanner.findClassesAnnotatedWith(SimpleComponentScan.class);

        resolveDistinctComponentScans(annotatedClasses)
                .forEach(clazz -> {
                    if (clazz.isAnnotationPresent(SimpleComponent.class)) {
                        dependencies.put(clazz, createComponentInstance(clazz, new HashSet<>()));
                    }
                });
    }

    private static Set<Class<?>> resolveDistinctComponentScans(Set<Class<?>> scanAnnotated) {
        Set<SimpleComponentScan> recursive = new HashSet<>(
                scanAnnotated.stream()
                        .map(clazz -> clazz.getAnnotation(SimpleComponentScan.class))
                        .filter(SimpleComponentScan::recursively)
                        .collect(Collectors.toMap(SimpleComponentScan::value, Function.identity(), (a, b) -> a))
                        .values()
        );

        Set<SimpleComponentScan> simple = scanAnnotated.stream()
                .map(clazz -> clazz.getAnnotation(SimpleComponentScan.class))
                .filter(componentScan -> !componentScan.recursively())
                .collect(Collectors.toSet());

        Set<SimpleComponentScan> distinctComponentScans = simple.stream()
                .filter(simpleComponentScan -> recursive.stream()
                        .noneMatch(recursiveComponentScan -> recursiveComponentScan.value().startsWith(simpleComponentScan.value()))
                )
                .collect(Collectors.toSet());

        distinctComponentScans.addAll(recursive);

        return distinctComponentScans.stream()
                .flatMap(componentScan -> ClassScanner.findClassesInPackage(componentScan.value(), componentScan.recursively()).stream())
                .collect(Collectors.toSet());
    }

    private static Object createComponentInstance(Class<?> clazz, Set<Class<?>> classes) {
        classes.add(clazz);
        Constructor<?>[] constructors = clazz.getConstructors();
        if (constructors.length == 0) {
            throw new MissingPublicConstructorException(String.format("No public constructor found for class %s", clazz.getName()));
        }

        Constructor<?> constructor = constructors[0];
        Class<?>[] parameterTypes = constructor.getParameterTypes();

        List<Object> parameters = new LinkedList<>();

        for (Class<?> parameterType : parameterTypes) {
            if (!parameterType.isAnnotationPresent(SimpleComponent.class)) {
                throw new MissingBeanException(String.format("No bean found for class %s", parameterType.getName()));
            }

            Object parameter = dependencies.get(parameterType);
            if (parameter == null) {
                if (classes.contains(parameterType)) {
                    String message = String.format("Cyclic dependency: %s", parameterType.getName());
                    throw new CyclicDependenciesException(message);
                }

                parameter = createComponentInstance(parameterType, classes);
                dependencies.put(parameterType, parameter);
            }
            parameters.add(parameter);
        }

        try {
            return constructor.newInstance(parameters.toArray());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static  <T> void injectAnnotatedFields(T object, Class<?> clazz) {
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(SimpleInject.class)) {
                field.setAccessible(true);
                Class<?> parameterType = field.getType();
                if (!parameterType.isAnnotationPresent(SimpleComponent.class)) {
                    throw new MissingBeanException(String.format("No bean found for class %s", parameterType.getName()));
                }
                Object innerObject = parameterType.cast(dependencies.get(parameterType));

                try {
                    if (innerObject == null) {
                        innerObject = createComponentInstance(parameterType, new HashSet<>());
                    }
                    field.set(object, innerObject);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                injectAnnotatedFields(innerObject, parameterType);
            }
        }
    }
}
