package com.hcmus.awad_email.controller;

import com.hcmus.awad_email.dto.common.ApiResponse;
import com.hcmus.awad_email.dto.common.PageResponse;
import com.hcmus.awad_email.dto.email.EmailActionRequest;
import com.hcmus.awad_email.dto.email.EmailDetailResponse;
import com.hcmus.awad_email.dto.email.EmailListResponse;
import com.hcmus.awad_email.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class EmailController {
    
    @Autowired
    private EmailService emailService;
    
    @GetMapping("/mailboxes/{mailboxId}/emails")
    public ResponseEntity<ApiResponse<PageResponse<EmailListResponse>>> getEmails(
            Authentication authentication,
            @PathVariable String mailboxId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String userId = (String) authentication.getPrincipal();
        PageResponse<EmailListResponse> emails = emailService.getEmailsByMailbox(userId, mailboxId, page, size);
        return ResponseEntity.ok(ApiResponse.success(emails));
    }
    
    @GetMapping("/emails/{emailId}")
    public ResponseEntity<ApiResponse<EmailDetailResponse>> getEmail(
            Authentication authentication,
            @PathVariable String emailId) {
        String userId = (String) authentication.getPrincipal();
        EmailDetailResponse email = emailService.getEmailById(userId, emailId);
        return ResponseEntity.ok(ApiResponse.success(email));
    }
    
    @PostMapping("/emails/actions")
    public ResponseEntity<ApiResponse<Void>> performAction(
            Authentication authentication,
            @RequestBody EmailActionRequest request) {
        String userId = (String) authentication.getPrincipal();
        
        switch (request.getAction().toLowerCase()) {
            case "read":
                emailService.markAsRead(userId, request.getEmailIds());
                break;
            case "unread":
                emailService.markAsUnread(userId, request.getEmailIds());
                break;
            case "star":
                emailService.toggleStar(userId, request.getEmailIds(), true);
                break;
            case "unstar":
                emailService.toggleStar(userId, request.getEmailIds(), false);
                break;
            case "delete":
                emailService.deleteEmails(userId, request.getEmailIds());
                break;
            case "archive":
                emailService.archiveEmails(userId, request.getEmailIds());
                break;
            default:
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid action: " + request.getAction()));
        }
        
        return ResponseEntity.ok(ApiResponse.success("Action performed successfully", null));
    }
    
    @PatchMapping("/emails/{emailId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            Authentication authentication,
            @PathVariable String emailId) {
        String userId = (String) authentication.getPrincipal();
        emailService.markAsRead(userId, java.util.Collections.singletonList(emailId));
        return ResponseEntity.ok(ApiResponse.success("Email marked as read", null));
    }
    
    @PatchMapping("/emails/{emailId}/unread")
    public ResponseEntity<ApiResponse<Void>> markAsUnread(
            Authentication authentication,
            @PathVariable String emailId) {
        String userId = (String) authentication.getPrincipal();
        emailService.markAsUnread(userId, java.util.Collections.singletonList(emailId));
        return ResponseEntity.ok(ApiResponse.success("Email marked as unread", null));
    }
    
    @PatchMapping("/emails/{emailId}/star")
    public ResponseEntity<ApiResponse<Void>> toggleStar(
            Authentication authentication,
            @PathVariable String emailId,
            @RequestParam boolean starred) {
        String userId = (String) authentication.getPrincipal();
        emailService.toggleStar(userId, java.util.Collections.singletonList(emailId), starred);
        return ResponseEntity.ok(ApiResponse.success(
                starred ? "Email starred" : "Email unstarred", null));
    }
    
    @DeleteMapping("/emails/{emailId}")
    public ResponseEntity<ApiResponse<Void>> deleteEmail(
            Authentication authentication,
            @PathVariable String emailId) {
        String userId = (String) authentication.getPrincipal();
        emailService.deleteEmails(userId, java.util.Collections.singletonList(emailId));
        return ResponseEntity.ok(ApiResponse.success("Email deleted", null));
    }
}

