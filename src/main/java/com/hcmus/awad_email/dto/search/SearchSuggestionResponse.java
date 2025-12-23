package com.hcmus.awad_email.dto.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for search auto-suggestions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchSuggestionResponse {
    
    private String query;
    
    // Contact suggestions (sender names/emails)
    private List<ContactSuggestion> contacts;
    
    // Subject keyword suggestions
    private List<KeywordSuggestion> keywords;
    
    // Recent search queries (if implemented)
    private List<String> recentSearches;
    
    /**
     * Contact suggestion item.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactSuggestion {
        private String email;
        private String name;
        private int emailCount; // Number of emails from this contact
    }
    
    /**
     * Keyword suggestion item.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeywordSuggestion {
        private String keyword;
        private int occurrences; // Number of emails containing this keyword
        private String type; // "subject", "sender", etc.
    }
}

