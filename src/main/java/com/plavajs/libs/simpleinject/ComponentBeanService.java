package com.plavajs.libs.simpleinject;

import com.plavajs.libs.simpleinject.annotation.SimpleBean;
import com.plavajs.libs.simpleinject.annotation.SimpleComponent;
import com.plavajs.libs.simpleinject.annotation.SimpleComponentScan;
import com.plavajs.libs.simpleinject.exception.MissingPublicConstructorException;
import com.plavajs.libs.simpleinject.exception.MultipleBeanConstructorsException;

import java.lang.reflect.Constructor;
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

    static Constructor<?> validateGetComponentBeanConstructor(Class<?> type) {
        Constructor<?>[] allConstructors = type.getDeclaredConstructors();
        if (allConstructors.length == 0) {
            throw new MissingPublicConstructorException(
                    String.format("No public constructor found for class %s", type.getName()));
        }

        List<Constructor<?>> beanConstructors = Arrays.stream(allConstructors)
                .filter(allConstructor -> allConstructor.isAnnotationPresent(SimpleBean.class))
                .toList();

        if (beanConstructors.size() > 1) {
            throw new MultipleBeanConstructorsException(
                    String.format("Multiple 'SimpleBean' annotated public constructors found for class %s", type.getName()));
        }

        return beanConstructors.isEmpty() ? allConstructors[0] : beanConstructors.get(0);
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
}
