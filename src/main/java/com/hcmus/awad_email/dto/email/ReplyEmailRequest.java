package com.hcmus.awad_email.dto.email;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplyEmailRequest {
    
    @NotBlank(message = "Body is required")
    private String body;
    
    private boolean replyAll; // Reply to all recipients or just sender
    
    private List<String> attachmentIds; // For future attachment support
}

