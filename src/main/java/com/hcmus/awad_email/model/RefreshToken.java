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
 * Stores refresh tokens with SHA-256 hashing for security.
 * The actual token is never stored - only its hash.
 * This protects tokens even if the database is compromised.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "refresh_tokens")
public class RefreshToken {

    @Id
    private String id;

    /**
     * SHA-256 hash of the refresh token.
     * The actual token is never stored in the database.
     */
    @Indexed(unique = true)
    private String tokenHash;

    @Indexed
    private String userId;

    private LocalDateTime expiryDate;

    private LocalDateTime createdAt;

    private boolean revoked;
}

