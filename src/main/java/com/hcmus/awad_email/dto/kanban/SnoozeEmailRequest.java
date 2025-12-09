package com.hcmus.awad_email.dto.kanban;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SnoozeEmailRequest {
    
    @NotBlank(message = "Email ID is required")
    private String emailId;
    
    @NotNull(message = "Snooze until time is required")
    @Future(message = "Snooze time must be in the future")
    private LocalDateTime snoozeUntil;
}

