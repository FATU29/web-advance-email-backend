package com.hcmus.awad_email.dto.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailListResponse {
    
    private String id;
    
    private String from;
    
    private String fromName;
    
    private String subject;
    
    private String preview;
    
    private boolean isRead;
    
    private boolean isStarred;
    
    private boolean isImportant;
    
    private boolean hasAttachments;
    
    private LocalDateTime receivedAt;
}

