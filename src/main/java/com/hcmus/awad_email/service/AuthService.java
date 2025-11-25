package com.hcmus.awad_email.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.hcmus.awad_email.dto.auth.*;
import com.hcmus.awad_email.exception.BadRequestException;
import com.hcmus.awad_email.exception.UnauthorizedException;
import com.hcmus.awad_email.model.RefreshToken;
import com.hcmus.awad_email.model.User;
import com.hcmus.awad_email.repository.RefreshTokenRepository;
import com.hcmus.awad_email.repository.UserRepository;
import com.hcmus.awad_email.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OtpService otpService;

    @Autowired
    private GmailService gmailService;

    @Value("${app.google.client-id}")
    private String googleClientId;

    @Value("${app.google.client-secret}")
    private String googleClientSecret;

    @Value("${app.google.redirect-uri}")
    private String googleRedirectUri;
    
    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .authProvider(User.AuthProvider.EMAIL)
                .enabled(false) // Disabled until email is verified
                .verified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        user = userRepository.save(user);

        // Send OTP for email verification
        otpService.generateAndSendOtp(user.getEmail(), user.getName(), com.hcmus.awad_email.model.Otp.OtpType.SIGNUP);
    }

    @Transactional
    public AuthResponse verifyEmail(VerifyOtpRequest request) {
        // Verify OTP
        otpService.verifyOtp(request.getEmail(), request.getCode(), com.hcmus.awad_email.model.Otp.OtpType.SIGNUP);

        // Find user and enable account
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));

        user.setEnabled(true);
        user.setVerified(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return generateAuthResponse(user);
    }

    @Transactional
    public void resendVerificationOtp(SendOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (user.isVerified()) {
            throw new BadRequestException("Email is already verified");
        }

        otpService.generateAndSendOtp(user.getEmail(), user.getName(), com.hcmus.awad_email.model.Otp.OtpType.SIGNUP);
    }
    
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // Check if user has password (EMAIL or BOTH auth provider)
        if (user.getPassword() == null) {
            throw new BadRequestException("Please use Google to login");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!user.isEnabled()) {
            throw new UnauthorizedException("Account is not verified. Please verify your email.");
        }

        return generateAuthResponse(user);
    }
    
    /**
     * Google OAuth 2.0 Authorization Code Flow
     * This method exchanges the authorization code for tokens and authenticates the user
     */
    @Transactional
    public AuthResponse googleAuthCodeLogin(GoogleAuthCodeRequest request) {
        try {
            // Exchange authorization code for tokens using configured redirect URI
            GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    "https://oauth2.googleapis.com/token",
                    googleClientId,
                    googleClientSecret,
                    request.getCode(),
                    googleRedirectUri // Use configured redirect URI from application.yml
            ).execute();

            // Verify and extract user info from ID token
            GoogleIdToken idToken = tokenResponse.parseIdToken();
            if (idToken == null) {
                throw new UnauthorizedException("Invalid Google token response");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String googleId = payload.getSubject();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            // Find or create user
            User user = userRepository.findByGoogleId(googleId)
                    .orElseGet(() -> {
                        // Check if user exists with same email
                        return userRepository.findByEmail(email)
                                .map(existingUser -> {
                                    // Link Google account to existing user
                                    existingUser.setGoogleId(googleId);
                                    // If user has password, set to BOTH, otherwise GOOGLE
                                    if (existingUser.getPassword() != null) {
                                        existingUser.setAuthProvider(User.AuthProvider.BOTH);
                                    } else {
                                        existingUser.setAuthProvider(User.AuthProvider.GOOGLE);
                                    }
                                    existingUser.setProfilePicture(pictureUrl);
                                    // Google login verifies the email, so enable the account
                                    existingUser.setVerified(true);
                                    existingUser.setEnabled(true);
                                    existingUser.setUpdatedAt(LocalDateTime.now());

                                    // Delete any pending OTPs for this email since Google verified it
                                    otpService.deleteOtp(email);

                                    return userRepository.save(existingUser);
                                })
                                .orElseGet(() -> {
                                    // Create new user
                                    User newUser = User.builder()
                                            .email(email)
                                            .name(name)
                                            .googleId(googleId)
                                            .profilePicture(pictureUrl)
                                            .authProvider(User.AuthProvider.GOOGLE)
                                            .enabled(true)
                                            .verified(true) // Google accounts are pre-verified
                                            .createdAt(LocalDateTime.now())
                                            .updatedAt(LocalDateTime.now())
                                            .build();
                                    return userRepository.save(newUser);
                                });
                    });

            // Store Gmail refresh token for API access
            // Use the already-exchanged tokenResponse to avoid "invalid_grant" error
            // (authorization codes are single-use only and cannot be exchanged twice)
            try {
                gmailService.storeTokensFromResponse(user.getId(), tokenResponse);
                user.setGmailConnected(true);
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
            } catch (Exception e) {
                // Log but don't fail authentication if Gmail token storage fails
                // User can still use the app, just without Gmail integration
                System.err.println("Failed to store Gmail tokens: " + e.getMessage());
            }

            return generateAuthResponse(user);

        } catch (Exception e) {
            throw new UnauthorizedException("Failed to authenticate with Google authorization code: " + e.getMessage());
        }
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new UnauthorizedException("Refresh token has expired");
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        // Generate new access token
        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getName(), user.getProfilePicture());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getAccessTokenExpirationMs() / 1000)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .profilePicture(user.getProfilePicture())
                        .build())
                .build();
    }
    
    @Transactional
    public void logout(String userId, String refreshTokenStr) {
        if (refreshTokenStr != null) {
            refreshTokenRepository.deleteByToken(refreshTokenStr);
        } else {
            refreshTokenRepository.deleteByUserId(userId);
        }
    }

    public AuthResponse.UserInfo getUserInfo(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        return AuthResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .profilePicture(user.getProfilePicture())
                .build();
    }

    public TokenIntrospectResponse introspectToken(TokenIntrospectRequest request) {
        String token = request.getToken();
        boolean isValid = tokenProvider.validateToken(token);

        return TokenIntrospectResponse.builder()
                .isValid(isValid)
                .build();
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Check if user has password (not pure Google OAuth user)
        if (user.getPassword() == null) {
            throw new BadRequestException("This account uses Google login only. Please login with Google.");
        }

        // Send OTP for password reset
        otpService.generateAndSendOtp(user.getEmail(), user.getName(), com.hcmus.awad_email.model.Otp.OtpType.FORGOT_PASSWORD);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // Verify OTP
        otpService.verifyOtp(request.getEmail(), request.getCode(), com.hcmus.awad_email.model.Otp.OtpType.FORGOT_PASSWORD);

        // Find user and update password
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());

        // If user was Google-only, now they have BOTH
        if (user.getAuthProvider() == User.AuthProvider.GOOGLE && user.getGoogleId() != null) {
            user.setAuthProvider(User.AuthProvider.BOTH);
        } else if (user.getAuthProvider() == null || user.getAuthProvider() == User.AuthProvider.GOOGLE) {
            user.setAuthProvider(User.AuthProvider.EMAIL);
        }

        userRepository.save(user);

        // Revoke all refresh tokens for security
        refreshTokenRepository.deleteByUserId(user.getId());
    }

    @Transactional
    public void sendChangePasswordOtp(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Check if user has password
        if (user.getPassword() == null) {
            throw new BadRequestException("This account uses Google login only. Cannot change password.");
        }

        // Send OTP for password change
        otpService.generateAndSendOtp(user.getEmail(), user.getName(), com.hcmus.awad_email.model.Otp.OtpType.CHANGE_PASSWORD);
    }

    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        // Verify OTP
        otpService.verifyOtp(user.getEmail(), request.getCode(), com.hcmus.awad_email.model.Otp.OtpType.CHANGE_PASSWORD);

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Revoke all refresh tokens for security
        refreshTokenRepository.deleteByUserId(user.getId());
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getName(), user.getProfilePicture());
        String refreshTokenStr = tokenProvider.generateRefreshToken(user.getId());

        // Save refresh token to database
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenStr)
                .userId(user.getId())
                .expiryDate(LocalDateTime.now().plusSeconds(tokenProvider.getRefreshTokenExpirationMs() / 1000))
                .createdAt(LocalDateTime.now())
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getAccessTokenExpirationMs() / 1000)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .profilePicture(user.getProfilePicture())
                        .build())
                .build();
    }
}

