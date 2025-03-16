package com.quantumai.customer.dto;

import java.util.List;

public class PaginatedResultDTO<T> {
  private List<T> data;
  private long totalRecords;

  public PaginatedResultDTO(List<T> data, long totalRecords) {
    this.data = data;
    this.totalRecords = totalRecords;
  }

  // Getters and setters
  public List<T> getData() {
    return data;
  }

  public void setData(List<T> data) {
    this.data = data;
  }

  public long getTotalRecords() {
    return totalRecords;
  }

  public void setTotalRecords(long totalRecords) {
    this.totalRecords = totalRecords;
  }
}
