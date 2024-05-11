package com.plavajs.libs.simpleinject;

import com.plavajs.libs.simpleinject.annotation.SimpleConfiguration;
import com.plavajs.libs.simpleinject.exception.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

final class SimpleBeanService extends BeanService<SimpleBean> {

    @Override
    @SuppressWarnings("unchecked")
    List<SimpleBean> loadBeans() {
        beans = new ArrayList<>();
        ClassScanner.findClassesAnnotatedWith(SimpleConfiguration.class).stream()
                .peek(this::validateConfigurationClass)
                .flatMap(clazz -> Arrays.stream(clazz.getDeclaredMethods()))
                .filter(method -> method.isAnnotationPresent(com.plavajs.libs.simpleinject.annotation.SimpleBean.class))
                .peek(this::validateSimpleBeanMethod)
                .map(method -> new SimpleBean(method, method.getReturnType()))
                .peek(this::validateDuplicitBean)
                .forEach(beans::add);
        return beans;
    }

    @Override
    <B extends Bean> void setupInstance(B bean) {
        validateBeanType(bean);
        bean.setInstance(createInstance(bean, new HashSet<>()));
    }

    @Override
    Object createInstance(Bean bean, Set<Class<?>> cache) {
        validateBeanType(bean);

        Class<?> type = bean.getType();
        if (cache.contains(type)) {
            throw new CyclicDependencyException(String.format("Cyclic dependency: %s", type.getName()));
        }
        cache.add(type);

        Method method = ((SimpleBean) bean).getMethod();
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] parameters = validateCollectParameters(cache, parameterTypes);

        try {
            Object instance = method.invoke(null, parameters);
            injectAnnotatedFields(instance, type);
            return instance;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private void validateConfigurationClass(Class<?> clazz) {
        try {
            clazz.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new MissingPublicNoArgumentConstructorException(
                    String.format("Configuration class must have a public constructor with no arguments! (%s)", clazz.getName()));
        }
    }

    private void validateSimpleBeanMethod(Method method) {
        if (!Modifier.isStatic(method.getModifiers()) || !Modifier.isPublic(method.getModifiers())) {
            throw new ConfigBeanMethodNotPublicStaticException(
                    String.format("Configuration 'SimpleBean' annotated method must be public static! (%s in %s)",
                            method.getName(), method.getClass().getName()));
        }
    }

    private void validateDuplicitBean(SimpleBean bean) {
        beans.forEach(existingSimpleBean -> {
            if (existingSimpleBean.getType().equals(bean.getType())) {
                throw new DuplicitBeanException(
                        String.format("There is already a bean registered for type %s", bean.getType().getName()));
            }
        });
    }
}
