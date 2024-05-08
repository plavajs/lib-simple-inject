package com.plavajs.libs.simpleinject;

import com.plavajs.libs.simpleinject.annotation.*;
import com.plavajs.libs.simpleinject.exception.*;
import com.plavajs.libs.simpleinject.model.AbstractBean;
import com.plavajs.libs.simpleinject.model.Bean;
import com.plavajs.libs.simpleinject.model.Component;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApplicationContext {

    @Getter
    private static final List<Component> components = new ArrayList<>();

    @Getter
    private static final List<Bean> beans = new ArrayList<>();

    public static void initializeContext() {
        ClassScanner.loadAllClasses();
        registerBeans();
        registerComponents();
        validateCreateBeanComponentInstances();
    }

    public static  <T> T getInstance(Class<T> clazz) {
        Bean foundBean = findBean(clazz);
        AbstractBean parameterBean = foundBean == null ? findComponent(clazz) : foundBean;
        if (parameterBean == null) {
            throw new MissingBeanException(String.format("No bean registered for class %s", clazz.getName()));
        }
        T instance = clazz.cast(parameterBean.getClass().cast(parameterBean).getInstance());
        injectAnnotatedFields(instance, clazz);
        return instance;
    }

    private static void registerBeans() {
        Set<Class<?>> configurationAnnotated = ClassScanner.findClassesAnnotatedWith(SimpleConfiguration.class);

        configurationAnnotated.stream()
                .peek(ApplicationContext::validateConfigurationClass)
                .flatMap(clazz -> Arrays.stream(clazz.getDeclaredMethods()))
                .filter(method -> method.isAnnotationPresent(SimpleBean.class))
                .peek(method -> {
                    if (!Modifier.isStatic(method.getModifiers())) {
                        throw new ConfigurationBeanMethodNotStaticException(
                                String.format("Configuration 'SimpleBean' method must be static! (%s in %s)",
                                        method.getName(), method.getClass().getName()));
                    }
                })
                .map(method -> new Bean(method, method.getReturnType()))
                .peek(ApplicationContext::validateDuplicitBean)
                .forEach(beans::add);
    }

    private static void validateConfigurationClass(Class<?> clazz) {
        try {
            Constructor<?> constructor = clazz.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new MissingNoArgumentConstructorException(
                    String.format("Configuration class must have a public constructor with no arguments! (%s)", clazz.getName()));
        }
    }
    
    private static void registerComponents() {
        Set<Class<?>> componentScanAnnotated = ClassScanner.findClassesAnnotatedWith(SimpleComponentScan.class);

        resolveDistinctComponentScans(componentScanAnnotated).stream()
                .filter(clazz -> clazz.isAnnotationPresent(SimpleComponent.class))
                .map(Component::new)
                .forEach(components::add);
    }

    private static void validateDuplicitBean(Bean bean) {
        beans.forEach(existingSimpleBean -> {
            if (existingSimpleBean.getType().equals(bean.getType())) {
                throw new DuplicitBeanException(
                        String.format("There is already a bean registered for type %s",
                                bean.getType().getName()));
            }
        });
    }

    private static void validateCreateBeanComponentInstances() {
        List<AbstractBean> allBeans = new ArrayList<>(beans);
        allBeans.addAll(components);
        allBeans.forEach(bean -> bean.setInstance(validateCreateBeanComponentInstance(bean, new HashSet<>())));
    }

    private static Object validateCreateBeanComponentInstance(AbstractBean bean, Set<Class<?>> cache) {
        if (bean instanceof Bean) {
            return createBeanInstance((Bean) bean, cache);
        } else {
            return createComponentInstanceNew(bean.getType(), cache);
        }
    }

    private static Object createBeanInstance(Bean bean, Set<Class<?>> cache) {
        Class<?> beanType = bean.getType();
        Class<?>[] parameterTypes;
        Method method = bean.getMethod();
        cache.add(beanType);
        parameterTypes = method.getParameterTypes();
        List<Object> parameters = validateCollectParameters(cache, parameterTypes, beanType);
        
        try {
            return bean.getMethod().invoke(parameters);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object createComponentInstanceNew(Class<?> beanType, Set<Class<?>> cache) {
        Class<?>[] parameterTypes;
        Constructor<?> constructor;
        Constructor<?>[] allConstructors = beanType.getDeclaredConstructors();
        if (allConstructors.length == 0) {
            throw new MissingPublicConstructorException(String.format("No public constructor found for class %s", beanType.getName()));
        }

        List<Constructor<?>> beanConstructors = Arrays.stream(allConstructors)
                .filter(allConstructor -> allConstructor.isAnnotationPresent(SimpleBean.class))
                .toList();

        if (beanConstructors.size() > 1) {
            throw new MultipleBeanConstructorsException(
                    String.format("Multiple 'SimpleBean' annotated constructors found for class %s", beanType.getName()));
        }

        constructor = beanConstructors.isEmpty() ? allConstructors[0] : beanConstructors.get(0);
        parameterTypes = constructor.getParameterTypes();
        List<Object> parameters = validateCollectParameters(cache, parameterTypes, beanType);
        
        try {
            return constructor.newInstance(parameters.toArray());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Object> validateCollectParameters(Set<Class<?>> cache, Class<?>[] parameterTypes, Class<?> beanType) {
        List<Object> parameters = new ArrayList<>();

        Arrays.stream(parameterTypes)
                .forEach(parameterType -> {
                    AbstractBean parameterBean = validateFindBean(parameterType, cache);
                    Object parameter = parameterBean.getInstance() == null ?
                            createComponentInstanceNew(parameterBean.getType(), cache) : parameterBean.getInstance();

                    parameters.add(parameter);
                });
        return parameters;
    }

    private static AbstractBean validateFindBean(Class<?> beanType, Set<Class<?>> cache) {
        if (cache.contains(beanType)) {
            throw new CyclicDependencyException(String.format("Cyclic dependency: %s", beanType.getName()));
        }

        Bean foundBean = findBean(beanType);
        AbstractBean parameterBean = foundBean == null ? findComponent(beanType) : foundBean;
        if (parameterBean == null) {
            throw new MissingBeanException(String.format("No bean registered for class %s", beanType.getName()));
        }
        return parameterBean;
    }

    private static Bean findBean(Class<?> type) {
        for (Bean bean : beans) {
            if (bean.getType().equals(type)) {
                return bean;
            }
        }
        return null;
    }

    private static Component findComponent(Class<?> type) {
        for (Component component : components) {
            if (component.getType().equals(type)) {
                return component;
            }
        }
        return null;
    }

    private static Set<Class<?>> resolveDistinctComponentScans(Set<Class<?>> scanAnnotatedClass) {
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
                .flatMap(componentScan -> ClassScanner.findClassesInPackage(componentScan.value(), componentScan.recursively()).stream())
                .collect(Collectors.toSet());
    }

    private static <T> void injectAnnotatedFields(T object, Class<?> clazz) {
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(SimpleInject.class)) {
                field.setAccessible(true);
                Class<?> parameterType = field.getType();
                Object innerObject = getInstance(parameterType);
                try {
                    if (innerObject == null) {
//                        innerObject = createComponentInstanceNew(parameterType, new HashSet<>());
                        throw new MissingBeanException(String.format("No bean found for class %s", parameterType.getName()));
                    }
                    field.set(object, parameterType.cast(innerObject));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                injectAnnotatedFields(innerObject, parameterType);
            }
        }
    }
}
