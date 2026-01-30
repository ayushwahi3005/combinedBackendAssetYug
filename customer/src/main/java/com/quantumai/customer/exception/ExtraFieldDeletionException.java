package com.quantumai.customer.exception;

public class ExtraFieldDeletionException extends Exception {

    /** */
    private static final long serialVersionUID = -2123476445614852319L;
    private Long count;

    public ExtraFieldDeletionException(String msg,Long count) {
        super(msg);
        this.count = count;
        // TODO Auto-generated constructor stub
    }
    public Long getCount() {
        return count;
    }
}
