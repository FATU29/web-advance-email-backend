package com.hcmus.awad_email.dto.kanban;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddEmailToKanbanRequest {
    
    @NotBlank(message = "Email ID is required")
    private String emailId;
    
    private String columnId; // Optional, defaults to Inbox column
    
    private boolean generateSummary; // Whether to generate AI summary immediately
}

