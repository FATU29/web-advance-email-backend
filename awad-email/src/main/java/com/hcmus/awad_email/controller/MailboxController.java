package com.hcmus.awad_email.controller;

import com.hcmus.awad_email.dto.common.ApiResponse;
import com.hcmus.awad_email.dto.email.MailboxResponse;
import com.hcmus.awad_email.service.MailboxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mailboxes")
public class MailboxController {
    
    @Autowired
    private MailboxService mailboxService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<MailboxResponse>>> getMailboxes(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        List<MailboxResponse> mailboxes = mailboxService.getUserMailboxes(userId);
        return ResponseEntity.ok(ApiResponse.success(mailboxes));
    }
    
    @GetMapping("/{mailboxId}")
    public ResponseEntity<ApiResponse<MailboxResponse>> getMailbox(
            Authentication authentication,
            @PathVariable String mailboxId) {
        String userId = (String) authentication.getPrincipal();
        MailboxResponse mailbox = mailboxService.getMailboxById(userId, mailboxId);
        return ResponseEntity.ok(ApiResponse.success(mailbox));
    }
}

