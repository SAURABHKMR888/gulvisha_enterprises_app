package com.gulvisha.backend.user;

import com.gulvisha.backend.dto.PasswordResetRequest;
import com.gulvisha.backend.dto.UserCreateRequest;
import com.gulvisha.backend.dto.UserResponse;
import com.gulvisha.backend.dto.UserUpdateRequest;
import com.gulvisha.backend.security.Role;
import com.gulvisha.backend.security.UserContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Tenant-scoped user management.
 * All operations are restricted to the caller's organization (UserContext),
 * so an organization admin can only manage users of their own organization.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponse> list() {
        return userRepository.findAllByOrganizationIdOrderByCreatedAtDesc(currentOrgId()).stream()
                .map(UserResponse::from)
                .toList();
    }

    public UserResponse create(UserCreateRequest request) {
        Role role = parseRole(request.role());

        // CLIENT users require a client-record linkage (portal) — not supported by this API yet.
        if (role == Role.CLIENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "CLIENT users are provisioned with a client record and cannot be created here yet");
        }
        // Only the platform itself may mint PLATFORM_ADMIN accounts.
        if (role == Role.PLATFORM_ADMIN && !isPlatformAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only a PLATFORM_ADMIN can create PLATFORM_ADMIN users");
        }

        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        User user = new User(currentOrgId(), request.username(), request.email(),
                passwordEncoder.encode(request.password()), request.fullName(), role);
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse update(UUID id, UserUpdateRequest request) {
        User user = loadOwned(id);
        Role role = parseRole(request.role());
        if (role == Role.PLATFORM_ADMIN && !isPlatformAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only a PLATFORM_ADMIN can grant the PLATFORM_ADMIN role");
        }
        if (role == Role.CLIENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CLIENT role cannot be assigned here");
        }
        userRepository.findByEmail(request.email())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists"); });

        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setRole(role);
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse resetPassword(UUID id, PasswordResetRequest request) {
        User user = loadOwned(id);
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse updateStatus(UUID id, String status) {
        User user = loadOwned(id);
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be ACTIVE or INACTIVE");
        }
        if (user.getUsername().equals(currentUserUsername()) && "INACTIVE".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot deactivate your own account");
        }
        user.setStatus(status);
        return UserResponse.from(userRepository.save(user));
    }

    private User loadOwned(UUID id) {
        return userRepository.findByIdAndOrganizationId(id, currentOrgId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Role parseRole(String role) {
        try {
            return Role.valueOf(role);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown role: " + role);
        }
    }

    private boolean isPlatformAdmin() {
        return "PLATFORM_ADMIN".equals(currentUserRole());
    }

    private String currentUserUsername() {
        UserContext.CurrentUser current = UserContext.get();
        return current == null ? null : current.username();
    }

    private String currentUserRole() {
        UserContext.CurrentUser current = UserContext.get();
        return current == null ? null : current.role();
    }

    private java.util.UUID currentOrgId() {
        UserContext.CurrentUser current = UserContext.get();
        if (current == null || current.organizationId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication context missing");
        }
        return current.organizationId();
    }
}
