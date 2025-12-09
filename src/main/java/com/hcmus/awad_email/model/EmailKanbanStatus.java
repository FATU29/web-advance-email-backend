package com.hcmus.awad_email.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Tracks the Kanban status of an email for a specific user.
 * Stores column assignment, snooze information, and AI-generated summary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "email_kanban_status")
@CompoundIndex(name = "user_email_idx", def = "{'userId': 1, 'emailId': 1}", unique = true)
@CompoundIndex(name = "user_column_idx", def = "{'userId': 1, 'columnId': 1}")
public class EmailKanbanStatus {
    
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    @Indexed
    private String emailId; // Gmail message ID
    
    @Indexed
    private String columnId; // Reference to KanbanColumn
    
    private int orderInColumn; // Position within the column
    
    // Snooze information
    private boolean snoozed;
    
    private LocalDateTime snoozeUntil; // When to restore the email
    
    private String previousColumnId; // Column to restore to after snooze
    
    // AI-generated summary
    private String summary;
    
    private LocalDateTime summaryGeneratedAt;
    
    // Email metadata cache (for quick display without fetching from Gmail)
    private String subject;
    
    private String fromEmail;
    
    private String fromName;
    
    private String preview;
    
    private LocalDateTime receivedAt;
    
    private boolean isRead;
    
    private boolean isStarred;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}

