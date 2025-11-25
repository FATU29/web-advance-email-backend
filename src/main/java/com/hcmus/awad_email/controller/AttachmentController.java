package com.hcmus.awad_email.controller;

import com.hcmus.awad_email.service.GmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for handling email attachments
 */
@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    private static final Logger log = LoggerFactory.getLogger(AttachmentController.class);

    @Autowired
    private GmailService gmailService;
    
    /**
     * Download an attachment from Gmail
     * @param messageId Gmail message ID
     * @param attachmentId Gmail attachment ID
     */
    @GetMapping("/{messageId}/{attachmentId}")
    public ResponseEntity<byte[]> downloadAttachment(
            Authentication authentication,
            @PathVariable String messageId,
            @PathVariable String attachmentId,
            @RequestParam(required = false) String filename) {

        String userId = (String) authentication.getPrincipal();
        log.info("📎 Download attachment for user: {} | messageId: {} | attachmentId: {} | filename: {}",
                userId, messageId, attachmentId, filename);

        // This is a placeholder - full implementation would require:
        // 1. Fetching attachment data from Gmail API
        // 2. Determining content type
        // 3. Streaming the data

        // For now, return a simple response
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        if (filename != null) {
            headers.setContentDispositionFormData("attachment", filename);
        }

        log.warn("⚠️ Attachment download not fully implemented yet");
        // TODO: Implement actual attachment download from Gmail API
        return ResponseEntity.ok()
                .headers(headers)
                .body(new byte[0]);
    }
}

