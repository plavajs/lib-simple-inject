package com.plavajs.libs.simpleinject;

import com.plavajs.libs.simpleinject.exception.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApplicationContext {

    private static final List<ComponentBean> componentsBeans;
    private static final List<SimpleBean> simpleBeans;
    private static final SimpleBeanService simpleBeanService;
    private static final ComponentBeanService componentBeanService;

    static {
        simpleBeanService = new SimpleBeanService();
        componentBeanService = new ComponentBeanService();
        simpleBeans = simpleBeanService.getBeans();
        componentsBeans = componentBeanService.getBeans();
    }

    public static <T> T getInstance(Class<T> clazz, String identifier) {
        Bean bean = validateFindBean(clazz, identifier);
        return clazz.cast(BeanService.createInstance(bean, new HashSet<>()));
    }

    public static <T> T getInstance(Class<T> clazz) {
        return getInstance(clazz, "");
    }

    static Bean validateFindBean(Class<?> type, String identifier) {
        List<Bean> foundBeans = getSimpleBeans(type, identifier);

        if (foundBeans.size() > 1) {
            throw new MultipleBeansException("This should not happen => bug in method 'ApplicationContext.validateFindBean()' !");
        }

        if (foundBeans.isEmpty()) {
            foundBeans = getComponentBeans(type, identifier);
        }

        if (foundBeans.isEmpty()) {
            String identifierMessage = identifier.isBlank() ? "a blank identifier" : String.format("identifier='%s'", identifier);
            throw new MissingBeanException(String.format("No bean registered for class: %s and %s !",
                    type.getName(),
                    identifierMessage));
        }

        if (foundBeans.size() > 1) {
            throw new MultipleBeansException("This should not happen => bug in method ApplicationContext.validateFindBean() !");
        }

        return foundBeans.get(0);
    }

    private static List<Bean> getSimpleBeans(Class<?> type, String identifier) {
        return new ArrayList<>(simpleBeans.stream()
                .filter(bean -> bean.getType().equals(type))
                .filter(bean -> bean.getIdentifier().equals(identifier))
                .toList());
    }

    private static List<Bean> getComponentBeans(Class<?> type, String identifier) {
        return new ArrayList<>(componentsBeans.stream()
                .filter(bean -> bean.getType().equals(type))
                .filter(bean -> bean.getIdentifier().equals(identifier))
                .toList());
    }
}
