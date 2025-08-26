package com.quantumai.customer.exception;

public class NameColumnMissingException extends Exception {
    private static final long serialVersionUID = 8721336965955527019L;

    public NameColumnMissingException(String msg) {
      super(msg);
    }
}
