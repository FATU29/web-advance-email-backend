package com.hcmus.awad_email.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "emails")
public class Email {
    
    @Id
    private String id;
    
    @Indexed
    private String userId; // Owner of the email
    
    @Indexed
    private String mailboxId; // Which mailbox this email belongs to
    
    private String from; // Sender email address
    
    private String fromName; // Sender name
    
    private List<String> to; // Recipient email addresses
    
    private List<String> cc; // CC email addresses
    
    private List<String> bcc; // BCC email addresses
    
    private String subject;
    
    private String body; // HTML or plain text body
    
    private String preview; // Short preview text (first 100 chars)
    
    private boolean isRead;
    
    private boolean isStarred;
    
    private boolean isImportant;
    
    private List<Attachment> attachments;
    
    private LocalDateTime receivedAt;
    
    private LocalDateTime sentAt;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // For future Gmail integration
    private String gmailMessageId; // Gmail message ID
    
    private String gmailThreadId; // Gmail thread ID
}

