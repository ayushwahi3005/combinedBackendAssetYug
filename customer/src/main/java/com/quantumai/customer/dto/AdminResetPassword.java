package com.quantumai.customer.dto;

import lombok.Data;

@Data
public class AdminResetPassword {

    private String id;
    private String email;
    private String password;


}
