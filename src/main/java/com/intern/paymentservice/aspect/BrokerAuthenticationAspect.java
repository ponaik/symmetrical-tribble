package com.intern.paymentservice.aspect;

import com.intern.paymentservice.service.AuthenticationService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class BrokerAuthenticationAspect {

    private final AuthenticationService authenticationService;

    @Autowired
    public BrokerAuthenticationAspect(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Around("@annotation(com.intern.paymentservice.aspect.annotation.BrokerAuthentication)")
    public Object authenticateBroker(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            log.trace("Setting broker authentication context for method: {}", joinPoint.getSignature());
            authenticationService.setBrokerAuthenticationInContext();
            
            return joinPoint.proceed();
        } finally {
            SecurityContextHolder.clearContext();
            log.trace("Cleared security context");
        }
    }
}