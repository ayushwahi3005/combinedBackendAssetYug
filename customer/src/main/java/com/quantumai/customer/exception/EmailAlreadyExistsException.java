package com.quantumai.customer.exception;

public class EmailAlreadyExistsException extends Exception{
    private static final long serialVersionUID = -2773714456289200963L;

    public EmailAlreadyExistsException(String message) {
      super(message);
    }
}
