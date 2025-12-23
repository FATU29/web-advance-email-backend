package com.hcmus.awad_email.dto.search;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for semantic search.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemanticSearchRequest {
    
    @NotBlank(message = "Query is required")
    @Size(min = 1, max = 500, message = "Query must be between 1 and 500 characters")
    private String query;
    
    // Maximum number of results to return (default: 20)
    private Integer limit;
    
    // Minimum similarity score threshold (0.0 to 1.0, default: 0.3)
    private Double minScore;
    
    // Whether to generate embeddings for emails that don't have them yet
    private Boolean generateMissingEmbeddings;
}

