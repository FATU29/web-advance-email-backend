package com.hcmus.awad_email.controller;

import com.google.api.services.gmail.model.Label;
import com.hcmus.awad_email.dto.common.ApiResponse;
import com.hcmus.awad_email.dto.kanban.*;
import com.hcmus.awad_email.service.FuzzySearchService;
import com.hcmus.awad_email.service.GmailService;
import com.hcmus.awad_email.service.KanbanService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/kanban")
@Slf4j
public class KanbanController {

    @Autowired
    private KanbanService kanbanService;

    @Autowired
    private FuzzySearchService fuzzySearchService;

    @Autowired
    private GmailService gmailService;
    
    // ==================== Board Operations ====================

    /**
     * Get the full Kanban board with all columns and emails.
     * Uses cached emails from database for fast loading.
     * Set sync=true to fetch new emails from Gmail first.
     *
     * @param maxEmails Maximum emails to display (default: 50, max: 100)
     * @param sync If true, sync new emails from Gmail before returning (default: false)
     */
    @GetMapping("/board")
    public ResponseEntity<ApiResponse<KanbanBoardResponse>> getBoard(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "50") Integer maxEmails,
            @RequestParam(required = false, defaultValue = "false") Boolean sync) {
        String userId = (String) authentication.getPrincipal();
        log.info("📋 Get Kanban board for user: {} (maxEmails: {}, sync: {})", userId, maxEmails, sync);

        // Limit max emails to 100
        int limitedMax = Math.min(maxEmails != null ? maxEmails : 50, 100);

        KanbanBoardResponse board = kanbanService.getBoard(userId, limitedMax, sync != null && sync);
        return ResponseEntity.ok(ApiResponse.success(board));
    }

    /**
     * Get the Kanban board with filtering and sorting options.
     * Supports sorting by date (newest/oldest) or sender name,
     * and filtering by unread status, attachments, or sender.
     *
     * Query Parameters:
     * - sortBy: "date_newest" (default), "date_oldest", "sender_name"
     * - unreadOnly: true/false (default: false)
     * - hasAttachmentsOnly: true/false (default: false)
     * - fromSender: partial match on sender email or name
     * - columnId: filter by specific column
     * - maxEmailsPerColumn: max emails per column (default: 50, max: 100)
     */
    @GetMapping("/board/filter")
    public ResponseEntity<ApiResponse<KanbanBoardResponse>> getBoardWithFilters(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "date_newest") String sortBy,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(required = false) Boolean hasAttachmentsOnly,
            @RequestParam(required = false) String fromSender,
            @RequestParam(required = false) String columnId,
            @RequestParam(required = false, defaultValue = "50") Integer maxEmailsPerColumn) {
        String userId = (String) authentication.getPrincipal();
        log.info("📋 Get filtered Kanban board for user: {} (sortBy: {}, unreadOnly: {}, hasAttachmentsOnly: {}, fromSender: {})",
                userId, sortBy, unreadOnly, hasAttachmentsOnly, fromSender);

        KanbanFilterRequest filterRequest = KanbanFilterRequest.builder()
                .sortBy(sortBy)
                .unreadOnly(unreadOnly)
                .hasAttachmentsOnly(hasAttachmentsOnly)
                .fromSender(fromSender)
                .columnId(columnId)
                .maxEmailsPerColumn(maxEmailsPerColumn)
                .build();

        KanbanBoardResponse board = kanbanService.getBoardWithFilters(userId, filterRequest);
        return ResponseEntity.ok(ApiResponse.success(board));
    }

    // ==================== Fuzzy Search Operations ====================

    /**
     * Fuzzy search for emails on the Kanban board.
     * Searches over subject and sender (name or email) with typo tolerance and partial matches.
     * Results are ranked by relevance (best matches first).
     *
     * Examples:
     * - "marketing" → finds emails about "marketing" even with typos like "markting"
     * - "Nguy" → finds senders like "Nguyễn Văn A", "nguyen@example.com"
     *
     * Query Parameters:
     * - query: The search query (required)
     * - limit: Maximum results to return (default: 20, max: 100)
     * - includeBody: Also search in preview/summary (default: false)
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<FuzzySearchResponse>> fuzzySearch(
            Authentication authentication,
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            @RequestParam(required = false, defaultValue = "false") Boolean includeBody) {
        String userId = (String) authentication.getPrincipal();
        log.info("🔍 Fuzzy search for user: {} with query: '{}' (limit: {}, includeBody: {})",
                userId, query, limit, includeBody);

        FuzzySearchRequest request = FuzzySearchRequest.builder()
                .query(query)
                .limit(limit)
                .includeBody(includeBody)
                .build();

        FuzzySearchResponse response = fuzzySearchService.search(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Fuzzy search for emails using POST request body.
     * Same as GET /search but accepts request body for complex queries.
     */
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<FuzzySearchResponse>> fuzzySearchPost(
            Authentication authentication,
            @RequestBody FuzzySearchRequest request) {
        String userId = (String) authentication.getPrincipal();
        log.info("🔍 Fuzzy search (POST) for user: {} with query: '{}' (limit: {}, includeBody: {})",
                userId, request.getQuery(), request.getLimit(), request.getIncludeBody());

        FuzzySearchResponse response = fuzzySearchService.search(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== Gmail Sync Operations ====================

    /**
     * Sync Gmail emails to the Kanban board.
     * New emails from Gmail INBOX are automatically added to the Kanban INBOX column.
     *
     * @param maxEmails Maximum number of emails to sync (default 50, max 100)
     */
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<KanbanSyncResult>> syncGmailEmails(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "50") Integer maxEmails) {
        String userId = (String) authentication.getPrincipal();
        log.info("📋 Sync Gmail emails to Kanban for user: {} (max: {})", userId, maxEmails);

        // Limit max emails to 100
        int limit = Math.min(maxEmails != null ? maxEmails : 50, 100);

        KanbanSyncResult result = kanbanService.syncGmailEmails(userId, limit);
        return ResponseEntity.ok(ApiResponse.success(result.getMessage(), result));
    }

    /**
     * Check if Gmail is connected for the user.
     */
    @GetMapping("/gmail-status")
    public ResponseEntity<ApiResponse<GmailStatusResponse>> getGmailStatus(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        boolean connected = kanbanService.isGmailConnected(userId);
        GmailStatusResponse status = new GmailStatusResponse(connected);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * Simple response for Gmail connection status.
     */
    public record GmailStatusResponse(boolean connected) {}

    /**
     * Get available Gmail labels for column mapping.
     */
    @GetMapping("/gmail-labels")
    public ResponseEntity<ApiResponse<List<GmailLabelResponse>>> getGmailLabels(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        log.info("📋 Get Gmail labels for user: {}", userId);

        if (!kanbanService.isGmailConnected(userId)) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }

        List<Label> labels = gmailService.listLabels(userId);
        List<GmailLabelResponse> response = labels.stream()
                .map(label -> new GmailLabelResponse(
                        label.getId(),
                        label.getName(),
                        label.getType(),
                        label.getMessageListVisibility(),
                        label.getLabelListVisibility()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Response for Gmail label information.
     */
    public record GmailLabelResponse(
            String id,
            String name,
            String type,
            String messageListVisibility,
            String labelListVisibility
    ) {}

    // ==================== Column Operations ====================
    
    /**
     * Get all columns for the user.
     */
    @GetMapping("/columns")
    public ResponseEntity<ApiResponse<List<KanbanColumnResponse>>> getColumns(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        log.info("📋 Get Kanban columns for user: {}", userId);
        List<KanbanColumnResponse> columns = kanbanService.getColumns(userId);
        return ResponseEntity.ok(ApiResponse.success(columns));
    }
    
    /**
     * Create a new custom column.
     */
    @PostMapping("/columns")
    public ResponseEntity<ApiResponse<KanbanColumnResponse>> createColumn(
            Authentication authentication,
            @Valid @RequestBody CreateColumnRequest request) {
        String userId = (String) authentication.getPrincipal();
        log.info("📋 Create Kanban column '{}' for user: {}", request.getName(), userId);
        KanbanColumnResponse column = kanbanService.createColumn(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Column created successfully", column));
    }
    
    /**
     * Update a column.
     */
    @PutMapping("/columns/{columnId}")
    public ResponseEntity<ApiResponse<KanbanColumnResponse>> updateColumn(
            Authentication authentication,
            @PathVariable String columnId,
            @Valid @RequestBody UpdateColumnRequest request) {
        String userId = (String) authentication.getPrincipal();
        log.info("📋 Update Kanban column {} for user: {}", columnId, userId);
        KanbanColumnResponse column = kanbanService.updateColumn(userId, columnId, request);
        return ResponseEntity.ok(ApiResponse.success("Column updated successfully", column));
    }
    
    /**
     * Delete a custom column.
     */
    @DeleteMapping("/columns/{columnId}")
    public ResponseEntity<ApiResponse<Void>> deleteColumn(
            Authentication authentication,
            @PathVariable String columnId) {
        String userId = (String) authentication.getPrincipal();
        log.info("📋 Delete Kanban column {} for user: {}", columnId, userId);
        kanbanService.deleteColumn(userId, columnId);
        return ResponseEntity.ok(ApiResponse.success("Column deleted successfully", null));
    }
    
    // ==================== Email Operations ====================
    
    /**
     * Get emails in a specific column.
     */
    @GetMapping("/columns/{columnId}/emails")
    public ResponseEntity<ApiResponse<List<KanbanEmailResponse>>> getEmailsInColumn(
            Authentication authentication,
            @PathVariable String columnId) {
        String userId = (String) authentication.getPrincipal();
        log.info("📋 Get emails in column {} for user: {}", columnId, userId);
        List<KanbanEmailResponse> emails = kanbanService.getEmailsInColumn(userId, columnId);
        return ResponseEntity.ok(ApiResponse.success(emails));
    }
    
    /**
     * Add an email to the Kanban board.
     */
    @PostMapping("/emails")
    public ResponseEntity<ApiResponse<KanbanEmailResponse>> addEmailToKanban(
            Authentication authentication,
            @Valid @RequestBody AddEmailToKanbanRequest request) {
        String userId = (String) authentication.getPrincipal();
        log.info("📋 Add email {} to Kanban for user: {}", request.getEmailId(), userId);
        KanbanEmailResponse email = kanbanService.addEmailToKanban(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Email added to Kanban board", email));
    }
    
    /**
     * Get a single email's Kanban status.
     */
    @GetMapping("/emails/{emailId}")
    public ResponseEntity<ApiResponse<KanbanEmailResponse>> getEmailStatus(
            Authentication authentication,
            @PathVariable String emailId) {
        String userId = (String) authentication.getPrincipal();
        log.info("📋 Get email {} status for user: {}", emailId, userId);
        KanbanEmailResponse email = kanbanService.getEmailStatus(userId, emailId);
        return ResponseEntity.ok(ApiResponse.success(email));
    }
    
    /**
     * Move an email to a different column (drag-and-drop).
     */
    @PostMapping("/emails/move")
    public ResponseEntity<ApiResponse<KanbanEmailResponse>> moveEmail(
            Authentication authentication,
            @Valid @RequestBody MoveEmailRequest request) {
        String userId = (String) authentication.getPrincipal();
        log.info("📋 Move email {} to column {} for user: {}", 
                request.getEmailId(), request.getTargetColumnId(), userId);
        KanbanEmailResponse email = kanbanService.moveEmail(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Email moved successfully", email));
    }
    
    /**
     * Remove an email from the Kanban board.
     */
    @DeleteMapping("/emails/{emailId}")
    public ResponseEntity<ApiResponse<Void>> removeEmailFromKanban(
            Authentication authentication,
            @PathVariable String emailId) {
        String userId = (String) authentication.getPrincipal();
        log.info("📋 Remove email {} from Kanban for user: {}", emailId, userId);
        kanbanService.removeEmailFromKanban(userId, emailId);
        return ResponseEntity.ok(ApiResponse.success("Email removed from Kanban board", null));
    }

    // ==================== Snooze Operations ====================

    /**
     * Snooze an email until a specific time.
     */
    @PostMapping("/emails/snooze")
    public ResponseEntity<ApiResponse<KanbanEmailResponse>> snoozeEmail(
            Authentication authentication,
            @Valid @RequestBody SnoozeEmailRequest request) {
        String userId = (String) authentication.getPrincipal();
        log.info("📋 Snooze email {} until {} for user: {}",
                request.getEmailId(), request.getSnoozeUntil(), userId);
        KanbanEmailResponse email = kanbanService.snoozeEmail(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Email snoozed successfully", email));
    }

    /**
     * Unsnooze an email (restore to previous column).
     */
    @PostMapping("/emails/{emailId}/unsnooze")
    public ResponseEntity<ApiResponse<KanbanEmailResponse>> unsnoozeEmail(
            Authentication authentication,
            @PathVariable String emailId) {
        String userId = (String) authentication.getPrincipal();
        log.info("📋 Unsnooze email {} for user: {}", emailId, userId);
        KanbanEmailResponse email = kanbanService.unsnoozeEmail(userId, emailId);
        return ResponseEntity.ok(ApiResponse.success("Email unsnoozed successfully", email));
    }

    // ==================== Summary Operations ====================

    /**
     * Generate or regenerate AI summary for an email.
     */
    @PostMapping("/emails/{emailId}/summarize")
    public ResponseEntity<ApiResponse<KanbanEmailResponse>> generateSummary(
            Authentication authentication,
            @PathVariable String emailId) {
        String userId = (String) authentication.getPrincipal();
        log.info("📋 Generate summary for email {} for user: {}", emailId, userId);
        KanbanEmailResponse email = kanbanService.generateSummary(userId, emailId);
        return ResponseEntity.ok(ApiResponse.success("Summary generated successfully", email));
    }
}

