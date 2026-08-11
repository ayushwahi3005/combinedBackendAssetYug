package com.quantumai.customer.exception;

public class FirebaseServiceException extends RuntimeException {
    public FirebaseServiceException(String message) {
        super(message);
    }

    public FirebaseServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
