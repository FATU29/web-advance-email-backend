package com.hcmus.awad_email.dto.kanban;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KanbanEmailResponse {
    
    private String id; // EmailKanbanStatus ID
    
    private String emailId; // Gmail message ID
    
    private String columnId;
    
    private int orderInColumn;
    
    // Email metadata
    private String subject;
    
    private String fromEmail;
    
    private String fromName;
    
    private String preview;
    
    private LocalDateTime receivedAt;
    
    private boolean isRead;
    
    private boolean isStarred;
    
    // AI Summary
    private String summary;
    
    private LocalDateTime summaryGeneratedAt;
    
    // Snooze info
    private boolean snoozed;
    
    private LocalDateTime snoozeUntil;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}

