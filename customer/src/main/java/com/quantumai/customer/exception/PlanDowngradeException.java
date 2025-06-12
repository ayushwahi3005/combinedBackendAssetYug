package com.quantumai.customer.exception;

public class PlanDowngradeException extends Exception {

  private static final long serialVersionUID = 1135898947336259291L;

  public PlanDowngradeException(String msg) {
    super(msg);
  }
}
