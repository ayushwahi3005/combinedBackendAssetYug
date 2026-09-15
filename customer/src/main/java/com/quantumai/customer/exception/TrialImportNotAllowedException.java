package com.quantumai.customer.exception;

public class TrialImportNotAllowedException extends Exception {

  private static final long serialVersionUID = 1L;

  public TrialImportNotAllowedException(String message) {
    super(message);
  }
}
