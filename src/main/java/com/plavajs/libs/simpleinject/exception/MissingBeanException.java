package com.plavajs.libs.simpleinject.exception;

public class MissingBeanException extends RuntimeException {
    public MissingBeanException(String message) {
        super(message);
    }
}
