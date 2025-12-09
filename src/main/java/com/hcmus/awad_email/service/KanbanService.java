package com.hcmus.awad_email.service;

import com.google.api.services.gmail.model.Message;
import com.hcmus.awad_email.dto.kanban.*;
import com.hcmus.awad_email.exception.BadRequestException;
import com.hcmus.awad_email.exception.ResourceNotFoundException;
import com.hcmus.awad_email.model.EmailKanbanStatus;
import com.hcmus.awad_email.model.KanbanColumn;
import com.hcmus.awad_email.repository.EmailKanbanStatusRepository;
import com.hcmus.awad_email.repository.KanbanColumnRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KanbanService {
    
    @Autowired
    private KanbanColumnRepository columnRepository;
    
    @Autowired
    private EmailKanbanStatusRepository emailStatusRepository;
    
    @Autowired
    private GmailService gmailService;
    
    @Autowired
    private GmailMessageConverter gmailMessageConverter;
    
    @Autowired
    private AISummarizationService aiSummarizationService;
    
    // ==================== Column Operations ====================
    
    /**
     * Initialize default columns for a new user.
     */
    @Transactional
    public List<KanbanColumn> initializeDefaultColumns(String userId) {
        // Check if user already has columns
        if (columnRepository.countByUserId(userId) > 0) {
            return columnRepository.findByUserIdOrderByOrderAsc(userId);
        }
        
        List<KanbanColumn> defaultColumns = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        defaultColumns.add(KanbanColumn.builder()
                .userId(userId).name("Inbox").type(KanbanColumn.ColumnType.INBOX)
                .order(0).color("#4285F4").isDefault(true).createdAt(now).updatedAt(now).build());
        
        defaultColumns.add(KanbanColumn.builder()
                .userId(userId).name("To Do").type(KanbanColumn.ColumnType.TODO)
                .order(1).color("#FBBC04").isDefault(true).createdAt(now).updatedAt(now).build());
        
        defaultColumns.add(KanbanColumn.builder()
                .userId(userId).name("In Progress").type(KanbanColumn.ColumnType.IN_PROGRESS)
                .order(2).color("#FF6D01").isDefault(true).createdAt(now).updatedAt(now).build());
        
        defaultColumns.add(KanbanColumn.builder()
                .userId(userId).name("Done").type(KanbanColumn.ColumnType.DONE)
                .order(3).color("#34A853").isDefault(true).createdAt(now).updatedAt(now).build());
        
        defaultColumns.add(KanbanColumn.builder()
                .userId(userId).name("Snoozed").type(KanbanColumn.ColumnType.SNOOZED)
                .order(4).color("#9E9E9E").isDefault(true).createdAt(now).updatedAt(now).build());
        
        return columnRepository.saveAll(defaultColumns);
    }
    
    /**
     * Get all columns for a user.
     */
    public List<KanbanColumnResponse> getColumns(String userId) {
        List<KanbanColumn> columns = columnRepository.findByUserIdOrderByOrderAsc(userId);
        if (columns.isEmpty()) {
            columns = initializeDefaultColumns(userId);
        }
        return columns.stream().map(this::toColumnResponse).collect(Collectors.toList());
    }
    
    /**
     * Create a new custom column.
     */
    @Transactional
    public KanbanColumnResponse createColumn(String userId, CreateColumnRequest request) {
        if (columnRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new BadRequestException("Column with name '" + request.getName() + "' already exists");
        }
        
        int order = request.getOrder() != null ? request.getOrder() 
                : (int) columnRepository.countByUserId(userId);
        
        KanbanColumn column = KanbanColumn.builder()
                .userId(userId).name(request.getName()).type(KanbanColumn.ColumnType.CUSTOM)
                .order(order).color(request.getColor()).isDefault(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        
        column = columnRepository.save(column);
        log.info("Created new column '{}' for user {}", request.getName(), userId);
        return toColumnResponse(column);
    }
    
    /**
     * Update a column.
     */
    @Transactional
    public KanbanColumnResponse updateColumn(String userId, String columnId, UpdateColumnRequest request) {
        KanbanColumn column = columnRepository.findByIdAndUserId(columnId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Column not found"));
        
        if (request.getName() != null && !request.getName().equals(column.getName())) {
            if (columnRepository.existsByUserIdAndName(userId, request.getName())) {
                throw new BadRequestException("Column with name '" + request.getName() + "' already exists");
            }
            column.setName(request.getName());
        }
        if (request.getColor() != null) column.setColor(request.getColor());
        if (request.getOrder() != null) column.setOrder(request.getOrder());
        column.setUpdatedAt(LocalDateTime.now());
        
        column = columnRepository.save(column);
        return toColumnResponse(column);
    }
    
    /**
     * Delete a custom column.
     */
    @Transactional
    public void deleteColumn(String userId, String columnId) {
        KanbanColumn column = columnRepository.findByIdAndUserId(columnId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Column not found"));
        
        if (column.isDefault()) {
            throw new BadRequestException("Cannot delete default columns");
        }
        
        // Move emails to Inbox before deleting
        KanbanColumn inboxColumn = columnRepository.findByUserIdAndType(userId, KanbanColumn.ColumnType.INBOX)
                .orElseThrow(() -> new ResourceNotFoundException("Inbox column not found"));
        
        List<EmailKanbanStatus> emails = emailStatusRepository
                .findByUserIdAndColumnIdOrderByOrderInColumnAsc(userId, columnId);
        for (EmailKanbanStatus email : emails) {
            email.setColumnId(inboxColumn.getId());
            email.setUpdatedAt(LocalDateTime.now());
        }
        emailStatusRepository.saveAll(emails);
        
        columnRepository.deleteByIdAndUserId(columnId, userId);
        log.info("Deleted column '{}' for user {}", column.getName(), userId);
    }
    
    private KanbanColumnResponse toColumnResponse(KanbanColumn column) {
        long emailCount = emailStatusRepository.countByUserIdAndColumnId(column.getUserId(), column.getId());
        return KanbanColumnResponse.builder()
                .id(column.getId()).name(column.getName()).type(column.getType())
                .order(column.getOrder()).color(column.getColor()).isDefault(column.isDefault())
                .emailCount(emailCount).createdAt(column.getCreatedAt()).updatedAt(column.getUpdatedAt())
                .build();
    }

    // ==================== Email Operations ====================

    /**
     * Get the full Kanban board with all columns and emails.
     */
    public KanbanBoardResponse getBoard(String userId) {
        List<KanbanColumnResponse> columns = getColumns(userId);
        Map<String, List<KanbanEmailResponse>> emailsByColumn = new HashMap<>();

        for (KanbanColumnResponse column : columns) {
            List<EmailKanbanStatus> emails = emailStatusRepository
                    .findByUserIdAndColumnIdOrderByOrderInColumnAsc(userId, column.getId());
            emailsByColumn.put(column.getId(),
                    emails.stream().map(this::toEmailResponse).collect(Collectors.toList()));
        }

        return KanbanBoardResponse.builder()
                .columns(columns)
                .emailsByColumn(emailsByColumn)
                .build();
    }

    /**
     * Get emails in a specific column.
     */
    public List<KanbanEmailResponse> getEmailsInColumn(String userId, String columnId) {
        columnRepository.findByIdAndUserId(columnId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Column not found"));

        List<EmailKanbanStatus> emails = emailStatusRepository
                .findByUserIdAndColumnIdOrderByOrderInColumnAsc(userId, columnId);
        return emails.stream().map(this::toEmailResponse).collect(Collectors.toList());
    }

    /**
     * Add an email to the Kanban board.
     */
    @Transactional
    public KanbanEmailResponse addEmailToKanban(String userId, AddEmailToKanbanRequest request) {
        // Check if email already exists in Kanban
        if (emailStatusRepository.existsByUserIdAndEmailId(userId, request.getEmailId())) {
            throw new BadRequestException("Email is already on the Kanban board");
        }

        // Determine target column
        String columnId = request.getColumnId();
        if (columnId == null || columnId.isEmpty()) {
            KanbanColumn inboxColumn = columnRepository.findByUserIdAndType(userId, KanbanColumn.ColumnType.INBOX)
                    .orElseGet(() -> initializeDefaultColumns(userId).get(0));
            columnId = inboxColumn.getId();
        } else {
            columnRepository.findByIdAndUserId(columnId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Column not found"));
        }

        // Fetch email details from Gmail
        Message gmailMessage = gmailService.getMessage(userId, request.getEmailId());

        // Extract email metadata
        String subject = gmailMessageConverter.getHeader(gmailMessage, "Subject");
        String from = gmailMessageConverter.getHeader(gmailMessage, "From");
        String fromName = extractName(from);
        String fromEmail = extractEmail(from);
        String body = gmailMessageConverter.getBody(gmailMessage);
        String preview = body != null ? body.substring(0, Math.min(200, body.length())) : "";

        // Generate summary if requested
        String summary = null;
        LocalDateTime summaryGeneratedAt = null;
        if (request.isGenerateSummary()) {
            summary = aiSummarizationService.generateSummary(subject, from, body);
            if (summary != null) {
                summaryGeneratedAt = LocalDateTime.now();
            }
        }

        // Get order in column
        int order = (int) emailStatusRepository.countByUserIdAndColumnId(userId, columnId);

        LocalDateTime now = LocalDateTime.now();
        EmailKanbanStatus status = EmailKanbanStatus.builder()
                .userId(userId)
                .emailId(request.getEmailId())
                .columnId(columnId)
                .orderInColumn(order)
                .subject(subject)
                .fromEmail(fromEmail)
                .fromName(fromName)
                .preview(preview)
                .receivedAt(gmailMessageConverter.getReceivedAt(gmailMessage))
                .isRead(gmailMessageConverter.isRead(gmailMessage))
                .isStarred(gmailMessageConverter.isStarred(gmailMessage))
                .summary(summary)
                .summaryGeneratedAt(summaryGeneratedAt)
                .snoozed(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        status = emailStatusRepository.save(status);
        log.info("Added email {} to Kanban board for user {}", request.getEmailId(), userId);
        return toEmailResponse(status);
    }

    /**
     * Move an email to a different column (drag-and-drop).
     */
    @Transactional
    public KanbanEmailResponse moveEmail(String userId, MoveEmailRequest request) {
        EmailKanbanStatus status = emailStatusRepository.findByUserIdAndEmailId(userId, request.getEmailId())
                .orElseThrow(() -> new ResourceNotFoundException("Email not found on Kanban board"));

        KanbanColumn targetColumn = columnRepository.findByIdAndUserId(request.getTargetColumnId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Target column not found"));

        // If moving to Snoozed column without snooze time, reject
        if (targetColumn.getType() == KanbanColumn.ColumnType.SNOOZED && !status.isSnoozed()) {
            throw new BadRequestException("Use the snooze endpoint to move emails to Snoozed column");
        }

        // Update column and order
        status.setColumnId(request.getTargetColumnId());
        status.setOrderInColumn(request.getNewOrder() != null ? request.getNewOrder()
                : (int) emailStatusRepository.countByUserIdAndColumnId(userId, request.getTargetColumnId()));

        // If moving out of Snoozed, clear snooze data
        if (status.isSnoozed() && targetColumn.getType() != KanbanColumn.ColumnType.SNOOZED) {
            status.setSnoozed(false);
            status.setSnoozeUntil(null);
            status.setPreviousColumnId(null);
        }

        status.setUpdatedAt(LocalDateTime.now());
        status = emailStatusRepository.save(status);

        log.info("Moved email {} to column {} for user {}", request.getEmailId(), targetColumn.getName(), userId);
        return toEmailResponse(status);
    }

    private KanbanEmailResponse toEmailResponse(EmailKanbanStatus status) {
        return KanbanEmailResponse.builder()
                .id(status.getId())
                .emailId(status.getEmailId())
                .columnId(status.getColumnId())
                .orderInColumn(status.getOrderInColumn())
                .subject(status.getSubject())
                .fromEmail(status.getFromEmail())
                .fromName(status.getFromName())
                .preview(status.getPreview())
                .receivedAt(status.getReceivedAt())
                .isRead(status.isRead())
                .isStarred(status.isStarred())
                .summary(status.getSummary())
                .summaryGeneratedAt(status.getSummaryGeneratedAt())
                .snoozed(status.isSnoozed())
                .snoozeUntil(status.getSnoozeUntil())
                .createdAt(status.getCreatedAt())
                .updatedAt(status.getUpdatedAt())
                .build();
    }

    private String extractName(String from) {
        if (from == null) return null;
        int idx = from.indexOf('<');
        if (idx > 0) return from.substring(0, idx).trim().replace("\"", "");
        return from;
    }

    private String extractEmail(String from) {
        if (from == null) return null;
        int start = from.indexOf('<');
        int end = from.indexOf('>');
        if (start >= 0 && end > start) return from.substring(start + 1, end);
        return from;
    }

    // ==================== Snooze Operations ====================

    /**
     * Snooze an email until a specific time.
     */
    @Transactional
    public KanbanEmailResponse snoozeEmail(String userId, SnoozeEmailRequest request) {
        EmailKanbanStatus status = emailStatusRepository.findByUserIdAndEmailId(userId, request.getEmailId())
                .orElseThrow(() -> new ResourceNotFoundException("Email not found on Kanban board"));

        // Get or create Snoozed column
        KanbanColumn snoozedColumn = columnRepository.findByUserIdAndType(userId, KanbanColumn.ColumnType.SNOOZED)
                .orElseGet(() -> {
                    initializeDefaultColumns(userId);
                    return columnRepository.findByUserIdAndType(userId, KanbanColumn.ColumnType.SNOOZED)
                            .orElseThrow(() -> new ResourceNotFoundException("Snoozed column not found"));
                });

        // Store previous column for restoration
        status.setPreviousColumnId(status.getColumnId());
        status.setColumnId(snoozedColumn.getId());
        status.setSnoozed(true);
        status.setSnoozeUntil(request.getSnoozeUntil());
        status.setOrderInColumn((int) emailStatusRepository.countByUserIdAndColumnId(userId, snoozedColumn.getId()));
        status.setUpdatedAt(LocalDateTime.now());

        status = emailStatusRepository.save(status);
        log.info("Snoozed email {} until {} for user {}", request.getEmailId(), request.getSnoozeUntil(), userId);
        return toEmailResponse(status);
    }

    /**
     * Unsnooze an email (restore to previous column).
     */
    @Transactional
    public KanbanEmailResponse unsnoozeEmail(String userId, String emailId) {
        EmailKanbanStatus status = emailStatusRepository.findByUserIdAndEmailId(userId, emailId)
                .orElseThrow(() -> new ResourceNotFoundException("Email not found on Kanban board"));

        if (!status.isSnoozed()) {
            throw new BadRequestException("Email is not snoozed");
        }

        // Restore to previous column or Inbox
        String targetColumnId = status.getPreviousColumnId();
        if (targetColumnId == null) {
            KanbanColumn inboxColumn = columnRepository.findByUserIdAndType(userId, KanbanColumn.ColumnType.INBOX)
                    .orElseThrow(() -> new ResourceNotFoundException("Inbox column not found"));
            targetColumnId = inboxColumn.getId();
        }

        status.setColumnId(targetColumnId);
        status.setSnoozed(false);
        status.setSnoozeUntil(null);
        status.setPreviousColumnId(null);
        status.setOrderInColumn((int) emailStatusRepository.countByUserIdAndColumnId(userId, targetColumnId));
        status.setUpdatedAt(LocalDateTime.now());

        status = emailStatusRepository.save(status);
        log.info("Unsnoozed email {} for user {}", emailId, userId);
        return toEmailResponse(status);
    }

    /**
     * Process expired snoozes (called by scheduler).
     */
    @Transactional
    public void processExpiredSnoozes() {
        List<EmailKanbanStatus> expiredSnoozes = emailStatusRepository
                .findBySnoozedTrueAndSnoozeUntilBefore(LocalDateTime.now());

        for (EmailKanbanStatus status : expiredSnoozes) {
            try {
                unsnoozeEmail(status.getUserId(), status.getEmailId());
                log.info("Auto-unsnoozed email {} for user {}", status.getEmailId(), status.getUserId());
            } catch (Exception e) {
                log.error("Failed to auto-unsnooze email {}: {}", status.getEmailId(), e.getMessage());
            }
        }

        if (!expiredSnoozes.isEmpty()) {
            log.info("Processed {} expired snoozes", expiredSnoozes.size());
        }
    }

    // ==================== Summary Operations ====================

    /**
     * Generate or regenerate AI summary for an email.
     */
    @Transactional
    public KanbanEmailResponse generateSummary(String userId, String emailId) {
        EmailKanbanStatus status = emailStatusRepository.findByUserIdAndEmailId(userId, emailId)
                .orElseThrow(() -> new ResourceNotFoundException("Email not found on Kanban board"));

        // Fetch email from Gmail to get full body
        Message gmailMessage = gmailService.getMessage(userId, emailId);
        String body = gmailMessageConverter.getBody(gmailMessage);

        String summary = aiSummarizationService.generateSummary(status.getSubject(), status.getFromEmail(), body);

        if (summary != null) {
            status.setSummary(summary);
            status.setSummaryGeneratedAt(LocalDateTime.now());
            status.setUpdatedAt(LocalDateTime.now());
            status = emailStatusRepository.save(status);
            log.info("Generated summary for email {} for user {}", emailId, userId);
        } else {
            throw new BadRequestException("Failed to generate summary. Please check AI service configuration.");
        }

        return toEmailResponse(status);
    }

    /**
     * Remove an email from the Kanban board.
     */
    @Transactional
    public void removeEmailFromKanban(String userId, String emailId) {
        if (!emailStatusRepository.existsByUserIdAndEmailId(userId, emailId)) {
            throw new ResourceNotFoundException("Email not found on Kanban board");
        }
        emailStatusRepository.deleteByUserIdAndEmailId(userId, emailId);
        log.info("Removed email {} from Kanban board for user {}", emailId, userId);
    }

    /**
     * Get a single email's Kanban status.
     */
    public KanbanEmailResponse getEmailStatus(String userId, String emailId) {
        EmailKanbanStatus status = emailStatusRepository.findByUserIdAndEmailId(userId, emailId)
                .orElseThrow(() -> new ResourceNotFoundException("Email not found on Kanban board"));
        return toEmailResponse(status);
    }
}

