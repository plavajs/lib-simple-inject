package com.plavajs.libs.simpleinject.exception;

public class CyclicDependenciesException extends RuntimeException {
    public CyclicDependenciesException(String message) {
        super(message);
    }
}
