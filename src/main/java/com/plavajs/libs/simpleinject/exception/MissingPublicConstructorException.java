package com.plavajs.libs.simpleinject.exception;

public class MissingPublicConstructorException extends RuntimeException {
    public MissingPublicConstructorException(String message) {
        super(message);
    }
}
