package com.hcmus.awad_email.dto.kanban;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KanbanFilterRequest {
    
    /**
     * Sort order for emails within columns.
     * Options: "date_newest", "date_oldest", "sender_name"
     * Default: "date_newest"
     */
    private String sortBy;
    
    /**
     * Filter to show only unread emails.
     * Default: false (show all)
     */
    private Boolean unreadOnly;
    
    /**
     * Filter to show only emails with attachments.
     * Default: false (show all)
     */
    private Boolean hasAttachmentsOnly;
    
    /**
     * Filter by specific sender email (partial match supported).
     * Example: "john@" will match "john@example.com"
     */
    private String fromSender;
    
    /**
     * Filter by specific column ID.
     * If null, returns all columns.
     */
    private String columnId;
    
    /**
     * Maximum emails per column (default: 50, max: 100)
     */
    private Integer maxEmailsPerColumn;
}

