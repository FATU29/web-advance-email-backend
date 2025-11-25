package com.hcmus.awad_email.service;

import com.google.api.services.gmail.model.Message;
import com.hcmus.awad_email.dto.common.PageResponse;
import com.hcmus.awad_email.dto.email.*;
import com.hcmus.awad_email.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmailService {

    @Autowired
    private GmailService gmailService;

    @Autowired
    private GmailMessageConverter gmailMessageConverter;

    public PageResponse<EmailListResponse> getEmailsByMailbox(String userId, String mailboxId,
                                                               int page, int size) {
        // Gmail API is required - throw error if not connected
        if (!gmailService.isGmailConnected(userId)) {
            throw new ResourceNotFoundException("Gmail not connected. Please connect your Gmail account first.");
        }

        return getEmailsFromGmail(userId, mailboxId, page, size);
    }
    
    public EmailDetailResponse getEmailById(String userId, String emailId) {
        // Gmail API is required - throw error if not connected
        if (!gmailService.isGmailConnected(userId)) {
            throw new ResourceNotFoundException("Gmail not connected. Please connect your Gmail account first.");
        }

        Message gmailMessage = gmailService.getMessage(userId, emailId);
        return gmailMessageConverter.toEmailDetailResponse(gmailMessage);
    }

    /**
     * Fetch emails from Gmail API
     */
    private PageResponse<EmailListResponse> getEmailsFromGmail(String userId, String mailboxId,
                                                                 int page, int size) {
        // Gmail uses label IDs - we need to map our mailbox ID to Gmail label ID
        // For now, use the mailboxId directly as it should be a Gmail label ID

        List<Message> messages = gmailService.listMessages(userId, mailboxId, (long) size, null);

        List<EmailListResponse> content = messages.stream()
                .map(gmailMessageConverter::toEmailListResponse)
                .collect(Collectors.toList());

        // Gmail API doesn't provide total count easily, so we approximate
        return PageResponse.<EmailListResponse>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements((long) content.size())
                .totalPages(1)
                .last(content.size() < size)
                .build();
    }
    
    @Transactional
    public void markAsRead(String userId, List<String> emailIds) {
        // Gmail API is required - throw error if not connected
        if (!gmailService.isGmailConnected(userId)) {
            throw new ResourceNotFoundException("Gmail not connected. Please connect your Gmail account first.");
        }

        for (String emailId : emailIds) {
            gmailService.modifyMessage(userId, emailId, null, Arrays.asList("UNREAD"));
        }
    }

    @Transactional
    public void markAsUnread(String userId, List<String> emailIds) {
        // Gmail API is required - throw error if not connected
        if (!gmailService.isGmailConnected(userId)) {
            throw new ResourceNotFoundException("Gmail not connected. Please connect your Gmail account first.");
        }

        for (String emailId : emailIds) {
            gmailService.modifyMessage(userId, emailId, Arrays.asList("UNREAD"), null);
        }
    }
    
    @Transactional
    public void toggleStar(String userId, List<String> emailIds, boolean starred) {
        // Gmail API is required - throw error if not connected
        if (!gmailService.isGmailConnected(userId)) {
            throw new ResourceNotFoundException("Gmail not connected. Please connect your Gmail account first.");
        }

        for (String emailId : emailIds) {
            if (starred) {
                gmailService.modifyMessage(userId, emailId, Arrays.asList("STARRED"), null);
            } else {
                gmailService.modifyMessage(userId, emailId, null, Arrays.asList("STARRED"));
            }
        }
    }
    
    @Transactional
    public void deleteEmails(String userId, List<String> emailIds) {
        // Gmail API is required - throw error if not connected
        if (!gmailService.isGmailConnected(userId)) {
            throw new ResourceNotFoundException("Gmail not connected. Please connect your Gmail account first.");
        }

        for (String emailId : emailIds) {
            gmailService.trashMessage(userId, emailId);
        }
    }
    
    @Transactional
    public void archiveEmails(String userId, List<String> emailIds) {
        // Gmail API is required - throw error if not connected
        if (!gmailService.isGmailConnected(userId)) {
            throw new ResourceNotFoundException("Gmail not connected. Please connect your Gmail account first.");
        }

        // Archive emails by removing INBOX label and adding ARCHIVE label
        for (String emailId : emailIds) {
            gmailService.modifyMessage(userId, emailId, Arrays.asList("ARCHIVE"), Arrays.asList("INBOX"));
        }
    }

    /**
     * Send an email via Gmail API
     */
    @Transactional
    public void sendEmail(String userId, SendEmailRequest request) {
        // Check if Gmail is connected
        if (!gmailService.isGmailConnected(userId)) {
            throw new ResourceNotFoundException("Gmail not connected. Please connect your Gmail account first.");
        }

        // Send via Gmail API
        String to = String.join(", ", request.getTo());
        gmailService.sendMessage(userId, to, request.getSubject(), request.getBody());
    }

    /**
     * Reply to an email via Gmail API
     */
    @Transactional
    public void replyToEmail(String userId, String emailId, ReplyEmailRequest request) {
        // Check if Gmail is connected
        if (!gmailService.isGmailConnected(userId)) {
            throw new ResourceNotFoundException("Gmail not connected. Please connect your Gmail account first.");
        }

        // Get original email from Gmail API
        Message originalMessage = gmailService.getMessage(userId, emailId);
        EmailDetailResponse originalEmail = gmailMessageConverter.toEmailDetailResponse(originalMessage);

        // Prepare reply
        String to = originalEmail.getFrom();
        String subject = originalEmail.getSubject().startsWith("Re:")
                ? originalEmail.getSubject()
                : "Re: " + originalEmail.getSubject();

        // Send reply via Gmail API
        gmailService.sendMessage(userId, to, subject, request.getBody());
    }
}

