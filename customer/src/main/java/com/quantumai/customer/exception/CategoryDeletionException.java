package com.quantumai.customer.exception;

public class CategoryDeletionException extends Exception {

  private static final long serialVersionUID = 1L;
  private final long count;
  private final String entityLabel;

  public CategoryDeletionException(String message, long count, String entityLabel) {
    super(message);
    this.count = count;
    this.entityLabel = entityLabel;
  }

  public long getCount() {
    return count;
  }

  public String getEntityLabel() {
    return entityLabel;
  }
}
