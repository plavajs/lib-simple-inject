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
import java.util.stream.Collectors;

final class ComponentBeanService extends BeanService<ComponentBean> {

    @Override
    @SuppressWarnings("unchecked")
    List<ComponentBean> loadBeans() {
        Set<Class<?>> annotatedClasses = ClassScanner.findClassesAnnotatedWith(SimpleComponentScans.class);
        annotatedClasses.addAll(ClassScanner.findClassesAnnotatedWith(SimpleComponentScan.class));
        validateSingleComponentScanClass(annotatedClasses);

        Class<?> componentScanAnnotatedClass = new ArrayList<>(annotatedClasses).get(0);
        return loadClassesInPackages(componentScanAnnotatedClass).stream()
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

    private static Set<Class<?>> loadClassesInPackages(Class<?> scanAnnotatedClass) {
        List<SimpleComponentScan> componentScans;
        SimpleComponentScans wrappedComponentScans = scanAnnotatedClass.getAnnotation(SimpleComponentScans.class);
        if (wrappedComponentScans != null) {
            componentScans = new ArrayList<>(Arrays.stream(wrappedComponentScans.value()).toList());
        } else {
            SimpleComponentScan componentScan = scanAnnotatedClass.getAnnotation(SimpleComponentScan.class);
            componentScans = List.of(componentScan);
        }

        Set<String> distinctRecursivePackages = resolveDistinctRecursivePackages(componentScans);
        if (distinctRecursivePackages.contains("")) {
            return getClassesForDistinctPackages(ClassScanner.getRootPackages(), new HashSet<>());
        }

        Set<String> distinctSimplePackages = resolveDistinctSimplePackages(componentScans, distinctRecursivePackages);
        if (distinctSimplePackages.contains("")) {
            distinctSimplePackages.remove("");
            distinctSimplePackages.addAll(ClassScanner.getRootPackages());
        }

        return getClassesForDistinctPackages(distinctRecursivePackages, distinctSimplePackages);
    }

    private static Set<String> resolveDistinctRecursivePackages(List<SimpleComponentScan> componentScans) {
        Set<String> recursivePackages = componentScans.stream()
                .filter(SimpleComponentScan::recursively)
                .flatMap(scan -> Arrays.stream(scan.value()))
                .collect(Collectors.toSet());

        return new HashSet<>(recursivePackages).stream()
                .filter(packageName -> recursivePackages.stream()
                        .noneMatch(originalPackage -> {
                            if (packageName.equals(originalPackage)) {
                                return false;
                            }
                            return packageName.startsWith(originalPackage);
                        }))
                .collect(Collectors.toSet());
    }

    private static Set<String> resolveDistinctSimplePackages(List<SimpleComponentScan> componentScans, Set<String> distinctRecursivePackages) {
        Set<String> simplePackages = componentScans.stream()
                .filter(scan -> !scan.recursively())
                .flatMap(scan -> Arrays.stream(scan.value()))
                .collect(Collectors.toSet());

        return simplePackages.stream()
                .filter(packageName -> distinctRecursivePackages.stream().noneMatch(packageName::startsWith))
                .collect(Collectors.toSet());
    }

    private static Set<Class<?>> getClassesForDistinctPackages(Set<String> distinctRecursivePackages, Set<String> distinctSimplePackages) {
        Map<String, Boolean> packagesMap = distinctRecursivePackages.stream()
                .collect(Collectors.toMap(packageName -> packageName, packageName -> true));

        packagesMap.putAll(distinctSimplePackages.stream()
                .collect(Collectors.toMap(packageName -> packageName, packageName -> false)));

        return packagesMap.entrySet().stream()
                .flatMap(entry -> ClassScanner.findClassesInPackage(entry.getKey(), entry.getValue()).stream())
                .collect(Collectors.toSet());
    }

    private static void validateSingleComponentScanClass(Set<Class<?>> componentScanAnnotated) {
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
