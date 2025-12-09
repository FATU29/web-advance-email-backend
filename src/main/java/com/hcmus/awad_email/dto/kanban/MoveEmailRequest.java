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
public class MoveEmailRequest {
    
    @NotBlank(message = "Email ID is required")
    private String emailId;
    
    @NotBlank(message = "Target column ID is required")
    private String targetColumnId;
    
    private Integer newOrder; // Optional, position in the target column
}

