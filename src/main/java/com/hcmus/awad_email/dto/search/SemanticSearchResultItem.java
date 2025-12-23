package com.hcmus.awad_email.dto.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Individual search result item for semantic search.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemanticSearchResultItem {
    
    private String emailId;
    
    private String subject;
    
    private String fromEmail;
    
    private String fromName;
    
    private String preview;
    
    private String columnId;
    
    private String columnName;
    
    private LocalDateTime receivedAt;
    
    private boolean isRead;
    
    private boolean isStarred;
    
    private boolean hasAttachments;
    
    // Semantic similarity score (0.0 to 1.0)
    private double similarityScore;
    
    // AI-generated summary if available
    private String summary;
}

