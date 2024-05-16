package com.plavajs.libs.simpleinject;

import com.plavajs.libs.simpleinject.annotation.SimpleBean;
import com.plavajs.libs.simpleinject.annotation.SimpleComponent;
import com.plavajs.libs.simpleinject.annotation.SimpleComponentScan;
import com.plavajs.libs.simpleinject.annotation.SimpleComponentScans;
import com.plavajs.libs.simpleinject.exception.MissingPublicConstructorException;
import com.plavajs.libs.simpleinject.exception.MultipleBeanConstructorsException;
import com.plavajs.libs.simpleinject.exception.MultipleClassesAnnotatedException;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

final class ComponentBeanService extends BeanService<ComponentBean> {

    @Override
    @SuppressWarnings("unchecked")
    List<ComponentBean> loadBeans() {
        Set<Class<?>> annotatedClasses = ClassScanner.findClassesAnnotatedWith(SimpleComponentScans.class);
        annotatedClasses.addAll(ClassScanner.findClassesAnnotatedWith(SimpleComponentScan.class));
        validateMultipleComponentScanClasses(annotatedClasses);

        Class<?> componentScanAnnotatedClass = new ArrayList<>(annotatedClasses).get(0);
        return resolveDistinctComponentScans(componentScanAnnotatedClass).stream()
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

    private Set<Class<?>> resolveDistinctComponentScans(Class<?> scanAnnotatedClass) {
        SimpleComponentScans componentScans = scanAnnotatedClass.getAnnotation(SimpleComponentScans.class);
        if (componentScans == null) {
            SimpleComponentScan componentScan = scanAnnotatedClass.getAnnotation(SimpleComponentScan.class);
            return ClassScanner.findClassesInPackage(componentScan.value(), componentScan.recursively());
        }

        Set<SimpleComponentScan> recursive = new HashSet<>(Arrays.stream(componentScans.value())
                        .filter(SimpleComponentScan::recursively)
                        .collect(Collectors.toMap(SimpleComponentScan::value, Function.identity(), (a, b) -> a))
                        .values());

        Set<SimpleComponentScan> simple = Arrays.stream(componentScans.value())
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

    private static void validateMultipleComponentScanClasses(Set<Class<?>> componentScanAnnotated) {
        if (componentScanAnnotated.size() > 1) {
            String classes = componentScanAnnotated.stream()
                    .map(Class::getName)
                    .collect(Collectors.joining("', '"));

            throw new MultipleClassesAnnotatedException(String.format("Only one class annotated with '%s' allowed! ['%s']",
                    SimpleComponentScan.class.getName(),
                    classes));
        }
    }
}
