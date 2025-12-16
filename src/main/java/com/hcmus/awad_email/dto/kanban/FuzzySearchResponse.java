package com.hcmus.awad_email.dto.kanban;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuzzySearchResponse {
    
    /**
     * The original search query
     */
    private String query;
    
    /**
     * Total number of results found
     */
    private int totalResults;
    
    /**
     * Search results ranked by relevance (best matches first)
     */
    private List<SearchResultItem> results;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResultItem {
        
        private String id; // EmailKanbanStatus ID
        
        private String emailId; // Gmail message ID
        
        private String columnId;
        
        private String columnName;
        
        // Email metadata
        private String subject;
        
        private String fromEmail;
        
        private String fromName;
        
        private String preview;
        
        private String summary;
        
        private java.time.LocalDateTime receivedAt;
        
        private boolean isRead;
        
        private boolean isStarred;
        
        private boolean hasAttachments;
        
        /**
         * Relevance score (higher is better match)
         */
        private double score;
        
        /**
         * Which fields matched the query
         */
        private List<String> matchedFields;
    }
}

