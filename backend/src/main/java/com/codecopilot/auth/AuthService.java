package com.codecopilot.auth;

import com.codecopilot.auth.dto.AuthResponse;
import com.codecopilot.auth.dto.CurrentUserDto;
import com.codecopilot.auth.dto.LoginRequest;
import com.codecopilot.auth.dto.RefreshRequest;
import com.codecopilot.auth.dto.RegisterRequest;
import com.codecopilot.common.api.ApiResponse;
import com.codecopilot.common.exception.BadRequestException;
import com.codecopilot.common.exception.ForbiddenException;
import com.codecopilot.common.exception.NotFoundException;
import com.codecopilot.config.AppProperties;
import com.codecopilot.security.JwtService;
import com.codecopilot.user.Role;
import com.codecopilot.user.RoleName;
import com.codecopilot.user.RoleRepository;
import com.codecopilot.user.User;
import com.codecopilot.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AppProperties properties;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                       JwtService jwtService, AppProperties properties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.properties = properties;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName() == null ? request.getUsername() : request.getDisplayName());

        Role developer = roleRepository.findByName(RoleName.DEVELOPER)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.DEVELOPER)));
        user.getRoles().add(developer);
        user = userRepository.save(user);

        return issueTokens(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));
        return issueTokens(user);
    }

    public AuthResponse refresh(RefreshRequest request) {
        if (!jwtService.isValidRefreshToken(request.getRefreshToken())) {
            throw new ForbiddenException("Invalid or expired refresh token");
        }
        Long userId = jwtService.extractUserId(request.getRefreshToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return issueTokens(user);
    }

    public CurrentUserDto currentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return new CurrentUserDto(user.getId(), user.getUsername(), user.getEmail(),
                user.getDisplayName(), user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
    }

    private AuthResponse issueTokens(User user) {
        Set<String> roles = user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet());
        return AuthResponse.builder()
                .accessToken(jwtService.createAccessToken(user.getId(), user.getUsername(), roles))
                .refreshToken(jwtService.createRefreshToken(user.getId()))
                .tokenType("Bearer")
                .expiresIn(properties.getSecurity().getJwtExpirationMs() / 1000)
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .displayName(user.getDisplayName())
                        .roles(roles)
                        .build())
                .build();
    }
}