package com.plavajs.libs.simpleinject;

import com.plavajs.libs.simpleinject.annotation.SimpleEagerInstances;
import com.plavajs.libs.simpleinject.exception.MissingBeanException;
import com.plavajs.libs.simpleinject.exception.DuplicitBeansException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Log4j2
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApplicationContext {

    private static final MethodBeanService methodBeanService = new MethodBeanService();
    private static final ComponentBeanService componentBeanService = new ComponentBeanService();
    private static final Set<ComponentBean> componentBeans;
    private static final Set<MethodBean> methodBeans;

    static {
        methodBeans = methodBeanService.getBeans();
        componentBeans = componentBeanService.getBeans();

        boolean isEagerInstances = isEagerInstances();
        if (log.isDebugEnabled()) log.debug("Eager instances: {}", String.valueOf(isEagerInstances).toUpperCase());
        if (isEagerInstances) {
            setupInstances();
        }
    }

    /**
     * @param type       the type you want to return the instance of
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
     * @param type the type you want to return the instance of
     * @return instance of the specified type
     */
    public static <T> T getInstance(Class<T> type) {
        return getInstance(type, "");
    }

    static Bean validateFindBean(Class<?> type, String identifier) {
        List<Bean> allBeansForType = findAllBeansForType(type);
        if (allBeansForType.isEmpty()) {
            throw new MissingBeanException(String.format("No bean registered for type: %s !", type.getName()));
        }

        List<Bean> foundBeans = allBeansForType.stream()
                .filter(bean -> bean instanceof MethodBean)
                .filter(bean -> bean.getIdentifier().equals(identifier))
                .toList();

        if (foundBeans.size() > 1) {
            throw new DuplicitBeansException("This should not happen => bug in method 'ApplicationContext.validateFindBean()' !");
        }

        if (foundBeans.isEmpty()) {
            foundBeans = allBeansForType.stream()
                    .filter(bean -> bean instanceof ComponentBean)
                    .filter(bean -> bean.getIdentifier().equals(identifier))
                    .toList();
        }

        if (foundBeans.isEmpty()) {
            String identifierMessage = identifier.isBlank() ? "a blank identifier" : String.format("identifier='%s'", identifier);
            throw new MissingBeanException(String.format("No bean registered for type: %s and %s !",
                    type.getName(),
                    identifierMessage));
        }

        if (foundBeans.size() > 1) {
            throw new DuplicitBeansException("This should not happen => bug in method ApplicationContext.validateFindBean() !");
        }

        return foundBeans.get(0);
    }

    private static List<Bean> findAllBeansForType(Class<?> type) {
        List<Bean> foundBeans = new ArrayList<>(methodBeans);
        foundBeans.addAll(componentBeans);
        return foundBeans.stream()
                .filter(bean -> bean.getType().equals(type))
                .toList();
    }

    private static boolean isEagerInstances() {
        return !ClassScanner.findClassesAnnotatedWith(SimpleEagerInstances.class).isEmpty();
    }

    private static void setupInstances() {
        if (log.isDebugEnabled()) log.debug("Setting up instances for all beans");
        methodBeans.forEach(bean -> {
            if (bean.getInstance() == null) bean.setInstance(MethodBeanService.createInstance(bean, new HashSet<>()));
        });
        componentBeans.forEach(bean -> {
            if (bean.getInstance() == null) bean.setInstance(ComponentBeanService.createInstance(bean, new HashSet<>()));
        });
        if (log.isDebugEnabled()) log.debug("All beans instantiated");
    }
}
