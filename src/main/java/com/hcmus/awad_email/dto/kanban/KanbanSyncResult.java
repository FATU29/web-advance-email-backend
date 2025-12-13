package com.hcmus.awad_email.dto.kanban;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of syncing Gmail emails to the Kanban board.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KanbanSyncResult {
    
    /**
     * Number of emails successfully synced to Kanban.
     */
    private int synced;
    
    /**
     * Number of emails skipped (already in Kanban or failed to sync).
     */
    private int skipped;
    
    /**
     * Total number of emails processed from Gmail.
     */
    private int total;
    
    /**
     * Human-readable message about the sync result.
     */
    private String message;
}

