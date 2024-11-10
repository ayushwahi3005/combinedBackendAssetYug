package com.quantumai.customer.dto;

import lombok.Data;

@Data
public class AccountLockInfoDTO {
	private String id;
	private String customerEmail;
	private Boolean lockedStatus;
	private Integer incorrectAttemptCount;
}
