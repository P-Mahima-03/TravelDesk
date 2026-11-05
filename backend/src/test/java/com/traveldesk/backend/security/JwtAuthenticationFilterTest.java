package com.traveldesk.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class JwtAuthenticationFilterTest {

    private static final String TEST_SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private JwtService jwtService;
    private CustomUserDetailsService userDetailsService;
    private JwtAuthenticationFilter filter;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, 3_600_000);
        userDetailsService =
                org.mockito.Mockito.mock(CustomUserDetailsService.class);
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        userDetails = User.withUsername("traveler@example.com")
                .password("unused")
                .authorities("ROLE_CUSTOMER")
                .build();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateRequestWithValidBearerToken()
            throws ServletException, IOException {
        String token = jwtService.generateToken(userDetails);
        when(userDetailsService.loadUserByUsername(userDetails.getUsername()))
                .thenReturn(userDetails);
        MockHttpServletRequest request = requestWithToken(token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(userDetails, authentication.getPrincipal());
        assertEquals(200, response.getStatus());
        verify(userDetailsService).loadUserByUsername(
                "traveler@example.com");
    }

    @Test
    void shouldRejectInvalidBearerToken()
            throws ServletException, IOException {
        MockHttpServletResponse response = processToken("not-a-jwt");

        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldRejectExpiredBearerToken()
            throws ServletException, IOException {
        MockHttpServletResponse response = processToken(expiredToken());

        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldRejectTokenForUnknownUser()
            throws ServletException, IOException {
        String token = jwtService.generateToken(userDetails);
        when(userDetailsService.loadUserByUsername(userDetails.getUsername()))
                .thenThrow(new UsernameNotFoundException("not found"));

        MockHttpServletResponse response = processToken(token);

        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private MockHttpServletResponse processToken(String token)
            throws ServletException, IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(
                requestWithToken(token),
                response,
                new MockFilterChain()
        );
        return response;
    }

    private MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private String expiredToken() {
        SecretKey key =
                Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(Date.from(now.minusSeconds(60)))
                .expiration(Date.from(now.minusSeconds(30)))
                .signWith(key)
                .compact();
    }
}
