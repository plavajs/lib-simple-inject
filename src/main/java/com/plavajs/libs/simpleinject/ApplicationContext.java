package com.plavajs.libs.simpleinject;

import com.plavajs.libs.simpleinject.annotation.SimpleEagerInstances;
import com.plavajs.libs.simpleinject.exception.MissingBeanException;
import com.plavajs.libs.simpleinject.exception.MultipleBeansException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Log4j2
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

        boolean isEagerInstances = isEagerInstances();
        if (log.isDebugEnabled()) log.debug("Eager instances: {}", String.valueOf(isEagerInstances).toUpperCase());
        if (isEagerInstances) {
            setupInstances();
        }
    }

    /**
     * @param type       the class of the type you want to return the instance of
     * @param identifier the unique identifier of the bean you want to use for instantiation
     * @return instance of the specified type
     */
    public static <T> T getInstance(Class<T> type, String identifier) {
        Bean bean = validateFindBean(type, identifier);
        if (bean.getInstance() == null) {
            return type.cast(BeanService.createInstance(bean, new HashSet<>()));
        }
        return type.cast(bean.getInstance());
    }

    /**
     * @param type the class of the type you want to return the instance of
     * @return instance of the specified type
     */
    public static <T> T getInstance(Class<T> type) {
        return getInstance(type, "");
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
        if (log.isDebugEnabled()) log.debug("Setting up instances for all beans");
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
        if (log.isDebugEnabled()) log.debug("All beans instantiated");
    }
}
