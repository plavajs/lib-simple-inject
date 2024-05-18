package com.plavajs.libs.simpleinject;

import com.plavajs.libs.simpleinject.annotation.SimpleEagerInstances;
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

        if (isEagerInstances()) {
            setupInstances();
        }
    }

    public static <T> T getInstance(Class<T> clazz, String identifier) {
        Bean bean = validateFindBean(clazz, identifier);
        if (bean.getInstance() == null) {
            return clazz.cast(BeanService.createInstance(bean, new HashSet<>()));
        }
        return clazz.cast(bean.getInstance());
    }

    public static <T> T getInstance(Class<T> clazz) {
        return getInstance(clazz, "");
    }

    static Bean validateFindBean(Class<?> type, String identifier) {
        List<Bean> allBeansForType = getAllBeansForType(type);
        if (allBeansForType.isEmpty()) {
            throw new MissingBeanException(String.format("No bean registered for class: %s !", type.getName()));
        }

        List<Bean> foundBeans = allBeansForType.stream()
                .filter(bean -> bean instanceof SimpleBean)
                .filter(bean -> bean.getIdentifier().equals(identifier))
                .toList();

        if (foundBeans.size() > 1) {
            throw new MultipleBeansException("This should not happen => bug in method 'ApplicationContext.validateFindBean()' !");
        }

        if (foundBeans.isEmpty()) {
            foundBeans = allBeansForType.stream()
                    .filter(bean -> bean instanceof ComponentBean)
                    .filter(bean -> bean.getIdentifier().equals(identifier))
                    .toList();
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

    private static List<Bean> getAllBeansForType(Class<?> type) {
        List<Bean> foundBeans = new ArrayList<>(simpleBeans);
        foundBeans.addAll(componentsBeans);
        return foundBeans.stream()
                .filter(bean -> bean.getType().equals(type))
                .toList();
    }

    private static boolean isEagerInstances() {
        return !ClassScanner.findClassesAnnotatedWith(SimpleEagerInstances.class).isEmpty();
    }

    private static void setupInstances() {
        simpleBeans.forEach(bean -> {
            if (bean.getInstance() == null) {
                bean.setInstance(SimpleBeanService.createInstance(bean, new HashSet<>()));
            }
        });
        componentsBeans.forEach(bean -> {
            if (bean.getInstance() == null) {
                bean.setInstance(ComponentBeanService.createInstance(bean, new HashSet<>()));
            }
        });
    }
}
