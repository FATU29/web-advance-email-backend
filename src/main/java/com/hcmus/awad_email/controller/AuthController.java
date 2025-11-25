package com.hcmus.awad_email.controller;

import com.hcmus.awad_email.dto.auth.*;
import com.hcmus.awad_email.dto.common.ApiResponse;
import com.hcmus.awad_email.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;
    
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        log.info("🔐 Signup attempt for email: {}", request.getEmail());
        authService.signup(request);
        log.info("✅ Signup successful for email: {}", request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Signup successful. Please check your email for verification code.", null));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyEmail(@Valid @RequestBody VerifyOtpRequest request) {
        log.info("🔐 Email verification attempt for: {}", request.getEmail());
        AuthResponse response = authService.verifyEmail(request);
        log.info("✅ Email verified successfully for: {}", request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", response));
    }

    @PostMapping("/resend-verification-otp")
    public ResponseEntity<ApiResponse<Void>> resendVerificationOtp(@Valid @RequestBody SendOtpRequest request) {
        log.info("🔐 Resend verification OTP for: {}", request.getEmail());
        authService.resendVerificationOtp(request);
        log.info("✅ Verification OTP sent to: {}", request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Verification code sent to your email", null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("🔐 Login attempt for email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        log.info("✅ Login successful for email: {}", request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
    
    /**
     * Google OAuth 2.0 Authorization Code Flow
     * Frontend sends the authorization code received from Google OAuth redirect
     */
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleAuthCodeLogin(@Valid @RequestBody GoogleAuthCodeRequest request) {
        log.info("🔐 Google OAuth login attempt");
        AuthResponse response = authService.googleAuthCodeLogin(request);
        log.info("✅ Google authentication successful for user: {}", response.getUser().getEmail());
        return ResponseEntity.ok(ApiResponse.success("Google authentication successful", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("🔐 Token refresh attempt");
        AuthResponse response = authService.refreshToken(request);
        log.info("✅ Token refreshed successfully");
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            Authentication authentication,
            @RequestBody(required = false) RefreshTokenRequest request) {
        String userId = (String) authentication.getPrincipal();
        log.info("🔐 Logout request from user: {}", userId);
        String refreshToken = request != null ? request.getRefreshToken() : null;
        authService.logout(userId, refreshToken);
        log.info("✅ Logout successful for user: {}", userId);
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse.UserInfo>> getCurrentUser(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        log.debug("📋 Get current user info for: {}", userId);
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

