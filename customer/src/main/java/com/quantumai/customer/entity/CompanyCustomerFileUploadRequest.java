package com.quantumai.customer.entity;

import org.springframework.web.multipart.MultipartFile;

public class CompanyCustomerFileUploadRequest {
  private MultipartFile file;
  private String companyId;

  private String email;

  public MultipartFile getFile() {
    return file;
  }

  public void setFile(MultipartFile file) {
    this.file = file;
  }

  public String getCompanyId() {
    return companyId;
  }

  public void setCompanyId(String companyId) {
    this.companyId = companyId;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }
}
