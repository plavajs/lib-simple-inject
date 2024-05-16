package com.plavajs.libs.simpleinject;

import com.plavajs.libs.simpleinject.annotation.SimpleConfiguration;
import com.plavajs.libs.simpleinject.exception.*;

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
                .map(SimpleBean::new)
                .peek(this::validateDuplicitBean)
                .forEach(beans::add);
        return beans;
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
                            method.getName(),
                            method.getClass().getName()));
        }
    }

    private void validateDuplicitBean(SimpleBean bean) {
        String identifier = bean.getIdentifier();
        beans.forEach(existingBean -> {
            if (existingBean.getType().equals(bean.getType()) && existingBean.getIdentifier().equals(identifier)) {
                String identifierMessage = identifier.isBlank() ? "With empty identifiers."
                        : String.format("With identical identifiers (identifier='%s').", identifier);

                throw new MultipleBeansException(
                        String.format("%s For type: %s, method: '%s()' in: %s and method: '%s()' in: %s",
                                identifierMessage,
                                bean.getType().getName(),
                                bean.getMethod().getName(),
                                bean.getMethod().getDeclaringClass().getName(),
                                existingBean.getMethod().getName(),
                                existingBean.getMethod().getDeclaringClass().getName()));
            }
        });
    }
}
