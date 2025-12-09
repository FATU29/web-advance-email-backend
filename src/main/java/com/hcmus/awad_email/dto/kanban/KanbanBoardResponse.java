package com.hcmus.awad_email.dto.kanban;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response containing the full Kanban board state.
 * Includes all columns and their emails.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KanbanBoardResponse {
    
    private List<KanbanColumnResponse> columns;
    
    // Map of columnId -> list of emails in that column
    private Map<String, List<KanbanEmailResponse>> emailsByColumn;
}

