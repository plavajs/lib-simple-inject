package com.plavajs.libs.simpleinject.exception;

public class MultipleBeanConstructorsException extends RuntimeException {
    public MultipleBeanConstructorsException(String message) {
        super(message);
    }
}
