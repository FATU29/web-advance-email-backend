package com.hcmus.awad_email.dto.email;

import com.hcmus.awad_email.model.Attachment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailDetailResponse {
    
    private String id;
    
    private String from;
    
    private String fromName;
    
    private List<String> to;
    
    private List<String> cc;
    
    private List<String> bcc;
    
    private String subject;
    
    private String body;
    
    private boolean isRead;
    
    private boolean isStarred;
    
    private boolean isImportant;
    
    private List<Attachment> attachments;
    
    private LocalDateTime receivedAt;
    
    private LocalDateTime sentAt;
}

