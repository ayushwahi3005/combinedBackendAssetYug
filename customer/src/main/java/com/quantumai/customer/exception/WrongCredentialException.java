package com.quantumai.customer.exception;

public class WrongCredentialException extends Exception{
    private static final long serialVersionUID = -9046302148486644676L;

    public WrongCredentialException(String message) {
        super(message);
    }
}
