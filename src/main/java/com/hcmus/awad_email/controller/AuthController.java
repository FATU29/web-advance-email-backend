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
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok(ApiResponse.success("Signup successful. Please check your email for verification code.", null));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyEmail(@Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = authService.verifyEmail(request);

        // Initialize mock emails for new user
        emailService.initializeMockEmails(response.getUser().getId());

        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", response));
    }

    @PostMapping("/resend-verification-otp")
    public ResponseEntity<ApiResponse<Void>> resendVerificationOtp(@Valid @RequestBody SendOtpRequest request) {
        authService.resendVerificationOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Verification code sent to your email", null));
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

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset code sent to your email", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
    }

    @PostMapping("/send-change-password-otp")
    public ResponseEntity<ApiResponse<Void>> sendChangePasswordOtp(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        authService.sendChangePasswordOtp(userId);
        return ResponseEntity.ok(ApiResponse.success("Verification code sent to your email", null));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        String userId = (String) authentication.getPrincipal();
        authService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }
}

