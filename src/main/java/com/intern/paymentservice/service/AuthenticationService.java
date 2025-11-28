package com.intern.paymentservice.service;

public interface AuthenticationService {
    String USER = "ROLE_user";
    String ADMIN = "ROLE_admin";

    long getInternalId();

    boolean isAdmin();

    boolean isUser();
}
