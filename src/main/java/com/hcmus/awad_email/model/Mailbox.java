package com.hcmus.awad_email.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "mailboxes")
public class Mailbox {
    
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    private String name; // Inbox, Sent, Drafts, Trash, Archive, Starred, or custom folder name
    
    private MailboxType type;
    
    private int unreadCount;
    
    private int totalCount;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    public enum MailboxType {
        INBOX,
        SENT,
        DRAFTS,
        TRASH,
        ARCHIVE,
        STARRED,
        CUSTOM
    }
}

