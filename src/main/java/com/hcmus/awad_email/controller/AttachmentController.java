package com.hcmus.awad_email.controller;

import com.hcmus.awad_email.dto.common.ApiResponse;
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
     * @param filename Optional filename for the download
     * @param mimeType Optional MIME type for the content
     */
    @GetMapping("/{messageId}/{attachmentId}")
    public ResponseEntity<byte[]> downloadAttachment(
            Authentication authentication,
            @PathVariable String messageId,
            @PathVariable String attachmentId,
            @RequestParam(required = false) String filename,
            @RequestParam(required = false) String mimeType) {

        String userId = (String) authentication.getPrincipal();
        log.info("📎 Download attachment for user: {} | messageId: {} | attachmentId: {} | filename: {}",
                userId, messageId, attachmentId, filename);

        // Check if Gmail is connected
        if (!gmailService.isGmailConnected(userId)) {
            log.warn("Gmail not connected for user: {}", userId);
            return ResponseEntity.badRequest().build();
        }

        // Fetch attachment data from Gmail API
        byte[] attachmentData = gmailService.getAttachment(userId, messageId, attachmentId);

        if (attachmentData == null || attachmentData.length == 0) {
            log.warn("Attachment data is empty for messageId: {} attachmentId: {}", messageId, attachmentId);
            return ResponseEntity.notFound().build();
        }

        // Set response headers
        HttpHeaders headers = new HttpHeaders();

        // Set content type
        if (mimeType != null && !mimeType.isEmpty()) {
            headers.setContentType(MediaType.parseMediaType(mimeType));
        } else {
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        }

        // Set content disposition for download
        if (filename != null && !filename.isEmpty()) {
            headers.setContentDispositionFormData("attachment", filename);
        } else {
            headers.setContentDispositionFormData("attachment", "attachment");
        }

        headers.setContentLength(attachmentData.length);

        log.info("✅ Attachment downloaded successfully | size: {} bytes", attachmentData.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(attachmentData);
    }
}

