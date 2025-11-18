package com.hcmus.awad_email.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Google OAuth 2.0 Authorization Code Flow
 * The frontend sends only the authorization code received from Google OAuth redirect
 * The redirect URI is configured on the backend in application.yml for security
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleAuthCodeRequest {

    @NotBlank(message = "Authorization code is required")
    private String code; // Authorization code from Google OAuth redirect
}

