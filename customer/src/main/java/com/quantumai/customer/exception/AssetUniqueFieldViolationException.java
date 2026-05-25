package com.quantumai.customer.exception;

import com.quantumai.customer.dto.AssetUniqueFieldValidationDTO;
import java.util.Map;
import java.util.List;

public class AssetUniqueFieldViolationException extends Exception {

  private static final long serialVersionUID = 1L;
  private AssetUniqueFieldValidationDTO validationDetails;
  private Map<String, List<String>> conflicts;
  private String fieldName;

  public AssetUniqueFieldViolationException(String msg) {
    super(msg);
  }

  public AssetUniqueFieldViolationException(String msg, AssetUniqueFieldValidationDTO validationDetails) {
    super(msg);
    this.validationDetails = validationDetails;
    if (validationDetails != null) {
      this.conflicts = validationDetails.getConflicts();
    }
  }

  public AssetUniqueFieldViolationException(String msg, String fieldName, Map<String, List<String>> conflicts) {
    super(msg);

    this.fieldName = fieldName;
    this.conflicts = conflicts;
  }

  public AssetUniqueFieldValidationDTO getValidationDetails() {
    return validationDetails;
  }

  public void setValidationDetails(AssetUniqueFieldValidationDTO validationDetails) {
    this.validationDetails = validationDetails;
  }

  public Map<String, List<String>> getConflicts() {
    return conflicts;
  }

  public void setConflicts(Map<String, List<String>> conflicts) {
    this.conflicts = conflicts;
  }

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }
}
