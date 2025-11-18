package com.hcmus.awad_email.service;

import com.hcmus.awad_email.dto.common.PageResponse;
import com.hcmus.awad_email.dto.email.EmailDetailResponse;
import com.hcmus.awad_email.dto.email.EmailListResponse;
import com.hcmus.awad_email.exception.ResourceNotFoundException;
import com.hcmus.awad_email.model.Attachment;
import com.hcmus.awad_email.model.Email;
import com.hcmus.awad_email.model.Mailbox;
import com.hcmus.awad_email.repository.EmailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmailService {
    
    @Autowired
    private EmailRepository emailRepository;
    
    @Autowired
    private MailboxService mailboxService;
    
    public PageResponse<EmailListResponse> getEmailsByMailbox(String userId, String mailboxId, 
                                                               int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt"));
        Page<Email> emailPage = emailRepository.findByUserIdAndMailboxId(userId, mailboxId, pageable);
        
        List<EmailListResponse> content = emailPage.getContent().stream()
                .map(this::convertToListResponse)
                .collect(Collectors.toList());
        
        return PageResponse.<EmailListResponse>builder()
                .content(content)
                .page(emailPage.getNumber())
                .size(emailPage.getSize())
                .totalElements(emailPage.getTotalElements())
                .totalPages(emailPage.getTotalPages())
                .last(emailPage.isLast())
                .build();
    }
    
    public EmailDetailResponse getEmailById(String userId, String emailId) {
        Email email = emailRepository.findByIdAndUserId(emailId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Email not found"));
        
        return convertToDetailResponse(email);
    }
    
    @Transactional
    public void markAsRead(String userId, List<String> emailIds) {
        for (String emailId : emailIds) {
            Email email = emailRepository.findByIdAndUserId(emailId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Email not found: " + emailId));
            email.setRead(true);
            email.setUpdatedAt(LocalDateTime.now());
            emailRepository.save(email);
        }
    }
    
    @Transactional
    public void markAsUnread(String userId, List<String> emailIds) {
        for (String emailId : emailIds) {
            Email email = emailRepository.findByIdAndUserId(emailId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Email not found: " + emailId));
            email.setRead(false);
            email.setUpdatedAt(LocalDateTime.now());
            emailRepository.save(email);
        }
    }
    
    @Transactional
    public void toggleStar(String userId, List<String> emailIds, boolean starred) {
        for (String emailId : emailIds) {
            Email email = emailRepository.findByIdAndUserId(emailId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Email not found: " + emailId));
            email.setStarred(starred);
            email.setUpdatedAt(LocalDateTime.now());
            emailRepository.save(email);
            
            // If starring, also add to starred mailbox
            if (starred) {
                Mailbox starredMailbox = mailboxService.getMailboxByType(userId, Mailbox.MailboxType.STARRED);
                // In a real implementation, we might create a copy or reference
            }
        }
    }
    
    @Transactional
    public void deleteEmails(String userId, List<String> emailIds) {
        Mailbox trashMailbox = mailboxService.getMailboxByType(userId, Mailbox.MailboxType.TRASH);
        
        for (String emailId : emailIds) {
            Email email = emailRepository.findByIdAndUserId(emailId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Email not found: " + emailId));
            
            // If already in trash, permanently delete
            if (email.getMailboxId().equals(trashMailbox.getId())) {
                emailRepository.delete(email);
            } else {
                // Move to trash
                email.setMailboxId(trashMailbox.getId());
                email.setUpdatedAt(LocalDateTime.now());
                emailRepository.save(email);
            }
        }
    }
    
    @Transactional
    public void archiveEmails(String userId, List<String> emailIds) {
        Mailbox archiveMailbox = mailboxService.getMailboxByType(userId, Mailbox.MailboxType.ARCHIVE);
        
        for (String emailId : emailIds) {
            Email email = emailRepository.findByIdAndUserId(emailId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Email not found: " + emailId));
            email.setMailboxId(archiveMailbox.getId());
            email.setUpdatedAt(LocalDateTime.now());
            emailRepository.save(email);
        }
    }
    
    /**
     * Initialize mock emails for a user's inbox
     * This will be replaced with Gmail API integration in the future
     */
    @Transactional
    public void initializeMockEmails(String userId) {
        Mailbox inbox = mailboxService.getMailboxByType(userId, Mailbox.MailboxType.INBOX);
        
        List<Email> mockEmails = createMockEmails(userId, inbox.getId());
        emailRepository.saveAll(mockEmails);
    }
    
    private List<Email> createMockEmails(String userId, String inboxId) {
        List<Email> emails = new ArrayList<>();
        
        // Mock email 1
        emails.add(Email.builder()
                .userId(userId)
                .mailboxId(inboxId)
                .from("john.doe@example.com")
                .fromName("John Doe")
                .to(Arrays.asList("user@example.com"))
                .subject("Welcome to our platform!")
                .body("<p>Hi there,</p><p>Welcome to our email platform. We're excited to have you on board!</p><p>Best regards,<br>The Team</p>")
                .preview("Hi there, Welcome to our email platform. We're excited to have you on board!")
                .isRead(false)
                .isStarred(false)
                .isImportant(true)
                .attachments(new ArrayList<>())
                .receivedAt(LocalDateTime.now().minusHours(2))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
        
        // Mock email 2
        emails.add(Email.builder()
                .userId(userId)
                .mailboxId(inboxId)
                .from("support@company.com")
                .fromName("Customer Support")
                .to(Arrays.asList("user@example.com"))
                .subject("Your account has been verified")
                .body("<p>Hello,</p><p>Your account has been successfully verified. You can now access all features.</p>")
                .preview("Hello, Your account has been successfully verified. You can now access all features.")
                .isRead(true)
                .isStarred(false)
                .isImportant(false)
                .attachments(new ArrayList<>())
                .receivedAt(LocalDateTime.now().minusDays(1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
        
        // Mock email 3 with attachment
        List<Attachment> attachments = new ArrayList<>();
        attachments.add(Attachment.builder()
                .id("att1")
                .filename("document.pdf")
                .mimeType("application/pdf")
                .size(1024000)
                .url("/api/attachments/att1")
                .build());
        
        emails.add(Email.builder()
                .userId(userId)
                .mailboxId(inboxId)
                .from("newsletter@tech.com")
                .fromName("Tech Newsletter")
                .to(Arrays.asList("user@example.com"))
                .cc(Arrays.asList("team@example.com"))
                .subject("Weekly Tech Digest - January 2024")
                .body("<p>Here's your weekly roundup of tech news...</p><p>Check out the attached document for more details.</p>")
                .preview("Here's your weekly roundup of tech news... Check out the attached document for more details.")
                .isRead(false)
                .isStarred(true)
                .isImportant(false)
                .attachments(attachments)
                .receivedAt(LocalDateTime.now().minusHours(5))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
        
        // Mock email 4
        emails.add(Email.builder()
                .userId(userId)
                .mailboxId(inboxId)
                .from("noreply@service.com")
                .fromName("Service Notifications")
                .to(Arrays.asList("user@example.com"))
                .subject("Password change confirmation")
                .body("<p>Your password was recently changed. If this wasn't you, please contact support immediately.</p>")
                .preview("Your password was recently changed. If this wasn't you, please contact support immediately.")
                .isRead(true)
                .isStarred(false)
                .isImportant(true)
                .attachments(new ArrayList<>())
                .receivedAt(LocalDateTime.now().minusDays(2))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
        
        // Mock email 5
        emails.add(Email.builder()
                .userId(userId)
                .mailboxId(inboxId)
                .from("team@project.com")
                .fromName("Project Team")
                .to(Arrays.asList("user@example.com"))
                .cc(Arrays.asList("manager@example.com", "lead@example.com"))
                .subject("Project Update - Q1 2024")
                .body("<p>Team,</p><p>Here's the latest update on our Q1 projects. Please review and provide feedback.</p><p>Thanks!</p>")
                .preview("Team, Here's the latest update on our Q1 projects. Please review and provide feedback.")
                .isRead(false)
                .isStarred(false)
                .isImportant(false)
                .attachments(new ArrayList<>())
                .receivedAt(LocalDateTime.now().minusMinutes(30))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
        
        return emails;
    }
    
    private EmailListResponse convertToListResponse(Email email) {
        return EmailListResponse.builder()
                .id(email.getId())
                .from(email.getFrom())
                .fromName(email.getFromName())
                .subject(email.getSubject())
                .preview(email.getPreview())
                .isRead(email.isRead())
                .isStarred(email.isStarred())
                .isImportant(email.isImportant())
                .hasAttachments(email.getAttachments() != null && !email.getAttachments().isEmpty())
                .receivedAt(email.getReceivedAt())
                .build();
    }
    
    private EmailDetailResponse convertToDetailResponse(Email email) {
        return EmailDetailResponse.builder()
                .id(email.getId())
                .from(email.getFrom())
                .fromName(email.getFromName())
                .to(email.getTo())
                .cc(email.getCc())
                .bcc(email.getBcc())
                .subject(email.getSubject())
                .body(email.getBody())
                .isRead(email.isRead())
                .isStarred(email.isStarred())
                .isImportant(email.isImportant())
                .attachments(email.getAttachments())
                .receivedAt(email.getReceivedAt())
                .sentAt(email.getSentAt())
                .build();
    }
}

