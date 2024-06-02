package com.plavajs.libs.simpleinject;

import com.plavajs.libs.simpleinject.annotation.SimpleBean;
import com.plavajs.libs.simpleinject.annotation.SimpleComponent;
import com.plavajs.libs.simpleinject.annotation.SimpleComponentScan;
import com.plavajs.libs.simpleinject.annotation.SimpleComponentScans;
import com.plavajs.libs.simpleinject.exception.MissingPublicConstructorException;
import com.plavajs.libs.simpleinject.exception.MultipleBeanConstructorsException;
import com.plavajs.libs.simpleinject.exception.MultipleClassesAnnotatedException;
import com.plavajs.libs.simpleinject.exception.MultipleConstructorsNoSimpleBeanException;
import lombok.extern.log4j.Log4j2;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
final class ComponentBeanService extends BeanService<ComponentBean> {

    @Override
    void loadBeans() {
        if (log.isDebugEnabled()) log.debug("Loading 'ComponentBeans'");
        Set<Class<?>> annotatedClasses = ClassScanner.findClassesAnnotatedWith(SimpleComponentScans.class);
        annotatedClasses.addAll(ClassScanner.findClassesAnnotatedWith(SimpleComponentScan.class));
        validateSingleComponentScanClass(annotatedClasses);

        if (annotatedClasses.isEmpty()) {
            if (log.isDebugEnabled()) log.debug("No 'SimpleComponentScan' found -> no 'SimpleComponents' loaded");
            return;
        }

        Class<?> componentScanAnnotatedClass = new ArrayList<>(annotatedClasses).get(0);
        loadClassesInPackages(componentScanAnnotatedClass).stream()
                .filter(clazz -> clazz.isAnnotationPresent(SimpleComponent.class))
                .map(ComponentBean::new)
                .forEach(bean -> beans.add(bean));

        if (log.isDebugEnabled()) {
            String message = beans.isEmpty() ? "No 'SimpleComponents' loaded" :
                    String.format("Loaded %d 'SimpleComponents': ['%s']",
                            beans.size(),
                            beans.stream().map(bean -> bean.getType().getSimpleName()).sorted().collect(Collectors.joining("', '")));
            log.debug(message);
        }
    }

    static Constructor<?> validateGetComponentBeanConstructor(Class<?> type) {
        Constructor<?>[] allConstructors = type.getDeclaredConstructors();
        if (allConstructors.length == 0) {
            String message = String.format("No public constructor found for type: %s", type.getName());
            log.error(message);
            throw new MissingPublicConstructorException(message);
        }

        List<Constructor<?>> beanConstructors = Arrays.stream(allConstructors)
                .filter(allConstructor -> allConstructor.isAnnotationPresent(SimpleBean.class))
                .toList();

        if (beanConstructors.size() > 1) {
            String message = String.format("Multiple 'SimpleBean' annotated public constructors found for type: %s", type.getName());
            log.error(message);
            throw new MultipleBeanConstructorsException(message);
        }

        if (allConstructors.length > 1 && beanConstructors.isEmpty()) {
            String message = String.format("Multiple constructors found in 'SimpleComponent' class but none 'SimpleBean' annotated: %s",
                    type.getName());
            log.error(message);
            throw new MultipleConstructorsNoSimpleBeanException(message);
        }

        return beanConstructors.isEmpty() ? allConstructors[0] : beanConstructors.get(0);
    }

    private static Set<Class<?>> loadClassesInPackages(Class<?> scanAnnotatedClass) {
        List<SimpleComponentScan> componentScans;
        SimpleComponentScans wrappedComponentScans = scanAnnotatedClass.getAnnotation(SimpleComponentScans.class);
        if (wrappedComponentScans != null) {
            componentScans = new ArrayList<>(Arrays.stream(wrappedComponentScans.value()).toList());
        } else {
            componentScans = List.of(scanAnnotatedClass.getAnnotation(SimpleComponentScan.class));
        }

        Set<String> distinctRecursivePackages = resolveDistinctRecursivePackages(componentScans);
        if (distinctRecursivePackages.contains("")) {
            Set<String> rootPackages = ClassScanner.getRootPackages();
            if (log.isDebugEnabled()) log.debug(
                    "Empty package name found to scan recursively. Scanning for 'SimpleComponents' in all root packages recursively: ['{}']",
                    String.join("', '", rootPackages));

            return getClassesForDistinctPackages(rootPackages, new HashSet<>());
        }

        Set<String> distinctSimplePackages = resolveDistinctSimplePackages(componentScans, distinctRecursivePackages);
        if (distinctSimplePackages.contains("")) {
            Set<String> rootPackages = ClassScanner.getRootPackages();
            if (log.isDebugEnabled()) log.debug(
                    "Empty package name found to scan NOT recursively. Adding root packages to scan for 'SimpleComponents': ['{}']",
                    String.join("', '", rootPackages));

            distinctSimplePackages.remove("");
            distinctSimplePackages.addAll(rootPackages);
        }

        if (log.isDebugEnabled()) {
            String recursivelyMessage = distinctRecursivePackages.isEmpty() ? "" :
                    String.format("recursively in: ['%s']", String.join("', '", distinctRecursivePackages));

            String simpleMessage = distinctSimplePackages.isEmpty() ? "" :
                    String.format("NOT recursively in: ['%s']", String.join("', '", distinctSimplePackages));

            String conjunction = distinctRecursivePackages.isEmpty() || distinctSimplePackages.isEmpty() ? "" : " and ";

            log.debug("Scanning for 'SimpleComponents' {}{}{}", recursivelyMessage, conjunction, simpleMessage);
        }

        return getClassesForDistinctPackages(distinctRecursivePackages, distinctSimplePackages);
    }

    private static Set<String> resolveDistinctRecursivePackages(List<SimpleComponentScan> componentScans) {
        Set<String> recursivePackages = componentScans.stream()
                .filter(SimpleComponentScan::recursively)
                .flatMap(scan -> Arrays.stream(scan.packages()))
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
                .flatMap(scan -> Arrays.stream(scan.packages()))
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

    private static void validateSingleComponentScanClass(Set<Class<?>> annotatedClasses) {
        if (annotatedClasses.size() > 1) {
            String classes = annotatedClasses.stream()
                    .map(Class::getName)
                    .collect(Collectors.joining("', '"));
            String message = String.format("Only one class annotated with '%s' allowed but multiple found: ['%s']", SimpleComponentScan.class.getName(), classes);
            log.error(message);
            throw new MultipleClassesAnnotatedException(message);
        }
    }
}
