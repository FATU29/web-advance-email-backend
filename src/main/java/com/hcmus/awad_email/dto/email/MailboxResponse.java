package com.hcmus.awad_email.dto.email;

import com.hcmus.awad_email.model.Mailbox;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailboxResponse {
    
    private String id;
    
    private String name;
    
    private Mailbox.MailboxType type;
    
    private int unreadCount;
    
    private int totalCount;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}

