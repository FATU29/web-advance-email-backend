package com.hcmus.awad_email.service;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.hcmus.awad_email.dto.email.EmailDetailResponse;
import com.hcmus.awad_email.dto.email.EmailListResponse;
import com.hcmus.awad_email.model.Attachment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class to convert Gmail API messages to our DTOs
 */
@Component
public class GmailMessageConverter {
    
    /**
     * Convert Gmail Message to EmailListResponse
     */
    public EmailListResponse toEmailListResponse(Message message) {
        Map<String, String> headers = extractHeaders(message);
        
        String from = headers.getOrDefault("From", "");
        String fromName = extractName(from);
        String fromEmail = extractEmail(from);
        
        String subject = headers.getOrDefault("Subject", "(No Subject)");
        String snippet = message.getSnippet() != null ? message.getSnippet() : "";
        
        // Check if message has attachments
        boolean hasAttachments = hasAttachments(message);
        
        // Check labels for read/starred status
        List<String> labelIds = message.getLabelIds() != null ? message.getLabelIds() : Collections.emptyList();
        boolean isRead = !labelIds.contains("UNREAD");
        boolean isStarred = labelIds.contains("STARRED");
        boolean isImportant = labelIds.contains("IMPORTANT");
        
        // Convert timestamp
        LocalDateTime receivedAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(message.getInternalDate()),
                ZoneId.systemDefault()
        );
        
        return EmailListResponse.builder()
                .id(message.getId())
                .from(fromEmail)
                .fromName(fromName)
                .subject(subject)
                .preview(snippet)
                .isRead(isRead)
                .isStarred(isStarred)
                .isImportant(isImportant)
                .hasAttachments(hasAttachments)
                .receivedAt(receivedAt)
                .build();
    }
    
    /**
     * Convert Gmail Message to EmailDetailResponse
     */
    public EmailDetailResponse toEmailDetailResponse(Message message) {
        Map<String, String> headers = extractHeaders(message);
        
        String from = headers.getOrDefault("From", "");
        String fromName = extractName(from);
        String fromEmail = extractEmail(from);
        
        String subject = headers.getOrDefault("Subject", "(No Subject)");
        
        // Parse recipients
        List<String> to = parseEmailList(headers.getOrDefault("To", ""));
        List<String> cc = parseEmailList(headers.getOrDefault("Cc", ""));
        List<String> bcc = parseEmailList(headers.getOrDefault("Bcc", ""));
        
        // Extract body
        String body = extractBody(message);
        
        // Extract attachments
        List<Attachment> attachments = extractAttachments(message);
        
        // Check labels
        List<String> labelIds = message.getLabelIds() != null ? message.getLabelIds() : Collections.emptyList();
        boolean isRead = !labelIds.contains("UNREAD");
        boolean isStarred = labelIds.contains("STARRED");
        boolean isImportant = labelIds.contains("IMPORTANT");
        
        // Convert timestamps
        LocalDateTime receivedAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(message.getInternalDate()),
                ZoneId.systemDefault()
        );
        
        return EmailDetailResponse.builder()
                .id(message.getId())
                .from(fromEmail)
                .fromName(fromName)
                .to(to)
                .cc(cc)
                .bcc(bcc)
                .subject(subject)
                .body(body)
                .isRead(isRead)
                .isStarred(isStarred)
                .isImportant(isImportant)
                .attachments(attachments)
                .receivedAt(receivedAt)
                .sentAt(receivedAt)
                .build();
    }
    
    /**
     * Extract headers from Gmail message
     */
    private Map<String, String> extractHeaders(Message message) {
        Map<String, String> headers = new HashMap<>();
        
        if (message.getPayload() != null && message.getPayload().getHeaders() != null) {
            for (MessagePartHeader header : message.getPayload().getHeaders()) {
                headers.put(header.getName(), header.getValue());
            }
        }
        
        return headers;
    }
    
    /**
     * Extract email body from message
     */
    private String extractBody(Message message) {
        if (message.getPayload() == null) {
            return "";
        }
        
        return extractBodyFromPart(message.getPayload());
    }
    
    private String extractBodyFromPart(MessagePart part) {
        if (part.getBody() != null && part.getBody().getData() != null) {
            byte[] bodyBytes = Base64.getUrlDecoder().decode(part.getBody().getData());
            return new String(bodyBytes);
        }
        
        if (part.getParts() != null) {
            for (MessagePart subPart : part.getParts()) {
                String mimeType = subPart.getMimeType();
                if ("text/html".equals(mimeType) || "text/plain".equals(mimeType)) {
                    String body = extractBodyFromPart(subPart);
                    if (!body.isEmpty()) {
                        return body;
                    }
                }
            }
        }
        
        return "";
    }
    
    /**
     * Check if message has attachments
     */
    private boolean hasAttachments(Message message) {
        if (message.getPayload() == null || message.getPayload().getParts() == null) {
            return false;
        }
        
        return message.getPayload().getParts().stream()
                .anyMatch(part -> part.getFilename() != null && !part.getFilename().isEmpty());
    }
    
    /**
     * Extract attachments from message
     */
    private List<Attachment> extractAttachments(Message message) {
        List<Attachment> attachments = new ArrayList<>();
        
        if (message.getPayload() != null && message.getPayload().getParts() != null) {
            for (MessagePart part : message.getPayload().getParts()) {
                if (part.getFilename() != null && !part.getFilename().isEmpty()) {
                    Attachment attachment = Attachment.builder()
                            .id(part.getBody().getAttachmentId())
                            .filename(part.getFilename())
                            .mimeType(part.getMimeType())
                            .size(part.getBody().getSize())
                            .url("/api/attachments/" + message.getId() + "/" + part.getBody().getAttachmentId())
                            .build();
                    attachments.add(attachment);
                }
            }
        }
        
        return attachments;
    }
    
    /**
     * Extract name from email address string (e.g., "John Doe <john@example.com>")
     */
    private String extractName(String emailString) {
        if (emailString == null || emailString.isEmpty()) {
            return "";
        }
        
        int startBracket = emailString.indexOf('<');
        if (startBracket > 0) {
            return emailString.substring(0, startBracket).trim().replaceAll("\"", "");
        }
        
        return emailString;
    }
    
    /**
     * Extract email address from string (e.g., "John Doe <john@example.com>")
     */
    private String extractEmail(String emailString) {
        if (emailString == null || emailString.isEmpty()) {
            return "";
        }
        
        int startBracket = emailString.indexOf('<');
        int endBracket = emailString.indexOf('>');
        
        if (startBracket >= 0 && endBracket > startBracket) {
            return emailString.substring(startBracket + 1, endBracket).trim();
        }
        
        return emailString.trim();
    }
    
    /**
     * Parse comma-separated email list
     */
    private List<String> parseEmailList(String emailListString) {
        if (emailListString == null || emailListString.isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(emailListString.split(","))
                .map(this::extractEmail)
                .filter(email -> !email.isEmpty())
                .collect(Collectors.toList());
    }

    // ==================== Public Helper Methods ====================

    /**
     * Get a specific header value from a Gmail message.
     */
    public String getHeader(Message message, String headerName) {
        Map<String, String> headers = extractHeaders(message);
        return headers.getOrDefault(headerName, null);
    }

    /**
     * Get the body content of a Gmail message.
     */
    public String getBody(Message message) {
        return extractBody(message);
    }

    /**
     * Get the received timestamp of a Gmail message.
     */
    public LocalDateTime getReceivedAt(Message message) {
        if (message.getInternalDate() == null) {
            return null;
        }
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(message.getInternalDate()),
                ZoneId.systemDefault()
        );
    }

    /**
     * Check if a Gmail message is read.
     */
    public boolean isRead(Message message) {
        List<String> labelIds = message.getLabelIds() != null ? message.getLabelIds() : Collections.emptyList();
        return !labelIds.contains("UNREAD");
    }

    /**
     * Check if a Gmail message is starred.
     */
    public boolean isStarred(Message message) {
        List<String> labelIds = message.getLabelIds() != null ? message.getLabelIds() : Collections.emptyList();
        return labelIds.contains("STARRED");
    }
}

