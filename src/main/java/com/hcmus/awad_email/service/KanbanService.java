package com.hcmus.awad_email.service;

import com.google.api.services.gmail.model.Message;
import com.hcmus.awad_email.dto.email.MessageListResult;
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
                .userId(userId).name("Backlog").type(KanbanColumn.ColumnType.BACKLOG)
                .order(1).color("#9C27B0").isDefault(true).createdAt(now).updatedAt(now).build());

        defaultColumns.add(KanbanColumn.builder()
                .userId(userId).name("To Do").type(KanbanColumn.ColumnType.TODO)
                .order(2).color("#FBBC04").isDefault(true).createdAt(now).updatedAt(now).build());

        defaultColumns.add(KanbanColumn.builder()
                .userId(userId).name("In Progress").type(KanbanColumn.ColumnType.IN_PROGRESS)
                .order(3).color("#FF6D01").isDefault(true).createdAt(now).updatedAt(now).build());

        defaultColumns.add(KanbanColumn.builder()
                .userId(userId).name("Done").type(KanbanColumn.ColumnType.DONE)
                .order(4).color("#34A853").isDefault(true).createdAt(now).updatedAt(now).build());

        defaultColumns.add(KanbanColumn.builder()
                .userId(userId).name("Snoozed").type(KanbanColumn.ColumnType.SNOOZED)
                .order(5).color("#9E9E9E").isDefault(true).createdAt(now).updatedAt(now).build());

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
        
        // Move emails to Backlog before deleting (fall back to Inbox for backward compatibility)
        KanbanColumn targetColumn = columnRepository.findByUserIdAndType(userId, KanbanColumn.ColumnType.BACKLOG)
                .orElseGet(() -> columnRepository.findByUserIdAndType(userId, KanbanColumn.ColumnType.INBOX)
                        .orElseThrow(() -> new ResourceNotFoundException("No suitable column found")));
        
        List<EmailKanbanStatus> emails = emailStatusRepository
                .findByUserIdAndColumnIdOrderByOrderInColumnAsc(userId, columnId);
        for (EmailKanbanStatus email : emails) {
            email.setColumnId(targetColumn.getId());
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
     * Uses cached emails from database for performance. Call with sync=true to fetch new emails from Gmail.
     *
     * @param userId The user ID
     * @param maxEmails Maximum emails to fetch/display (default 50)
     * @param sync If true, sync new emails from Gmail first
     */
    public KanbanBoardResponse getBoard(String userId, Integer maxEmails, boolean sync) {
        int limit = maxEmails != null ? maxEmails : 50;

        // Get columns
        List<KanbanColumnResponse> columns = getColumns(userId);
        Map<String, List<KanbanEmailResponse>> emailsByColumn = new HashMap<>();

        // Initialize empty lists for all columns
        for (KanbanColumnResponse column : columns) {
            emailsByColumn.put(column.getId(), new ArrayList<>());
        }

        // If sync is requested and Gmail is connected, sync new emails first
        if (sync && gmailService.isGmailConnected(userId)) {
            syncGmailEmails(userId, limit);
        }

        // Load all emails from database (cached)
        List<EmailKanbanStatus> allStatuses = emailStatusRepository.findByUserId(userId);

        if (allStatuses.isEmpty()) {
            // No cached emails - if Gmail is connected, do initial sync
            if (gmailService.isGmailConnected(userId)) {
                log.info("No cached emails found for user {}, performing initial sync", userId);
                syncGmailEmails(userId, limit);
                allStatuses = emailStatusRepository.findByUserId(userId);
            }
        }

        // Convert cached statuses to KanbanEmailResponse
        for (EmailKanbanStatus status : allStatuses) {
            KanbanEmailResponse emailResponse = toEmailResponse(status);

            String columnId = status.getColumnId();
            // Add to appropriate column
            if (emailsByColumn.containsKey(columnId)) {
                emailsByColumn.get(columnId).add(emailResponse);
            } else {
                // Column doesn't exist (maybe deleted), find backlog
                String backlogColumnId = columns.stream()
                        .filter(c -> c.getType() == KanbanColumn.ColumnType.BACKLOG)
                        .map(KanbanColumnResponse::getId)
                        .findFirst()
                        .orElseGet(() -> columns.stream()
                                .filter(c -> c.getType() == KanbanColumn.ColumnType.INBOX)
                                .map(KanbanColumnResponse::getId)
                                .findFirst()
                                .orElse(columns.get(0).getId()));
                emailsByColumn.get(backlogColumnId).add(emailResponse);
            }
        }

        // Sort emails in each column by orderInColumn
        for (List<KanbanEmailResponse> emails : emailsByColumn.values()) {
            emails.sort(Comparator.comparingInt(KanbanEmailResponse::getOrderInColumn));
        }

        log.info("Loaded Kanban board for user {} with {} cached emails (sync={})", userId, allStatuses.size(), sync);

        return KanbanBoardResponse.builder()
                .columns(columns)
                .emailsByColumn(emailsByColumn)
                .build();
    }

    /**
     * Get the full Kanban board with all columns and emails (no sync).
     */
    public KanbanBoardResponse getBoard(String userId, Integer maxEmails) {
        return getBoard(userId, maxEmails, false);
    }

    /**
     * Get the full Kanban board with default max emails.
     */
    public KanbanBoardResponse getBoard(String userId) {
        return getBoard(userId, 50);
    }

    /**
     * Build KanbanEmailResponse from Gmail Message.
     */
    private KanbanEmailResponse buildKanbanEmailResponse(Message gmailMessage, String columnId, int orderInColumn,
                                                          String summary, LocalDateTime summaryGeneratedAt,
                                                          boolean snoozed, LocalDateTime snoozeUntil) {
        String from = gmailMessageConverter.getHeader(gmailMessage, "From");
        String fromName = extractName(from);
        String fromEmail = extractEmail(from);
        String subject = gmailMessageConverter.getHeader(gmailMessage, "Subject");
        String snippet = gmailMessage.getSnippet() != null ? gmailMessage.getSnippet() : "";
        String preview = snippet.length() > 200 ? snippet.substring(0, 200) : snippet;

        return KanbanEmailResponse.builder()
                .id(gmailMessage.getId()) // Use Gmail message ID as the ID
                .emailId(gmailMessage.getId())
                .columnId(columnId)
                .orderInColumn(orderInColumn)
                .subject(subject != null ? subject : "(No Subject)")
                .fromEmail(fromEmail)
                .fromName(fromName)
                .preview(preview)
                .receivedAt(gmailMessageConverter.getReceivedAt(gmailMessage))
                .isRead(gmailMessageConverter.isRead(gmailMessage))
                .isStarred(gmailMessageConverter.isStarred(gmailMessage))
                .summary(summary)
                .summaryGeneratedAt(summaryGeneratedAt)
                .snoozed(snoozed)
                .snoozeUntil(snoozeUntil)
                .build();
    }

    /**
     * Get emails in a specific column.
     * Fetches from Gmail and filters by column.
     */
    public List<KanbanEmailResponse> getEmailsInColumn(String userId, String columnId) {
        KanbanBoardResponse board = getBoard(userId);
        return board.getEmailsByColumn().getOrDefault(columnId, Collections.emptyList());
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

        // Determine target column (default to Backlog, fall back to Inbox for backward compatibility)
        String columnId = request.getColumnId();
        if (columnId == null || columnId.isEmpty()) {
            KanbanColumn defaultColumn = columnRepository.findByUserIdAndType(userId, KanbanColumn.ColumnType.BACKLOG)
                    .orElseGet(() -> columnRepository.findByUserIdAndType(userId, KanbanColumn.ColumnType.INBOX)
                            .orElseGet(() -> initializeDefaultColumns(userId).get(1))); // Index 1 is Backlog
            columnId = defaultColumn.getId();
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
     * Creates an EmailKanbanStatus record if it doesn't exist.
     */
    @Transactional
    public KanbanEmailResponse moveEmail(String userId, MoveEmailRequest request) {
        KanbanColumn targetColumn = columnRepository.findByIdAndUserId(request.getTargetColumnId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Target column not found"));

        // Get or create EmailKanbanStatus
        EmailKanbanStatus status = emailStatusRepository.findByUserIdAndEmailId(userId, request.getEmailId())
                .orElseGet(() -> {
                    // Create a new status record for this email
                    Message gmailMessage;
                    try {
                        gmailMessage = gmailService.getMessage(userId, request.getEmailId());
                    } catch (Exception e) {
                        throw new ResourceNotFoundException("Email not found in Gmail");
                    }

                    String from = gmailMessageConverter.getHeader(gmailMessage, "From");
                    String subject = gmailMessageConverter.getHeader(gmailMessage, "Subject");
                    String snippet = gmailMessage.getSnippet() != null ? gmailMessage.getSnippet() : "";
                    String preview = snippet.length() > 200 ? snippet.substring(0, 200) : snippet;

                    return EmailKanbanStatus.builder()
                            .userId(userId)
                            .emailId(request.getEmailId())
                            .columnId(request.getTargetColumnId())
                            .orderInColumn(0)
                            .subject(subject != null ? subject : "(No Subject)")
                            .fromEmail(extractEmail(from))
                            .fromName(extractName(from))
                            .preview(preview)
                            .receivedAt(gmailMessageConverter.getReceivedAt(gmailMessage))
                            .isRead(gmailMessageConverter.isRead(gmailMessage))
                            .isStarred(gmailMessageConverter.isStarred(gmailMessage))
                            .snoozed(false)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                });

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
     * Creates an EmailKanbanStatus record if it doesn't exist.
     */
    @Transactional
    public KanbanEmailResponse snoozeEmail(String userId, SnoozeEmailRequest request) {
        // Get or create Snoozed column
        KanbanColumn snoozedColumn = columnRepository.findByUserIdAndType(userId, KanbanColumn.ColumnType.SNOOZED)
                .orElseGet(() -> {
                    initializeDefaultColumns(userId);
                    return columnRepository.findByUserIdAndType(userId, KanbanColumn.ColumnType.SNOOZED)
                            .orElseThrow(() -> new ResourceNotFoundException("Snoozed column not found"));
                });

        // Get Backlog column for previousColumnId if creating new status
        String backlogColumnId = columnRepository.findByUserIdAndType(userId, KanbanColumn.ColumnType.BACKLOG)
                .map(KanbanColumn::getId)
                .orElseGet(() -> columnRepository.findByUserIdAndType(userId, KanbanColumn.ColumnType.INBOX)
                        .map(KanbanColumn::getId)
                        .orElse(null));

        // Get or create EmailKanbanStatus
        EmailKanbanStatus status = emailStatusRepository.findByUserIdAndEmailId(userId, request.getEmailId())
                .orElseGet(() -> {
                    // Create a new status record for this email
                    Message gmailMessage;
                    try {
                        gmailMessage = gmailService.getMessage(userId, request.getEmailId());
                    } catch (Exception e) {
                        throw new ResourceNotFoundException("Email not found in Gmail");
                    }

                    String from = gmailMessageConverter.getHeader(gmailMessage, "From");
                    String subject = gmailMessageConverter.getHeader(gmailMessage, "Subject");
                    String snippet = gmailMessage.getSnippet() != null ? gmailMessage.getSnippet() : "";
                    String preview = snippet.length() > 200 ? snippet.substring(0, 200) : snippet;

                    return EmailKanbanStatus.builder()
                            .userId(userId)
                            .emailId(request.getEmailId())
                            .columnId(backlogColumnId) // Will be updated to snoozed column
                            .orderInColumn(0)
                            .subject(subject != null ? subject : "(No Subject)")
                            .fromEmail(extractEmail(from))
                            .fromName(extractName(from))
                            .preview(preview)
                            .receivedAt(gmailMessageConverter.getReceivedAt(gmailMessage))
                            .isRead(gmailMessageConverter.isRead(gmailMessage))
                            .isStarred(gmailMessageConverter.isStarred(gmailMessage))
                            .snoozed(false)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
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
     * Creates an EmailKanbanStatus record if it doesn't exist (places email in Backlog).
     */
    @Transactional
    public KanbanEmailResponse generateSummary(String userId, String emailId) {
        // Fetch email from Gmail first to verify it exists
        Message gmailMessage;
        try {
            gmailMessage = gmailService.getMessage(userId, emailId);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Email not found in Gmail");
        }

        // Get or create EmailKanbanStatus
        EmailKanbanStatus status = emailStatusRepository.findByUserIdAndEmailId(userId, emailId)
                .orElseGet(() -> {
                    // Create a new status record for this email (place in Backlog)
                    KanbanColumn backlogColumn = columnRepository.findByUserIdAndType(userId, KanbanColumn.ColumnType.BACKLOG)
                            .orElseGet(() -> columnRepository.findByUserIdAndType(userId, KanbanColumn.ColumnType.INBOX)
                                    .orElseGet(() -> initializeDefaultColumns(userId).get(1)));

                    String from = gmailMessageConverter.getHeader(gmailMessage, "From");
                    String subject = gmailMessageConverter.getHeader(gmailMessage, "Subject");
                    String snippet = gmailMessage.getSnippet() != null ? gmailMessage.getSnippet() : "";
                    String preview = snippet.length() > 200 ? snippet.substring(0, 200) : snippet;

                    EmailKanbanStatus newStatus = EmailKanbanStatus.builder()
                            .userId(userId)
                            .emailId(emailId)
                            .columnId(backlogColumn.getId())
                            .orderInColumn(0)
                            .subject(subject != null ? subject : "(No Subject)")
                            .fromEmail(extractEmail(from))
                            .fromName(extractName(from))
                            .preview(preview)
                            .receivedAt(gmailMessageConverter.getReceivedAt(gmailMessage))
                            .isRead(gmailMessageConverter.isRead(gmailMessage))
                            .isStarred(gmailMessageConverter.isStarred(gmailMessage))
                            .snoozed(false)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();

                    return emailStatusRepository.save(newStatus);
                });

        // Get email body for summary generation
        String body = gmailMessageConverter.getBody(gmailMessage);
        String subject = gmailMessageConverter.getHeader(gmailMessage, "Subject");
        String from = gmailMessageConverter.getHeader(gmailMessage, "From");

        String summary = aiSummarizationService.generateSummary(
                subject != null ? subject : status.getSubject(),
                extractEmail(from) != null ? extractEmail(from) : status.getFromEmail(),
                body);

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
     * If no status exists, fetches from Gmail and returns with Backlog column.
     */
    public KanbanEmailResponse getEmailStatus(String userId, String emailId) {
        // Check if status exists
        Optional<EmailKanbanStatus> statusOpt = emailStatusRepository.findByUserIdAndEmailId(userId, emailId);

        if (statusOpt.isPresent()) {
            return toEmailResponse(statusOpt.get());
        }

        // No status exists - fetch from Gmail and return with Backlog column
        if (!gmailService.isGmailConnected(userId)) {
            throw new ResourceNotFoundException("Email not found on Kanban board");
        }

        try {
            Message gmailMessage = gmailService.getMessage(userId, emailId);

            // Find Backlog column
            String backlogColumnId = columnRepository.findByUserIdAndType(userId, KanbanColumn.ColumnType.BACKLOG)
                    .map(KanbanColumn::getId)
                    .orElseGet(() -> columnRepository.findByUserIdAndType(userId, KanbanColumn.ColumnType.INBOX)
                            .map(KanbanColumn::getId)
                            .orElse(null));

            if (backlogColumnId == null) {
                List<KanbanColumn> columns = initializeDefaultColumns(userId);
                backlogColumnId = columns.get(1).getId(); // Index 1 is Backlog
            }

            return buildKanbanEmailResponse(gmailMessage, backlogColumnId, 0, null, null, false, null);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Email not found in Gmail");
        }
    }

    // ==================== Gmail Sync Operations ====================

    /**
     * Sync Gmail emails to the Kanban board.
     * New emails are placed in the BACKLOG column by default (falls back to INBOX for backward compatibility).
     *
     * @param userId The user ID
     * @param maxEmails Maximum number of emails to sync (default 50)
     * @return SyncResult containing counts of synced and skipped emails
     */
    @Transactional
    public KanbanSyncResult syncGmailEmails(String userId, Integer maxEmails) {
        // Check if Gmail is connected
        if (!gmailService.isGmailConnected(userId)) {
            log.warn("Gmail not connected for user {}, skipping sync", userId);
            return KanbanSyncResult.builder()
                    .synced(0)
                    .skipped(0)
                    .total(0)
                    .message("Gmail not connected. Please connect your Gmail account first.")
                    .build();
        }

        int limit = maxEmails != null ? maxEmails : 50;

        // Initialize columns if needed
        List<KanbanColumn> existingColumns = columnRepository.findByUserIdOrderByOrderAsc(userId);
        final List<KanbanColumn> columns = existingColumns.isEmpty()
                ? initializeDefaultColumns(userId)
                : existingColumns;

        // Get the BACKLOG column (default column for new Gmail emails)
        // Fall back to INBOX if BACKLOG doesn't exist (for backward compatibility)
        KanbanColumn targetColumn = columns.stream()
                .filter(c -> c.getType() == KanbanColumn.ColumnType.BACKLOG)
                .findFirst()
                .orElseGet(() -> columns.stream()
                        .filter(c -> c.getType() == KanbanColumn.ColumnType.INBOX)
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("No suitable column found for syncing emails")));

        // Fetch emails from Gmail INBOX
        MessageListResult gmailResult = gmailService.listMessages(userId, "INBOX", (long) limit, null);
        List<Message> gmailMessages = gmailResult.getMessages();

        if (gmailMessages.isEmpty()) {
            return KanbanSyncResult.builder()
                    .synced(0)
                    .skipped(0)
                    .total(0)
                    .message("No emails found in Gmail inbox.")
                    .build();
        }

        // Get existing email IDs in Kanban
        List<String> gmailEmailIds = gmailMessages.stream()
                .map(Message::getId)
                .collect(Collectors.toList());

        Set<String> existingEmailIds = emailStatusRepository.findByUserIdAndEmailIdIn(userId, gmailEmailIds)
                .stream()
                .map(EmailKanbanStatus::getEmailId)
                .collect(Collectors.toSet());

        int synced = 0;
        int skipped = 0;
        int currentOrder = (int) emailStatusRepository.countByUserIdAndColumnId(userId, targetColumn.getId());
        LocalDateTime now = LocalDateTime.now();

        for (Message gmailMessage : gmailMessages) {
            String emailId = gmailMessage.getId();

            // Skip if already in Kanban
            if (existingEmailIds.contains(emailId)) {
                skipped++;
                continue;
            }

            try {
                // Extract email metadata
                String subject = gmailMessageConverter.getHeader(gmailMessage, "Subject");
                String from = gmailMessageConverter.getHeader(gmailMessage, "From");
                String fromName = extractName(from);
                String fromEmail = extractEmail(from);
                String snippet = gmailMessage.getSnippet() != null ? gmailMessage.getSnippet() : "";
                String preview = snippet.length() > 200 ? snippet.substring(0, 200) : snippet;

                EmailKanbanStatus status = EmailKanbanStatus.builder()
                        .userId(userId)
                        .emailId(emailId)
                        .columnId(targetColumn.getId())
                        .orderInColumn(currentOrder++)
                        .subject(subject != null ? subject : "(No Subject)")
                        .fromEmail(fromEmail)
                        .fromName(fromName)
                        .preview(preview)
                        .receivedAt(gmailMessageConverter.getReceivedAt(gmailMessage))
                        .isRead(gmailMessageConverter.isRead(gmailMessage))
                        .isStarred(gmailMessageConverter.isStarred(gmailMessage))
                        .snoozed(false)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

                emailStatusRepository.save(status);
                synced++;

            } catch (Exception e) {
                log.error("Failed to sync email {} for user {}: {}", emailId, userId, e.getMessage());
                skipped++;
            }
        }

        log.info("Synced {} emails to Kanban board (column: {}) for user {} ({} skipped)",
                synced, targetColumn.getName(), userId, skipped);

        return KanbanSyncResult.builder()
                .synced(synced)
                .skipped(skipped)
                .total(gmailMessages.size())
                .message(String.format("Successfully synced %d emails to Kanban board (Backlog).", synced))
                .build();
    }

    /**
     * Check if Gmail is connected for the user.
     */
    public boolean isGmailConnected(String userId) {
        return gmailService.isGmailConnected(userId);
    }
}

