package com.hcmus.awad_email.dto.email;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendEmailRequest {
    
    @NotEmpty(message = "At least one recipient is required")
    private List<String> to;
    
    private List<String> cc;
    
    private List<String> bcc;
    
    @NotBlank(message = "Subject is required")
    private String subject;
    
    @NotBlank(message = "Body is required")
    private String body;
    
    private List<String> attachmentIds; // For future attachment support
}

