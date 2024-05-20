package com.plavajs.libs.simpleinject;

import com.plavajs.libs.simpleinject.annotation.SimpleConfiguration;
import com.plavajs.libs.simpleinject.exception.*;
import lombok.extern.log4j.Log4j2;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

@Log4j2
final class SimpleBeanService extends BeanService<SimpleBean> {

    @Override
    @SuppressWarnings("unchecked")
    List<SimpleBean> loadBeans() {
        if (log.isDebugEnabled()) log.debug("Loading 'SimpleBeans'");
        beans = new ArrayList<>();

        Set<Class<?>> configurationClasses = ClassScanner.findClassesAnnotatedWith(SimpleConfiguration.class);
        if (configurationClasses.isEmpty()) {
            if (log.isDebugEnabled()) log.debug("No 'SimpleConfiguration' classes found -> no 'SimpleBeans' loaded");
            return new ArrayList<>();
        }

        List<SimpleBean> beans = new ArrayList<>();
        configurationClasses.stream()
                .peek(this::validateConfigurationClass)
                .flatMap(clazz -> Arrays.stream(clazz.getDeclaredMethods()))
                .filter(method -> method.isAnnotationPresent(com.plavajs.libs.simpleinject.annotation.SimpleBean.class))
                .peek(this::validateSimpleBeanMethod)
                .map(SimpleBean::new)
                .peek(this::validateDuplicitBean)
                .forEach(beans::add);

        if (log.isDebugEnabled()) log.debug("Loaded {} 'SimpleBeans': {}", beans.size(), beans.stream().map(bean -> {
            String identifierMessage = bean.getIdentifier().isBlank() ? "" : "'" + bean.getIdentifier() + "' ";
            return bean.getType().getSimpleName() + ": " + identifierMessage + bean.getMethod().getName() + "()";
        }).toList());

        return beans;
    }

    private void validateConfigurationClass(Class<?> clazz) {
        try {
            clazz.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            String message = String.format(
                    "'SimpleConfiguration' class must have a public constructor with no arguments! [%s]", clazz.getName());

            log.error(message);
            throw new MissingPublicNoArgumentConstructorException(message);
        }
    }

    private void validateSimpleBeanMethod(Method method) {
        if (!Modifier.isStatic(method.getModifiers()) || !Modifier.isPublic(method.getModifiers())) {
            String message = String.format("'SimpleBean' annotated method must be public static! ['%s()' in: %s]",
                            method.getName(),
                            method.getDeclaringClass().getName());
            log.error(message);
            throw new ConfigBeanMethodNotPublicStaticException(message);
        }
    }

    private void validateDuplicitBean(SimpleBean bean) {
        String identifier = bean.getIdentifier();
        beans.forEach(existingBean -> {
            if (existingBean.getType().equals(bean.getType()) && existingBean.getIdentifier().equals(identifier)) {
                String identifierMessage = identifier.isBlank() ? "With empty identifiers."
                        : String.format("With identical identifiers (identifier='%s').", identifier);

                String message = String.format("%s For type: %s, method: '%s()' in: %s and method: '%s()' in: %s",
                        identifierMessage,
                        bean.getType().getName(),
                        bean.getMethod().getName(),
                        bean.getMethod().getDeclaringClass().getName(),
                        existingBean.getMethod().getName(),
                        existingBean.getMethod().getDeclaringClass().getName());

                log.error("Duplicit 'SimpleBean' methods found! {}", message);
                throw new MultipleBeansException(message);
            }
        });
    }
}
