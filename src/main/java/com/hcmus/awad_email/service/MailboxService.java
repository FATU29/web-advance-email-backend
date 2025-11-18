package com.hcmus.awad_email.service;

import com.hcmus.awad_email.dto.email.MailboxResponse;
import com.hcmus.awad_email.exception.ResourceNotFoundException;
import com.hcmus.awad_email.model.Mailbox;
import com.hcmus.awad_email.repository.EmailRepository;
import com.hcmus.awad_email.repository.MailboxRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MailboxService {
    
    @Autowired
    private MailboxRepository mailboxRepository;
    
    @Autowired
    private EmailRepository emailRepository;
    
    @Transactional
    public void initializeDefaultMailboxes(String userId) {
        Mailbox.MailboxType[] defaultTypes = {
            Mailbox.MailboxType.INBOX,
            Mailbox.MailboxType.SENT,
            Mailbox.MailboxType.DRAFTS,
            Mailbox.MailboxType.TRASH,
            Mailbox.MailboxType.ARCHIVE,
            Mailbox.MailboxType.STARRED
        };
        
        for (Mailbox.MailboxType type : defaultTypes) {
            Mailbox mailbox = Mailbox.builder()
                    .userId(userId)
                    .name(type.name().charAt(0) + type.name().substring(1).toLowerCase())
                    .type(type)
                    .unreadCount(0)
                    .totalCount(0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            mailboxRepository.save(mailbox);
        }
    }
    
    public List<MailboxResponse> getUserMailboxes(String userId) {
        List<Mailbox> mailboxes = mailboxRepository.findByUserId(userId);
        
        // Update unread counts
        for (Mailbox mailbox : mailboxes) {
            long unreadCount = emailRepository.countByUserIdAndMailboxIdAndIsRead(
                    userId, mailbox.getId(), false);
            mailbox.setUnreadCount((int) unreadCount);
        }
        
        return mailboxes.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    public MailboxResponse getMailboxById(String userId, String mailboxId) {
        Mailbox mailbox = mailboxRepository.findByIdAndUserId(mailboxId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Mailbox not found"));
        
        long unreadCount = emailRepository.countByUserIdAndMailboxIdAndIsRead(
                userId, mailbox.getId(), false);
        mailbox.setUnreadCount((int) unreadCount);
        
        return convertToResponse(mailbox);
    }
    
    public Mailbox getMailboxByType(String userId, Mailbox.MailboxType type) {
        return mailboxRepository.findByUserIdAndType(userId, type)
                .orElseThrow(() -> new ResourceNotFoundException("Mailbox not found"));
    }
    
    private MailboxResponse convertToResponse(Mailbox mailbox) {
        return MailboxResponse.builder()
                .id(mailbox.getId())
                .name(mailbox.getName())
                .type(mailbox.getType())
                .unreadCount(mailbox.getUnreadCount())
                .totalCount(mailbox.getTotalCount())
                .createdAt(mailbox.getCreatedAt())
                .updatedAt(mailbox.getUpdatedAt())
                .build();
    }
}

