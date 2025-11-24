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
@Document(collection = "otps")
public class Otp {
    
    @Id
    private String id;
    
    @Indexed
    private String email;
    
    private String code;
    
    private OtpType type;
    
    private LocalDateTime expiryTime;
    
    private LocalDateTime createdAt;
    
    private boolean used;
    
    private int attempts; // Track verification attempts
    
    public enum OtpType {
        SIGNUP,
        FORGOT_PASSWORD,
        CHANGE_PASSWORD
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }
}

