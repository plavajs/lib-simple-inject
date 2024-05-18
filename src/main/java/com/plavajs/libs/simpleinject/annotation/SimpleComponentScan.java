package com.plavajs.libs.simpleinject.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(SimpleComponentScans.class)
public @interface SimpleComponentScan {
    String[] value() default "";
    boolean recursively() default true;
}
