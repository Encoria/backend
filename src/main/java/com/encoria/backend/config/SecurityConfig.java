package com.encoria.backend.config;


import com.encoria.backend.model.User;
import com.encoria.backend.model.UserRole;
import com.encoria.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Configuration
@EnableWebSecurity
public class SecurityConfig { // okta  - auth0

    // Define the endpoint for profile creation/update
    private static final String PROFILE_SETUP_PATH = "/api/users/profile"; // Adjust as needed
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper(); // For error responses

    // Inject your UserRepository
    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Disable CSRF for stateless APIs
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Configure Authorization Rules
                .authorizeHttpRequests(authz -> authz
                        // Allow access to the profile setup endpoint even if profile is incomplete
                        .requestMatchers(HttpMethod.POST, PROFILE_SETUP_PATH).authenticated() // Or PUT
                        // Define any other public endpoints here (e.g., actuator health)
                        .requestMatchers("/public/**", "/actuator/health").permitAll()
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )

                // 3. Configure Session Management to be Stateless
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 4. Enable OAuth2 Resource Server with JWT support
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                // Use our custom JwtAuthenticationConverter
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )

                // 5. Add our custom filter AFTER the standard BearerTokenAuthenticationFilter
                .addFilterAfter(profileCompletionFilter(), BearerTokenAuthenticationFilter.class);


        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        // Configure it to extract authorities from our DB based on the 'sub' claim
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String externalAuthId = jwt.getSubject(); // 'sub' claim is the standard identifier
            if (externalAuthId == null) {
                return Collections.emptyList();
            }

            // Find the user in your local database using the external ID
            Optional<User> userOptional = userRepository.findByExternalAuthId(externalAuthId);

            // If user exists (and profile is assumed complete by this point), load their roles
            return userOptional.map(user -> {
                // Assuming user.getRole() returns your UserRole entity
                UserRole role = user.getRole();
                if (role != null && role.getCode() != null) {
                    // Create Spring Security authorities (prefix with ROLE_ is standard)
                    return List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_" + role.getCode().toUpperCase()));
                } else {
                    // Handle case where user exists but has no role (assign default? log warning?)
                    // Returning empty list means they are authenticated but have no specific app roles
                    return Collections.<GrantedAuthority>emptyList();
                }
            }).orElseGet(Collections::emptyList); // If user not found in DB, return no authorities
            // (ProfileCompletionFilter should prevent access anyway)
        });

        // Optional: If you want the principal object in SecurityContext to be your User entity
        // instead of the Jwt object, you can customize the Authentication object creation.
        // This is more advanced and often not strictly necessary if you load the user
        // in your service layer based on the authenticated principal's name (which is the 'sub').
        // Example (use with caution, ensure user is loaded efficiently):
        /*
        converter.setPrincipalClaimName("sub"); // Keep 'sub' as the name identifier
        // Custom converter to create the Authentication object
        converter.setJwtAuthenticationConverter(jwt -> {
            Collection<GrantedAuthority> authorities = converter.getJwtGrantedAuthoritiesConverter().convert(jwt);
            String principalClaimValue = jwt.getClaimAsString(converter.getPrincipalClaimName());
            // Load your User entity here based on principalClaimValue (externalAuthId)
            User userPrincipal = userRepository.findByExternalAuthId(principalClaimValue).orElse(null); // Handle null case
            return new JwtAuthenticationToken(jwt, authorities, userPrincipal != null ? userPrincipal.getUsername() : principalClaimValue);
            // Or return new UsernamePasswordAuthenticationToken(userPrincipal, "N/A", authorities); if you prefer
        });
        */

        return converter;
    }


    // Define the ProfileCompletionFilter Bean
    @Bean
    public OncePerRequestFilter profileCompletionFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                // Allow requests to the profile creation/update endpoint itself to pass through
                if (request.getRequestURI().equals(PROFILE_SETUP_PATH) &&
                        (request.getMethod().equalsIgnoreCase("POST") || request.getMethod().equalsIgnoreCase("PUT"))) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // Check only if user is authenticated via JWT
                if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                    String externalAuthId = jwtAuth.getToken().getSubject(); // Assuming 'sub' claim

                    // Check if user exists AND is considered "complete" in your DB
                    // Adapt the logic based on your definition of "complete" (e.g., username != null)
                    boolean profileComplete = userRepository.findByExternalAuthId(externalAuthId)
                            .map(user -> user.getUsername() != null /* && other required fields are not null */)
                            .orElse(false);

                    if (!profileComplete) {
                        // Profile is not complete, reject the request
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        Map<String, String> errorResponse = Map.of(
                                "error", "PROFILE_SETUP_REQUIRED",
                                "message", "User profile setup is incomplete."
                                // Optionally include email if easily available from token claims for the client
                                // "email", jwtAuth.getToken().getClaimAsString("email")
                        );
                        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                        return; // Stop filter chain
                    }
                }

                // If authenticated and profile is complete, or not authenticated (for public endpoints), continue
                filterChain.doFilter(request, response);
            }
        };
    }
}
