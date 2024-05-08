package com.plavajs.libs.simpleinject.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SimpleComponentScan {
    String value() default "";
    boolean recursively() default false;
}
