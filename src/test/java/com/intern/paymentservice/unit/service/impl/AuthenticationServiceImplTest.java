package com.intern.paymentservice.unit.service.impl;

import com.intern.paymentservice.service.impl.AuthenticationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AuthenticationServiceImplTest {

    private static final String ROLE_ADMIN = "ROLE_admin";
    private static final String ROLE_USER = "ROLE_user";

    @Mock
    SecurityContext securityContext;

    @Mock
    Authentication authentication;

    @Mock
    Jwt jwt;

    @InjectMocks
    AuthenticationServiceImpl service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getInternalId_validJwt_returnsId() {
        // given
        Long expectedId = 12345L;
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(jwt);
        given(jwt.getClaims()).willReturn(Map.of("internal_id", expectedId));

        // action
        long actualId = service.getInternalId();

        // assertThat
        assertThat(actualId).isEqualTo(expectedId);
    }

    @Test
    void getInternalId_noAuthentication_throwsIllegalState() {
        // given
        given(securityContext.getAuthentication()).willReturn(null);

        // action & assertThat
        assertThatThrownBy(() -> service.getInternalId())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Authentication is missing");
    }

    @Test
    void getInternalId_principalNotJwtOrNull_throwsIllegalState() {
        // given
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(null);

        // action & assertThat
        assertThatThrownBy(() -> service.getInternalId())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Authentication principal is missing");
    }

    @Test
    void getInternalId_missingClaim_throwsIllegalState() {
        // given
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(jwt);
        // Empty claims map
        given(jwt.getClaims()).willReturn(Map.of());

        // action & assertThat
        assertThatThrownBy(() -> service.getInternalId())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Internal id is missing");
    }

    @Test
    void isAdmin_hasAdminAuthority_returnsTrue() {
        // given
        // Assuming AuthenticationService.ADMIN corresponds to "ROLE_ADMIN"
        setupAuthority(ROLE_ADMIN);

        // action
        boolean result = service.isAdmin();

        // assertThat
        assertThat(result).isTrue();
    }

    @Test
    void isAdmin_hasUserAuthority_returnsFalse() {
        // given
        setupAuthority(ROLE_USER);

        // action
        boolean result = service.isAdmin();

        // assertThat
        assertThat(result).isFalse();
    }

    @Test
    void isAdmin_noAuthentication_returnsFalse() {
        // given
        given(securityContext.getAuthentication()).willReturn(null);

        // action
        boolean result = service.isAdmin();

        // assertThat
        assertThat(result).isFalse();
    }

    @Test
    void isUser_hasUserAuthority_returnsTrue() {
        // given
        setupAuthority(ROLE_USER);

        // action
        boolean result = service.isUser();

        // assertThat
        assertThat(result).isTrue();
    }

    @Test
    void isUser_hasAdminAuthority_returnsFalse() {
        // given
        setupAuthority(ROLE_ADMIN);

        // action
        boolean result = service.isUser();

        // assertThat
        assertThat(result).isFalse();
    }

    @Test
    void setBrokerAuthenticationInContext_setsAdminToken() {
        // given
        // We need to ensure getContext() returns our mock, which calls setAuthentication
        // This is already handled by @BeforeEach SecurityContextHolder.setContext(securityContext)

        // action
        service.setBrokerAuthenticationInContext();

        // assertThat
        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(securityContext).setAuthentication(captor.capture());

        Authentication setAuth = captor.getValue();
        assertThat(setAuth.getName()).isEqualTo("kafka-system");
        
        // Verify it has the ADMIN authority
        boolean hasAdmin = setAuth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(ROLE_ADMIN));
        assertThat(hasAdmin).isTrue();
    }

    // --- Helper ---
    private void setupAuthority(String role) {
        given(securityContext.getAuthentication()).willReturn(authentication);
        GrantedAuthority authority = new SimpleGrantedAuthority(role);
        // Generic mocking for wildcard return types can be tricky, casting helps or doReturn
        doReturn(List.of(authority)).when(authentication).getAuthorities();
    }
}