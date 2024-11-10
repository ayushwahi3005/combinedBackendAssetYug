package com.quantumai.customer.dto;

import lombok.Data;

import com.quantumai.customer.entity.Role;

import lombok.Builder;

@Data
@Builder
public class AuthenticationResponseDTO {
	private String token;
	private String role;
}
