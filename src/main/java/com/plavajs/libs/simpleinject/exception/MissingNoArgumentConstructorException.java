package com.plavajs.libs.simpleinject.exception;

public class MissingNoArgumentConstructorException extends RuntimeException {
    public MissingNoArgumentConstructorException(String message) {
        super(message);
    }
}
