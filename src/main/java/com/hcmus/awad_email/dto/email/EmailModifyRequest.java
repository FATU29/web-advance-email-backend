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
public class EmailModifyRequest {
    
    @NotEmpty(message = "At least one email ID is required")
    private List<String> emailIds;
    
    @NotBlank(message = "Action is required")
    private String action; // read, unread, star, unstar, delete, archive
}

