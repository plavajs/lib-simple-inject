package com.plavajs.libs.simpleinject;

import com.plavajs.libs.simpleinject.exception.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApplicationContext {

    @Getter //TODO make only package accessible
    private static final List<ComponentBean> componentsBeans;

    @Getter //TODO make only package accessible
    private static final List<SimpleBean> simpleBeans;

    private static final SimpleBeanService simpleBeanService;
    private static final ComponentBeanService componentBeanService;

    static {
        simpleBeanService = new SimpleBeanService();
        componentBeanService = new ComponentBeanService();
        simpleBeans = simpleBeanService.getBeans();
        componentsBeans = componentBeanService.getBeans();
        setupInstances();
    }

    public static <T> T getInstance(Class<T> clazz) {
        Bean parameterBean = validateFindBean(clazz);
        T instance = clazz.cast(parameterBean.getClass().cast(parameterBean).getInstance());
        BeanService.injectAnnotatedFields(instance, clazz);
        return instance;
    }

    static Bean validateFindBean(Class<?> beanType) {
        for (SimpleBean bean : simpleBeans) {
            if (bean.getType().equals(beanType)) {
                return bean;
            }
        }
        for (ComponentBean component : componentsBeans) {
            if (component.getType().equals(beanType)) {
                return component;
            }
        }
        throw new MissingBeanException(String.format("No bean registered for class %s", beanType.getName()));
    }

    private static void setupInstances() {
        simpleBeans.forEach(simpleBeanService::setupInstance);
        componentsBeans.forEach(componentBeanService::setupInstance);
    }
}
