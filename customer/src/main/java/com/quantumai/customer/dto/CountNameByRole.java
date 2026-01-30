package com.quantumai.customer.dto;

import lombok.Data;

import java.util.List;

@Data
public class CountNameByRole {
    private Long totalCount;
    private List<UserNameEmail> users;
}
