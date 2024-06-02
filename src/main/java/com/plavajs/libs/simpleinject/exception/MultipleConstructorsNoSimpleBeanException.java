package com.plavajs.libs.simpleinject.exception;

public class MultipleConstructorsNoSimpleBeanException extends RuntimeException {
    public MultipleConstructorsNoSimpleBeanException(String message) {
        super(message);
    }
}
