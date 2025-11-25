package com.hcmus.awad_email.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String email;
    
    private String password; // Hashed password, null for Google OAuth users
    
    private String name;
    
    private String profilePicture;
    
    private AuthProvider authProvider; // EMAIL or GOOGLE
    
    private String googleId; // Google user ID for OAuth users

    private boolean gmailConnected; // Whether Gmail API is connected for this user

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private boolean enabled;

    private boolean verified; // Email verification status

    public enum AuthProvider {
        EMAIL,
        GOOGLE,
        BOTH // User can login with both email/password and Google
    }
}

