package com.hcmus.awad_email.controller;

import com.hcmus.awad_email.dto.auth.*;
import com.hcmus.awad_email.dto.common.ApiResponse;
import com.hcmus.awad_email.security.JwtTokenProvider;
import com.hcmus.awad_email.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:None}")
    private String cookieSameSite;

    /**
     * Sets the refresh token as an HttpOnly, Secure cookie.
     * This prevents XSS attacks from accessing the refresh token.
     */
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        int maxAgeSeconds = (int) (tokenProvider.getRefreshTokenExpirationMs() / 1000);

        // Build cookie with security attributes
        StringBuilder cookieValue = new StringBuilder();
        cookieValue.append(REFRESH_TOKEN_COOKIE_NAME).append("=").append(refreshToken);
        cookieValue.append("; Max-Age=").append(maxAgeSeconds);
        cookieValue.append("; Path=/api/auth");
        cookieValue.append("; HttpOnly");
        if (cookieSecure) {
            cookieValue.append("; Secure");
        }
        cookieValue.append("; SameSite=").append(cookieSameSite);

        response.addHeader("Set-Cookie", cookieValue.toString());
    }

    /**
     * Clears the refresh token cookie on logout.
     */
    private void clearRefreshTokenCookie(HttpServletResponse response) {
        StringBuilder cookieValue = new StringBuilder();
        cookieValue.append(REFRESH_TOKEN_COOKIE_NAME).append("=");
        cookieValue.append("; Max-Age=0");
        cookieValue.append("; Path=/api/auth");
        cookieValue.append("; HttpOnly");
        if (cookieSecure) {
            cookieValue.append("; Secure");
        }
        cookieValue.append("; SameSite=").append(cookieSameSite);

        response.addHeader("Set-Cookie", cookieValue.toString());
    }

    /**
     * Extracts refresh token from HttpOnly cookie.
     */
    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        log.info("🔐 Signup attempt for email: {}", request.getEmail());
        authService.signup(request);
        log.info("✅ Signup successful for email: {}", request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Signup successful. Please check your email for verification code.", null));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyEmail(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletResponse response) {
        log.info("🔐 Email verification attempt for: {}", request.getEmail());
        AuthResponse authResponse = authService.verifyEmail(request);

        // Set refresh token in HttpOnly cookie
        setRefreshTokenCookie(response, authResponse.getRefreshToken());

        // Remove refresh token from response body (security best practice)
        authResponse.setRefreshToken(null);

        log.info("✅ Email verified successfully for: {}", request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", authResponse));
    }

    @PostMapping("/resend-verification-otp")
    public ResponseEntity<ApiResponse<Void>> resendVerificationOtp(@Valid @RequestBody SendOtpRequest request) {
        log.info("🔐 Resend verification OTP for: {}", request.getEmail());
        authService.resendVerificationOtp(request);
        log.info("✅ Verification OTP sent to: {}", request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Verification code sent to your email", null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        log.info("🔐 Login attempt for email: {}", request.getEmail());
        AuthResponse authResponse = authService.login(request);

        // Set refresh token in HttpOnly cookie
        setRefreshTokenCookie(response, authResponse.getRefreshToken());

        // Remove refresh token from response body (security best practice)
        authResponse.setRefreshToken(null);

        log.info("✅ Login successful for email: {}", request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    /**
     * Google OAuth 2.0 Authorization Code Flow
     * Frontend sends the authorization code received from Google OAuth redirect
     */
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleAuthCodeLogin(
            @Valid @RequestBody GoogleAuthCodeRequest request,
            HttpServletResponse response) {
        log.info("🔐 Google OAuth login attempt");
        AuthResponse authResponse = authService.googleAuthCodeLogin(request);

        // Set refresh token in HttpOnly cookie
        setRefreshTokenCookie(response, authResponse.getRefreshToken());

        // Remove refresh token from response body (security best practice)
        authResponse.setRefreshToken(null);

        log.info("✅ Google authentication successful for user: {}", authResponse.getUser().getEmail());
        return ResponseEntity.ok(ApiResponse.success("Google authentication successful", authResponse));
    }

    /**
     * Refresh access token using the refresh token from HttpOnly cookie.
     * The refresh token is automatically sent by the browser in the cookie.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        log.info("🔐 Token refresh attempt");

        // Get refresh token from HttpOnly cookie
        String refreshToken = getRefreshTokenFromCookie(request);
        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn("⚠️ No refresh token found in cookie");
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("No refresh token provided"));
        }

        AuthResponse authResponse = authService.refreshToken(refreshToken);

        // Optionally rotate refresh token (set new cookie)
        // For now, we keep the same refresh token

        // Remove refresh token from response body
        authResponse.setRefreshToken(null);

        log.info("✅ Token refreshed successfully");
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", authResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        String userId = (String) authentication.getPrincipal();
        log.info("🔐 Logout request from user: {}", userId);

        // Get refresh token from cookie
        String refreshToken = getRefreshTokenFromCookie(request);

        // Revoke refresh token server-side
        authService.logout(userId, refreshToken);

        // Clear the refresh token cookie
        clearRefreshTokenCookie(response);

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

