package com.traveldesk.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class JwtServiceTest {

    private static final String TEST_SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final long EXPIRATION_MILLIS = 3_600_000;

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, EXPIRATION_MILLIS);
        userDetails = User.withUsername("traveler@example.com")
                .password("unused")
                .authorities("ROLE_CUSTOMER")
                .build();
    }

    @Test
    void shouldGenerateTokenWithUserEmailAsSubject() {
        Instant beforeGeneration = Instant.now();
        String token = jwtService.generateToken(userDetails);
        Instant afterGeneration = Instant.now();

        assertEquals("traveler@example.com", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, userDetails));

        Date expiration = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(
                        Decoders.BASE64.decode(TEST_SECRET)))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
        assertTrue(expiration.toInstant().isAfter(
                beforeGeneration.plusMillis(EXPIRATION_MILLIS - 1_000)));
        assertTrue(expiration.toInstant().isBefore(
                afterGeneration.plusMillis(EXPIRATION_MILLIS + 1_000)));
    }

    @Test
    void shouldValidateTokenForMatchingUser() {
        String token = jwtService.generateToken(userDetails);

        assertTrue(jwtService.isTokenValid(token, userDetails));

        UserDetails otherUser = User.withUsername("other@example.com")
                .password("unused")
                .authorities("ROLE_CUSTOMER")
                .build();
        assertFalse(jwtService.isTokenValid(token, otherUser));
    }

    @Test
    void shouldRejectInvalidToken() {
        assertFalse(jwtService.isTokenValid("not-a-jwt", userDetails));
    }

    @Test
    void shouldRejectExpiredToken() {
        String token = expiredToken();

        assertFalse(jwtService.isTokenValid(token, userDetails));
        assertThrows(
                ExpiredJwtException.class,
                () -> jwtService.extractUsername(token)
        );
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
