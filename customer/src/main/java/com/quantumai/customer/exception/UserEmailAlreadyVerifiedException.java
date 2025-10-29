package com.quantumai.customer.exception;

public class UserEmailAlreadyVerifiedException extends Exception{

    private static final long serialVersionUID = 9205348152463714145L;
    public UserEmailAlreadyVerifiedException(String message) {
        super(message);
    }
}
