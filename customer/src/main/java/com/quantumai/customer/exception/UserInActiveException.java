package com.quantumai.customer.exception;

public class UserInActiveException extends Exception {

  /** */
  private static final long serialVersionUID = 9205348152463714345L;

  public UserInActiveException(String message) {
    super(message);
  }
}
