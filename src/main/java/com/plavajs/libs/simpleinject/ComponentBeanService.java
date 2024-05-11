package com.plavajs.libs.simpleinject;

import com.plavajs.libs.simpleinject.annotation.SimpleBean;
import com.plavajs.libs.simpleinject.annotation.SimpleComponent;
import com.plavajs.libs.simpleinject.annotation.SimpleComponentScan;
import com.plavajs.libs.simpleinject.exception.CyclicDependencyException;
import com.plavajs.libs.simpleinject.exception.MissingPublicConstructorException;
import com.plavajs.libs.simpleinject.exception.MultipleBeanConstructorsException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

final class ComponentBeanService extends BeanService<ComponentBean> {

    @Override
    @SuppressWarnings("unchecked")
    List<ComponentBean> loadBeans() {
        Set<Class<?>> componentScanAnnotated = ClassScanner.findClassesAnnotatedWith(SimpleComponentScan.class);

        return resolveDistinctComponentScans(componentScanAnnotated).stream()
                .filter(clazz -> clazz.isAnnotationPresent(SimpleComponent.class))
                .map(ComponentBean::new)
                .collect(Collectors.toList());
    }

    @Override
    <B extends Bean> void setupInstance(B bean) {
        validateBeanType(bean);
        bean.setInstance(createInstance(bean, new HashSet<>()));
    }

    @Override
    Object createInstance(Bean bean, Set<Class<?>> cache) {
        validateBeanType(bean);

        Class<?> beanType = bean.getType();
        if (cache.contains(beanType)) {
            throw new CyclicDependencyException(String.format("Cyclic dependency: %s", beanType.getName()));
        }
        cache.add(beanType);

        Constructor<?> constructor = validateGetComponentBeanConstructor(beanType);
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Object[] parameters = validateCollectParameters(cache, parameterTypes);

        try {
            Object instance = constructor.newInstance(parameters);
            injectAnnotatedFields(instance, beanType);
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<Class<?>> resolveDistinctComponentScans(Set<Class<?>> scanAnnotatedClass) {
        Set<SimpleComponentScan> recursive = new HashSet<>(
                scanAnnotatedClass.stream()
                        .map(clazz -> clazz.getAnnotation(SimpleComponentScan.class))
                        .filter(SimpleComponentScan::recursively)
                        .collect(Collectors.toMap(SimpleComponentScan::value, Function.identity(), (a, b) -> a))
                        .values()
        );

        Set<SimpleComponentScan> simple = scanAnnotatedClass.stream()
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
                .flatMap(componentScan ->
                        ClassScanner.findClassesInPackage(componentScan.value(), componentScan.recursively()).stream())
                .collect(Collectors.toSet());
    }

    private Constructor<?> validateGetComponentBeanConstructor(Class<?> componentBeanType) {
        Constructor<?>[] allConstructors = componentBeanType.getDeclaredConstructors();
        if (allConstructors.length == 0) {
            throw new MissingPublicConstructorException(
                    String.format("No public constructor found for class %s", componentBeanType.getName()));
        }

        List<Constructor<?>> beanConstructors = Arrays.stream(allConstructors)
                .filter(allConstructor -> allConstructor.isAnnotationPresent(SimpleBean.class))
                .toList();

        if (beanConstructors.size() > 1) {
            throw new MultipleBeanConstructorsException(
                    String.format("Multiple 'SimpleBean' annotated public constructors found for class %s", componentBeanType.getName()));
        }

        return beanConstructors.isEmpty() ? allConstructors[0] : beanConstructors.get(0);
    }
}
