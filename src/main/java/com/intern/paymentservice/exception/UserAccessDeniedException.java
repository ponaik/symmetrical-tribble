package com.intern.paymentservice.exception;

public class UserAccessDeniedException extends RuntimeException {
    public UserAccessDeniedException(long userId) {
        super("User with id " + userId + " does not have access to this resource");
    }
}
