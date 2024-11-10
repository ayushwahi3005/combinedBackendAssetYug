package com.quantumai.customer.entity;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="CustomerSubscribe")
public class CustomerSubscribed {
	@Id
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
