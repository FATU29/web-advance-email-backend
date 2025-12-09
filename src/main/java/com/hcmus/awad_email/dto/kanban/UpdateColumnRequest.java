package com.hcmus.awad_email.dto.kanban;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateColumnRequest {
    
    @Size(min = 1, max = 50, message = "Column name must be between 1 and 50 characters")
    private String name;
    
    private String color;
    
    private Integer order;
}

