package com.hcmus.awad_email.dto.kanban;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuzzySearchRequest {
    
    /**
     * The search query string.
     * Supports typo tolerance and partial matches.
     */
    private String query;
    
    /**
     * Maximum number of results to return (default: 20, max: 100)
     */
    private Integer limit;
    
    /**
     * Whether to search in email body/summary as well (default: false)
     * When true, also searches in preview and summary fields
     */
    private Boolean includeBody;
}

