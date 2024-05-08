package com.plavajs.libs.simpleinject.exception;

public class MissingPublicNoArgumentConstructorException extends RuntimeException {
    public MissingPublicNoArgumentConstructorException(String message) {
        super(message);
    }
}
