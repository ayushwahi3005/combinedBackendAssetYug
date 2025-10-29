package com.quantumai.customer.exception;

public class UserCannotActivateException extends Exception{

    private static final long serialVersionUID = 1140070368086512313L;

    public UserCannotActivateException(String message) {
        super(message);
    }
}
