package com.hcmus.awad_email.controller;

import com.hcmus.awad_email.dto.common.ApiResponse;
import com.hcmus.awad_email.dto.kanban.*;
import com.hcmus.awad_email.service.KanbanService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kanban")
@Slf4j
public class KanbanController {
    
    @Autowired
    private KanbanService kanbanService;
    
    // ==================== Board Operations ====================
    
    /**
     * Get the full Kanban board with all columns and emails.
     */
    @GetMapping("/board")
    public ResponseEntity<ApiResponse<KanbanBoardResponse>> getBoard(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        log.info("📋 Get Kanban board for user: {}", userId);
        KanbanBoardResponse board = kanbanService.getBoard(userId);
        return ResponseEntity.ok(ApiResponse.success(board));
    }
    
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

