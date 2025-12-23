package com.hcmus.awad_email.dto.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for semantic search results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemanticSearchResponse {
    
    private String query;
    
    private int totalResults;
    
    private List<SemanticSearchResultItem> results;
    
    // Statistics about the search
    private int emailsWithEmbeddings;
    
    private int emailsWithoutEmbeddings;
    
    // Processing time in milliseconds
    private long processingTimeMs;
}

