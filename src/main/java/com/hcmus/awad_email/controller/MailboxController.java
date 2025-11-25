package com.hcmus.awad_email.controller;

import com.hcmus.awad_email.dto.common.ApiResponse;
import com.hcmus.awad_email.dto.email.MailboxResponse;
import com.hcmus.awad_email.service.MailboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mailboxes")
public class MailboxController {

    private static final Logger log = LoggerFactory.getLogger(MailboxController.class);

    @Autowired
    private MailboxService mailboxService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MailboxResponse>>> getMailboxes(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        log.info("📬 Get mailboxes for user: {}", userId);
        List<MailboxResponse> mailboxes = mailboxService.getUserMailboxes(userId);
        log.info("✅ Retrieved {} mailboxes for user: {}", mailboxes.size(), userId);
        return ResponseEntity.ok(ApiResponse.success(mailboxes));
    }
}

