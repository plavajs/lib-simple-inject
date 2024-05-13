package com.plavajs.libs.simpleinject.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SimpleComponent {
    String identifier() default "";
}
