package com.hcmus.awad_email.service;

import com.google.api.services.gmail.model.Label;
import com.hcmus.awad_email.dto.email.MailboxResponse;
import com.hcmus.awad_email.exception.ResourceNotFoundException;
import com.hcmus.awad_email.model.Mailbox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MailboxService {

    @Autowired
    private GmailService gmailService;
    

    
    public List<MailboxResponse> getUserMailboxes(String userId) {
        // Gmail API is required - throw error if not connected
        if (!gmailService.isGmailConnected(userId)) {
            throw new ResourceNotFoundException("Gmail not connected. Please connect your Gmail account first.");
        }

        return getMailboxesFromGmail(userId);
    }

    /**
     * Fetch mailboxes from Gmail labels
     */
    private List<MailboxResponse> getMailboxesFromGmail(String userId) {
        List<Label> labels = gmailService.listLabels(userId);
        List<MailboxResponse> mailboxes = new ArrayList<>();

        // Filter and convert Gmail labels to mailboxes
        for (Label label : labels) {
            // Only include system labels and user labels
            if (label.getType() != null && (label.getType().equals("system") || label.getType().equals("user"))) {
                MailboxResponse mailbox = MailboxResponse.builder()
                        .id(label.getId())
                        .name(label.getName())
                        .type(mapGmailLabelToType(label.getId()))
                        .unreadCount(label.getMessagesUnread() != null ? label.getMessagesUnread() : 0)
                        .totalCount(label.getMessagesTotal() != null ? label.getMessagesTotal() : 0)
                        .build();
                mailboxes.add(mailbox);
            }
        }

        return mailboxes;
    }

    /**
     * Map Gmail label ID to our MailboxType
     */
    private Mailbox.MailboxType mapGmailLabelToType(String labelId) {
        return switch (labelId) {
            case "INBOX" -> Mailbox.MailboxType.INBOX;
            case "SENT" -> Mailbox.MailboxType.SENT;
            case "DRAFT" -> Mailbox.MailboxType.DRAFTS;
            case "TRASH" -> Mailbox.MailboxType.TRASH;
            case "STARRED" -> Mailbox.MailboxType.STARRED;
            default -> Mailbox.MailboxType.CUSTOM;
        };
    }
    

}

