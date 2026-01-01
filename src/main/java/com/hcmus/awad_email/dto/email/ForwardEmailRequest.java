package com.hcmus.awad_email.dto.email;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for forwarding an email.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForwardEmailRequest {
    
    @NotEmpty(message = "At least one recipient is required")
    private List<String> to;
    
    private List<String> cc;
    
    private List<String> bcc;
    
    /**
     * Optional additional message to include before the forwarded content.
     */
    private String additionalMessage;
    
    private List<String> attachmentIds; // For future attachment support
}

