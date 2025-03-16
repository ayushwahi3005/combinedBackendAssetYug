package com.quantumai.customer.exception;

public class NoEmailFoundException extends Exception {
  private static final long serialVersionUID = 8721336965955527019L;

  public NoEmailFoundException(String msg) {
    super(msg);
  }
}
