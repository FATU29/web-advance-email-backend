package com.hcmus.awad_email.controller;

import com.hcmus.awad_email.dto.common.ApiResponse;
import com.hcmus.awad_email.dto.search.SemanticSearchRequest;
import com.hcmus.awad_email.dto.search.SemanticSearchResponse;
import com.hcmus.awad_email.dto.search.SearchSuggestionResponse;
import com.hcmus.awad_email.dto.search.SearchSuggestionResponse.ContactSuggestion;
import com.hcmus.awad_email.service.SemanticSearchService;
import com.hcmus.awad_email.service.SearchSuggestionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for search operations including semantic search and auto-suggestions.
 */
@RestController
@RequestMapping("/api/search")
@Slf4j
public class SearchController {

    @Autowired
    private SemanticSearchService semanticSearchService;

    @Autowired
    private SearchSuggestionService searchSuggestionService;

    /**
     * Check if semantic search is available (AI service configured and running).
     */
    @GetMapping("/semantic/status")
    public ResponseEntity<ApiResponse<SemanticSearchStatusResponse>> getSemanticSearchStatus() {
        boolean available = semanticSearchService.isAvailable();
        SemanticSearchStatusResponse status = new SemanticSearchStatusResponse(
                available,
                available ? "Semantic search is available" : "AI service not available"
        );
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * Perform semantic search on emails via AI service.
     * Uses vector embeddings to find conceptually related emails.
     *
     * Example: Searching for "money" will find emails about "invoice", "price", "salary"
     * even if the word "money" doesn't appear in them.
     */
    @PostMapping("/semantic")
    public ResponseEntity<ApiResponse<SemanticSearchResponse>> semanticSearch(
            Authentication authentication,
            @Valid @RequestBody SemanticSearchRequest request) {
        String userId = (String) authentication.getPrincipal();
        log.info("🔍 Semantic search for user: {} | query: '{}'", userId, request.getQuery());

        if (!semanticSearchService.isAvailable()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Semantic search is not available. AI service not configured or not running."));
        }

        SemanticSearchResponse response = semanticSearchService.search(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Generate embeddings for all emails that don't have them via AI service.
     * This is useful for initializing semantic search on existing emails.
     */
    @PostMapping("/semantic/generate-embeddings")
    public ResponseEntity<ApiResponse<GenerateEmbeddingsResponse>> generateEmbeddings(
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        log.info("🔄 Generating embeddings for user: {}", userId);

        if (!semanticSearchService.isAvailable()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Semantic search is not available. AI service not configured or not running."));
        }

        int generated = semanticSearchService.generateAllEmbeddings(userId);
        GenerateEmbeddingsResponse response = new GenerateEmbeddingsResponse(
                generated,
                "Generated embeddings for " + generated + " emails"
        );
        return ResponseEntity.ok(ApiResponse.success(response.message(), response));
    }

    /**
     * Generate embedding for a single email via AI service.
     */
    @PostMapping("/semantic/generate-embedding/{emailId}")
    public ResponseEntity<ApiResponse<Void>> generateEmbeddingForEmail(
            Authentication authentication,
            @PathVariable String emailId) {
        String userId = (String) authentication.getPrincipal();
        log.info("🔄 Generating embedding for email {} for user: {}", emailId, userId);

        if (!semanticSearchService.isAvailable()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Semantic search is not available. AI service not configured or not running."));
        }

        boolean success = semanticSearchService.generateEmbeddingForEmail(userId, emailId);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("Embedding generated successfully", null));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to generate embedding. Email not found."));
        }
    }

    // ==================== Auto-Suggestion Endpoints ====================

    /**
     * Get search suggestions based on query prefix.
     * Returns matching contacts and keywords for type-ahead functionality.
     *
     * As the user types, call this endpoint to get suggestions.
     */
    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<SearchSuggestionResponse>> getSuggestions(
            Authentication authentication,
            @RequestParam String query) {
        String userId = (String) authentication.getPrincipal();
        log.debug("💡 Getting suggestions for user: {} | query: '{}'", userId, query);

        SearchSuggestionResponse response = searchSuggestionService.getSuggestions(userId, query);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all unique contacts (senders) for the user.
     * Useful for populating contact autocomplete or address book.
     */
    @GetMapping("/contacts")
    public ResponseEntity<ApiResponse<List<ContactSuggestion>>> getAllContacts(
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        log.debug("📇 Getting all contacts for user: {}", userId);

        List<ContactSuggestion> contacts = searchSuggestionService.getAllContacts(userId);
        return ResponseEntity.ok(ApiResponse.success(contacts));
    }

    /**
     * Response for semantic search status check.
     */
    public record SemanticSearchStatusResponse(boolean available, String message) {}

    /**
     * Response for generate embeddings operation.
     */
    public record GenerateEmbeddingsResponse(int generated, String message) {}
}

