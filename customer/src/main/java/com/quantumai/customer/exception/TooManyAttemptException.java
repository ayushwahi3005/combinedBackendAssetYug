package com.quantumai.customer.exception;

public class TooManyAttemptException extends Exception {

    /** */
    private static final long serialVersionUID = 9205348152463714345L;

    public TooManyAttemptException(String message) {
        super(message);
    }
}
