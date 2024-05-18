package com.plavajs.libs.simpleinject;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.*;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

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
                .filter(clazz -> {
                    if (recursively) {
                        return clazz.getPackageName().startsWith(packageName);
                    }
                    return clazz.getPackageName().equals(packageName);
                })
                .collect(Collectors.toSet());
    }

    static Set<String> getRootPackages() {
        return allClasses.stream()
                .map(Class::getPackageName)
                .map(packageName -> packageName.substring(0, packageName.indexOf('.')))
                .collect(Collectors.toSet());
    }

    private static void loadAllClasses() {
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
                Class<?> clazz;
                try {
                    clazz = Class.forName(packageName + "." + file.getName().substring(0, file.getName().lastIndexOf('.')));
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                classes.add(clazz);
            }
        }
    }
}
