package com.quantumai.customer.exception;

public class LocationAlreadyPresentException extends Exception {
    private static final long serialVersionUID = 8721336965955527019L;
    public LocationAlreadyPresentException(String msg) {
        super(msg);
      }
}
