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
    private MailboxService mailboxService;
    
    @Value("${app.google.client-id}")
    private String googleClientId;

    @Value("${app.google.client-secret}")
    private String googleClientSecret;

    @Value("${app.google.redirect-uri}")
    private String googleRedirectUri;
    
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }
        
        User user = User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .authProvider(User.AuthProvider.EMAIL)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        user = userRepository.save(user);
        
        // Initialize default mailboxes for the new user
        mailboxService.initializeDefaultMailboxes(user.getId());
        
        return generateAuthResponse(user);
    }
    
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        
        if (user.getAuthProvider() != User.AuthProvider.EMAIL) {
            throw new BadRequestException("Please use " + user.getAuthProvider() + " to login");
        }
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        
        if (!user.isEnabled()) {
            throw new UnauthorizedException("Account is disabled");
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
                                    existingUser.setAuthProvider(User.AuthProvider.GOOGLE);
                                    existingUser.setProfilePicture(pictureUrl);
                                    existingUser.setUpdatedAt(LocalDateTime.now());
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
                                            .createdAt(LocalDateTime.now())
                                            .updatedAt(LocalDateTime.now())
                                            .build();
                                    newUser = userRepository.save(newUser);

                                    // Initialize default mailboxes
                                    mailboxService.initializeDefaultMailboxes(newUser.getId());

                                    return newUser;
                                });
                    });

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

