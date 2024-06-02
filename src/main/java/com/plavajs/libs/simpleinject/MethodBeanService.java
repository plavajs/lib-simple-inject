package com.plavajs.libs.simpleinject;

import com.plavajs.libs.simpleinject.annotation.SimpleBean;
import com.plavajs.libs.simpleinject.annotation.SimpleConfiguration;
import com.plavajs.libs.simpleinject.exception.*;
import lombok.extern.log4j.Log4j2;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

@Log4j2
final class MethodBeanService extends BeanService<MethodBean> {

    @Override
      void loadBeans() {
        if (log.isDebugEnabled()) log.debug("Loading 'SimpleBeans'");

        Set<Class<?>> configurationClasses = ClassScanner.findClassesAnnotatedWith(SimpleConfiguration.class);
        if (configurationClasses.isEmpty()) {
            if (log.isDebugEnabled()) log.debug("No 'SimpleConfiguration' classes found -> no 'SimpleBeans' loaded");
            return;
        }

        Set<MethodBean> beans = new HashSet<>();
        configurationClasses.stream()
                .peek(this::validateConfigurationClass)
                .flatMap(clazz -> Arrays.stream(clazz.getDeclaredMethods()))
                .filter(method -> method.isAnnotationPresent(SimpleBean.class))
                .peek(this::validateBeanMethod)
                .map(MethodBean::new)
                .peek(this::validateDuplicitBean)
                .forEach(bean -> {
                    this.beans.add(bean);
                    beans.add(bean);
                });

        if (log.isDebugEnabled()) {
            String message = beans.isEmpty() ? "No 'SimpleBeans' loaded" : String.format("Loaded %d 'SimpleBeans': %s", beans.size(),
                    beans.stream().map(bean -> {
                        String identifierMessage = bean.getIdentifier().isBlank() ? "" : String.format("'%s' ", bean.getIdentifier());
                        return String.format("%s: %s%s()", bean.getType().getSimpleName(), identifierMessage, bean.getMethod().getName());
                    }).sorted().toList());

            log.debug(message);
        }
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

    private void validateBeanMethod(Method method) {
        if (!Modifier.isStatic(method.getModifiers()) || !Modifier.isPublic(method.getModifiers())) {
            String message = String.format("'SimpleBean' annotated method must be public static! ['%s()' in: %s]",
                            method.getName(),
                            method.getDeclaringClass().getName());
            log.error(message);
            throw new ConfigBeanMethodNotPublicStaticException(message);
        }
    }

    private void validateDuplicitBean(MethodBean bean) {
        String identifier = bean.getIdentifier();
        beans.forEach(existingBean -> {
            if (existingBean.equals(bean)) {
                String identifierMessage = identifier.isBlank() ? "with empty identifiers"
                        : String.format("with identical identifiers (identifier='%s')", identifier);

                String message = String.format("Duplicit beans %s. For type: %s, method: '%s()' in: %s and method: '%s()' in: %s",
                        identifierMessage,
                        bean.getType().getName(),
                        bean.getMethod().getName(),
                        bean.getMethod().getDeclaringClass().getName(),
                        existingBean.getMethod().getName(),
                        existingBean.getMethod().getDeclaringClass().getName());

                log.error("Duplicit 'SimpleBean' methods found! {}", message);
                throw new DuplicitBeansException(message);
            }
        });
    }
}
