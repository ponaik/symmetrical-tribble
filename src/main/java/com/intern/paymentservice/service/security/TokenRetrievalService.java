package com.intern.paymentservice.service.security;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static com.intern.paymentservice.config.ClientRegistrationConfiguration.SERVICE_ACCOUNT;

@Service
@Slf4j
@NullMarked
public class TokenRetrievalService {

    private final AuthorizedClientServiceOAuth2AuthorizedClientManager clientManager;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final JwtDecoder jwtDecoder;

    @Autowired
    public TokenRetrievalService(AuthorizedClientServiceOAuth2AuthorizedClientManager clientManager, JwtAuthenticationConverter jwtAuthenticationConverter, JwtDecoder jwtDecoder) {
        this.clientManager = clientManager;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.jwtDecoder = jwtDecoder;
    }

    public String getAccessToken() {
        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
                .withClientRegistrationId(SERVICE_ACCOUNT)
                .principal(SERVICE_ACCOUNT)
                .build();
        return Objects.requireNonNull(clientManager.authorize(request)).getAccessToken().getTokenValue();
    }

    public void setAuthenticationInContext() {
        String tokenValue = getAccessToken();
        Jwt jwt = jwtDecoder.decode(tokenValue);
        AbstractAuthenticationToken token = jwtAuthenticationConverter.convert(jwt);
        SecurityContextHolder.getContext().setAuthentication(token);
        log.debug("Set authentication in security context with Bearer token");
    }
}