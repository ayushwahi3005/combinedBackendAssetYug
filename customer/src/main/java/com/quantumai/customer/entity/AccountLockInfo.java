package com.quantumai.customer.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document
public class AccountLockInfo {

	@Id
	private String id;
	private String customerEmail;
	private Boolean lockedStatus;
	private Integer incorrectAttemptCount;
}
