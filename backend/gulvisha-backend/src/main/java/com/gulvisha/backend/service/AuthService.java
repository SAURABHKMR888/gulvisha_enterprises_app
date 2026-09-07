package com.gulvisha.backend.service;

import com.gulvisha.backend.dto.AuthResponse;
import com.gulvisha.backend.dto.LoginRequest;
import com.gulvisha.backend.security.JwtService;
import com.gulvisha.backend.security.Permission;
import com.gulvisha.backend.security.RolePermission;
import com.gulvisha.backend.user.User;
import com.gulvisha.backend.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalArgumentException("Account is not active");
        }

        List<String> permissions = RolePermission.getPermissionsForRole(user.getRole())
                .stream()
                .map(Permission::getPermission)
                .toList();

        String token = jwtService.generateToken(
                user.getUsername(), permissions, user.getOrganizationId(), user.getClientId(), user.getRole().name());

        return new AuthResponse(token, user.getUsername(), List.of(user.getRole().name()), permissions, user.getClientId());
    }
}
