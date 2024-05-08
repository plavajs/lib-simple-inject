package com.plavajs.libs.simpleinject;

import lombok.Getter;

import java.io.*;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class ClassScanner {

    @Getter
    private static final Set<Class<?>> allClasses = new HashSet<>();

    public static Set<Class<?>> findClassesInPackage(String packageName, boolean recursively) {
        return allClasses.stream()
                .filter(clazz -> {
                    if (recursively) {
                        return clazz.getPackageName().startsWith(packageName);
                    }
                    return clazz.getPackageName().equals(packageName);
                })
                .collect(Collectors.toSet());

//
//        Set<Class<?>> classes = new HashSet<>();
//        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
//
//        Enumeration<URL> resources;
//        try {
//            resources = classLoader.getResources(packageName.replaceAll("[.]", "/"));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        while (resources.hasMoreElements()) {
//            URL resourceUrl = resources.nextElement();
//            if (resourceUrl.getProtocol().equals("file")) {
//                File directory = new File(resourceUrl.getFile());
//                scanDirectory(directory, packageName, classes);
//            }
//        }
//
//        return classes;
    }

    static void loadAllClasses() {
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

    public static Set<Class<?>> findClassesAnnotatedWith(Class<? extends Annotation> annotationClass) {
        Set<Class<?>> classes = findClassesInPackage("", true);
        Set<Class<?>> filtered = classes.stream().filter(clazz -> clazz.isAnnotationPresent(annotationClass)).collect(Collectors.toSet());
        return filtered;
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
