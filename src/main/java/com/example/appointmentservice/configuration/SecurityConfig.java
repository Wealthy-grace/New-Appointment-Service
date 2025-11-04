//package com.example.appointmentservice.configuration;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//
//@Configuration
//@EnableWebSecurity
//@EnableMethodSecurity(prePostEnabled = true)  // CRITICAL: Enables @PreAuthorize
//public class SecurityConfig {
//
//    @Autowired
//    private JwtAuthenticationFilter jwtAuthenticationFilter;
//
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(AbstractHttpConfigurer::disable)
//                .sessionManagement(session ->
//                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .authorizeHttpRequests(authz -> authz
//                        // Public endpoints
//                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
//                        .requestMatchers("/api/v1/appointments/confirm-by-token/**").permitAll()
//
//                        // All appointment endpoints require authentication
//                        // Role-based access controlled by @PreAuthorize in controller
//                        .requestMatchers("/api/v1/appointments/**").authenticated()
//
//                        // All other requests require authentication
//                        .anyRequest().authenticated()
//                )
//                // Add JWT filter to extract username and roles from token
//                .addFilterBefore(jwtAuthenticationFilter,
//                        UsernamePasswordAuthenticationFilter.class);
//
//        return http.build();
//    }
//}


// TODO: Add custom user service

package com.example.appointmentservice.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;



// Spring Security Configuration for Appointment Service

//  Integrates with Keycloak for authentication and authorization
    // Security Model:
// Public endpoints: Health checks, token-based appointment confirmation
// Authenticated endpoints: All appointment CRUD operations
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(authz -> authz
                        // Public endpoints
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/v1/appointments/confirm-by-token/**").permitAll()

                        // All appointment endpoints require authentication
                        // Fine-grained authorization is handled by AppointmentSecurityService
                        .requestMatchers("/api/v1/appointments/**").authenticated()

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )

                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

        return http.build();
    }


    // JWT Authentication Converter
    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());

        // Use preferred_username as the principal name (Keycloak default)
        converter.setPrincipalClaimName("preferred_username");

        return converter;
    }

    //  Extracts authorities/roles from Keycloak JWT token
    @Bean
    public Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
        return jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();

            // Extract roles from realm_access claim
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) realmAccess.get("roles");

                // Map each role to an authority with correct prefix handling
                authorities = roles.stream()
                        .map(this::mapRoleToAuthority)
                        .collect(Collectors.toList());
            }

            return authorities;
        };
    }


    // Map a Keycloak role to a Spring Security GrantedAuthority
    // ADMIN" → "ROLE_ADMIN" (adds prefix
    private GrantedAuthority mapRoleToAuthority(String role) {
        // If role already starts with "ROLE_", use it as-is
        if (role.startsWith("ROLE_")) {
            return new SimpleGrantedAuthority(role);
        }

        // Standard Keycloak internal roles - use as-is without ROLE_ prefix
        if (role.startsWith("default-roles-") ||
                role.equals("offline_access") ||
                role.equals("uma_authorization")) {
            return new SimpleGrantedAuthority(role);
        }

        // Custom application roles - add ROLE_ prefix
        return new SimpleGrantedAuthority("ROLE_" + role);
    }
}












