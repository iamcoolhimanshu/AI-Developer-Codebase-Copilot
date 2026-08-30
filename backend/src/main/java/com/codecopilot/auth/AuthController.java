package com.codecopilot.auth;

import com.codecopilot.auth.dto.AuthResponse;
import com.codecopilot.auth.dto.CurrentUserDto;
import com.codecopilot.auth.dto.LoginRequest;
import com.codecopilot.auth.dto.RefreshRequest;
import com.codecopilot.auth.dto.RegisterRequest;
import com.codecopilot.common.api.ApiResponse;
import com.codecopilot.common.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.ok("Registered", authService.register(request)));
	}

	@PostMapping("/login")
	public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.ok(authService.login(request));
	}

	@PostMapping("/refresh")
	public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
		return ApiResponse.ok(authService.refresh(request));
	}

	@GetMapping("/me")
	public ApiResponse<CurrentUserDto> me() {
		return ApiResponse.ok(authService.currentUser(SecurityUtils.currentUserId()));
	}
}