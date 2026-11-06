package com.traveldesk.backend.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origin}") String allowedOrigin) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigin));
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );
        configuration.setAllowedHeaders(
                List.of("Authorization", "Content-Type")
        );
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        JwtAuthenticationFilter jwtFilter =
                new JwtAuthenticationFilter(jwtService, userDetailsService);

        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/destinations", "/api/destinations/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/packages", "/api/packages/**").permitAll()
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/register",
                    "/error"
                ).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/destinations").hasAnyRole("ADMIN", "TRAVEL_AGENT")
                .requestMatchers(HttpMethod.PUT, "/api/destinations/**").hasAnyRole("ADMIN", "TRAVEL_AGENT")
                .requestMatchers(HttpMethod.DELETE, "/api/destinations/**").hasAnyRole("ADMIN", "TRAVEL_AGENT")
                .requestMatchers(HttpMethod.POST, "/api/packages").hasAnyRole("ADMIN", "TRAVEL_AGENT")
                .requestMatchers(HttpMethod.PUT, "/api/packages/**").hasAnyRole("ADMIN", "TRAVEL_AGENT")
                .requestMatchers(HttpMethod.DELETE, "/api/packages/**").hasAnyRole("ADMIN", "TRAVEL_AGENT")
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, exception) ->
                    response.sendError(
                        jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED
                    )
                )
            )
            .addFilterBefore(
                jwtFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}