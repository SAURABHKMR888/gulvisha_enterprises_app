package com.gulvisha.backend.config;

import com.gulvisha.backend.security.JwtAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/health").permitAll()
                        // Public website endpoints: submit enquiries/quote requests, read services
                        .requestMatchers(HttpMethod.POST, "/api/enquiries", "/api/quote-requests").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/services").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/admin/**").hasAuthority("PERMISSION_enquiry:view")
                        .requestMatchers(HttpMethod.PATCH, "/api/admin/**").hasAuthority("PERMISSION_enquiry:update")
                        .requestMatchers("/api/portal/**").hasAuthority("PERMISSION_project:view")
                        .requestMatchers("/api/organizations/current").hasAuthority("PERMISSION_organization:settings")
                        .requestMatchers("/api/users/**").hasAuthority("PERMISSION_user:manage")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
