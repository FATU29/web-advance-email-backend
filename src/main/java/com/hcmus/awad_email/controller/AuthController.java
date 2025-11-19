package com.hcmus.awad_email.controller;

import com.hcmus.awad_email.dto.auth.*;
import com.hcmus.awad_email.dto.common.ApiResponse;
import com.hcmus.awad_email.service.AuthService;
import com.hcmus.awad_email.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private EmailService emailService;
    
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody SignupRequest request) {
        AuthResponse response = authService.signup(request);
        
        // Initialize mock emails for new user
        emailService.initializeMockEmails(response.getUser().getId());
        
        return ResponseEntity.ok(ApiResponse.success("User registered successfully", response));
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
    
    /**
     * Google OAuth 2.0 Authorization Code Flow
     * Frontend sends the authorization code received from Google OAuth redirect
     */
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleAuthCodeLogin(@Valid @RequestBody GoogleAuthCodeRequest request) {
        AuthResponse response = authService.googleAuthCodeLogin(request);

        // Check if this is a new user and initialize mock emails
        try {
            emailService.initializeMockEmails(response.getUser().getId());
        } catch (Exception e) {
            // Mailboxes might already exist, ignore
        }

        return ResponseEntity.ok(ApiResponse.success("Google authentication successful", response));
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            Authentication authentication,
            @RequestBody(required = false) RefreshTokenRequest request) {
        String userId = (String) authentication.getPrincipal();
        String refreshToken = request != null ? request.getRefreshToken() : null;
        authService.logout(userId, refreshToken);
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse.UserInfo>> getCurrentUser(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        AuthResponse.UserInfo userInfo = authService.getUserInfo(userId);
        return ResponseEntity.ok(ApiResponse.success("User info retrieved successfully", userInfo));
    }

    @PostMapping("/introspect")
    public ResponseEntity<ApiResponse<TokenIntrospectResponse>> introspectToken(@Valid @RequestBody TokenIntrospectRequest request) {
        TokenIntrospectResponse response = authService.introspectToken(request);
        String message = response.isValid() ? "Token is valid" : "Token is invalid";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }
}

