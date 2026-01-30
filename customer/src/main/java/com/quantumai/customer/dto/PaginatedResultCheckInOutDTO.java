package com.quantumai.customer.dto;

import lombok.Data;

import java.util.List;

@Data
public class PaginatedResultCheckInOutDTO<T> {
    private List<T> data;
    private long totalRecords;
    private long totalCheckIn;
    private long totalCheckOut;

    public PaginatedResultCheckInOutDTO(List<T> data, long totalRecords, long totalCheckIn, long totalCheckOut) {
        this.totalCheckIn = totalCheckIn;
        this.totalCheckOut = totalCheckOut;
        this.data = data;
        this.totalRecords = totalRecords;
    }

}
