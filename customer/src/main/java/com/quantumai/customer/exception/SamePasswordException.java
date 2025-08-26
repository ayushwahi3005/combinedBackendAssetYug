package com.quantumai.customer.exception;

import org.springframework.mail.MailException;

public class SamePasswordException extends RuntimeException  {
    private static final long serialVersionUID = -6432688625962585803L;

    public SamePasswordException(String message) {
        super(message);
    }
}
