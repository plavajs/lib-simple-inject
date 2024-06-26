package com.plavajs.libs.simpleinject;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ClassScanner {

    private static final Set<Class<?>> allClasses = new HashSet<>();

    static {
        loadAllClasses();
    }

    static Set<Class<?>> findClassesAnnotatedWith(Class<? extends Annotation> annotationClass) {
        return allClasses.stream()
                .filter(clazz -> clazz.isAnnotationPresent(annotationClass))
                .collect(Collectors.toSet());
    }

    static Set<Class<?>> findClassesInPackage(String packageName, boolean recursively) {
        return allClasses.stream()
                .filter(clazz -> recursively ? clazz.getPackageName().startsWith(packageName) :
                        clazz.getPackageName().equals(packageName))
                .collect(Collectors.toSet());
    }

    static Set<String> getRootPackages() {
        return allClasses.stream()
                .map(Class::getPackageName)
                .map(packageName ->  packageName.indexOf('.') == -1 ? packageName :
                        packageName.substring(0, packageName.indexOf('.')))
                .collect(Collectors.toSet());
    }

    private static void loadAllClasses() {
        if (log.isDebugEnabled()) log.debug("Loading all classes");
        Set<Class<?>> classes = new HashSet<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String packageName = "";

        Enumeration<URL> resources;
        try {
            resources = classLoader.getResources(packageName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        while (resources.hasMoreElements()) {
            URL resourceUrl = resources.nextElement();
            if (resourceUrl.getProtocol().equals("file")) {
                File directory = new File(resourceUrl.getFile());
                scanDirectory(directory, packageName, classes);
            }
        }

        if (log.isDebugEnabled()) log.debug("Loaded {} classes", classes.size());
        allClasses.addAll(classes);
    }

    private static void scanDirectory(File directory, String packageName, Set<Class<?>> classes) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                String nextPackageName = packageName.isBlank() ? file.getName() : packageName + "." + file.getName();
                scanDirectory(file, nextPackageName, classes);
            } else if (file.getName().endsWith(".class")) {
                Class<?> type;
                try {
                    type = Class.forName(packageName + "." + file.getName().substring(0, file.getName().lastIndexOf('.')));
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                classes.add(type);
            }
        }
    }
}
