package com.quantumai.customer.exception;

public class WrongAdminEmailException extends Exception {

    private static final long serialVersionUID = -9046302148486644676L;

    public WrongAdminEmailException(String message){
        super(message);
    }
}
