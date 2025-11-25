package com.hcmus.awad_email.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Stores Google OAuth tokens for Gmail API access
 * Refresh tokens are stored securely server-side
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "google_tokens")
public class GoogleToken {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String userId; // Reference to User
    
    private String accessToken; // Short-lived access token (cached)
    
    private String refreshToken; // Long-lived refresh token (stored securely)
    
    private LocalDateTime accessTokenExpiresAt; // When access token expires
    
    private String scope; // OAuth scopes granted
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    /**
     * Check if access token is expired or about to expire (within 5 minutes)
     */
    public boolean isAccessTokenExpired() {
        if (accessTokenExpiresAt == null) {
            return true;
        }
        return LocalDateTime.now().plusMinutes(5).isAfter(accessTokenExpiresAt);
    }
}

