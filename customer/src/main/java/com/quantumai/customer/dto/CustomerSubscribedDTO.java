package com.quantumai.customer.dto;

import java.time.LocalDate;

public class CustomerSubscribedDTO {
	private String email;
	private boolean subscription;
	private LocalDate lastDate;
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public boolean isSubscription() {
		return subscription;
	}
	public void setSubscription(boolean subscription) {
		this.subscription = subscription;
	}
	public LocalDate getLastDate() {
		return lastDate;
	}
	public void setLastDate(LocalDate lastDate) {
		this.lastDate = lastDate;
	}
}
