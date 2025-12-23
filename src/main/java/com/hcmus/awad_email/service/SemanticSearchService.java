package com.hcmus.awad_email.service;

import com.hcmus.awad_email.dto.search.SemanticSearchRequest;
import com.hcmus.awad_email.dto.search.SemanticSearchResponse;
import com.hcmus.awad_email.dto.search.SemanticSearchResultItem;
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

/**
 * Service for semantic search using vector embeddings.
 * Finds conceptually related emails, not just exact text matches.
 */
@Service
@Slf4j
public class SemanticSearchService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final double DEFAULT_MIN_SCORE = 0.3;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private EmailKanbanStatusRepository emailStatusRepository;

    @Autowired
    private KanbanColumnRepository columnRepository;

    /**
     * Check if semantic search is available (OpenAI API configured).
     */
    public boolean isAvailable() {
        return embeddingService.isAvailable();
    }

    /**
     * Perform semantic search on user's emails.
     * Converts query to embedding and finds similar emails using cosine similarity.
     */
    @Transactional
    public SemanticSearchResponse search(String userId, SemanticSearchRequest request) {
        long startTime = System.currentTimeMillis();
        
        String query = request.getQuery().trim();
        int limit = request.getLimit() != null ? Math.min(request.getLimit(), MAX_LIMIT) : DEFAULT_LIMIT;
        double minScore = request.getMinScore() != null ? request.getMinScore() : DEFAULT_MIN_SCORE;
        boolean generateMissing = request.getGenerateMissingEmbeddings() != null 
                && request.getGenerateMissingEmbeddings();

        log.info("🔍 Semantic search for user: {} | query: '{}' | limit: {} | minScore: {}", 
                userId, query, limit, minScore);

        // Generate embedding for the query
        List<Double> queryEmbedding = embeddingService.generateEmbedding(query, null);
        if (queryEmbedding == null) {
            log.error("Failed to generate embedding for query: {}", query);
            return SemanticSearchResponse.builder()
                    .query(query)
                    .totalResults(0)
                    .results(Collections.emptyList())
                    .emailsWithEmbeddings(0)
                    .emailsWithoutEmbeddings(0)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        // Get all emails for the user
        List<EmailKanbanStatus> allEmails = emailStatusRepository.findByUserId(userId);
        
        // Get column names for display
        Map<String, String> columnNames = getColumnNames(userId);

        // Separate emails with and without embeddings
        List<EmailKanbanStatus> emailsWithEmbeddings = new ArrayList<>();
        List<EmailKanbanStatus> emailsWithoutEmbeddings = new ArrayList<>();
        
        for (EmailKanbanStatus email : allEmails) {
            if (email.getEmbedding() != null && !email.getEmbedding().isEmpty()) {
                emailsWithEmbeddings.add(email);
            } else {
                emailsWithoutEmbeddings.add(email);
            }
        }

        // Generate embeddings for emails that don't have them (if requested)
        if (generateMissing && !emailsWithoutEmbeddings.isEmpty()) {
            generateMissingEmbeddings(emailsWithoutEmbeddings);
            // Re-fetch to get updated embeddings
            allEmails = emailStatusRepository.findByUserId(userId);
            emailsWithEmbeddings = allEmails.stream()
                    .filter(e -> e.getEmbedding() != null && !e.getEmbedding().isEmpty())
                    .collect(Collectors.toList());
            emailsWithoutEmbeddings = allEmails.stream()
                    .filter(e -> e.getEmbedding() == null || e.getEmbedding().isEmpty())
                    .collect(Collectors.toList());
        }

        // Calculate similarity scores and rank results
        List<ScoredEmail> scoredEmails = new ArrayList<>();
        for (EmailKanbanStatus email : emailsWithEmbeddings) {
            double score = embeddingService.cosineSimilarity(queryEmbedding, email.getEmbedding());
            if (score >= minScore) {
                scoredEmails.add(new ScoredEmail(email, score));
            }
        }

        // Sort by score descending
        scoredEmails.sort((a, b) -> Double.compare(b.score, a.score));

        // Convert to response items
        List<SemanticSearchResultItem> results = scoredEmails.stream()
                .limit(limit)
                .map(scored -> toResultItem(scored, columnNames))
                .collect(Collectors.toList());

        long processingTime = System.currentTimeMillis() - startTime;
        log.info("✅ Semantic search completed | results: {} | time: {}ms", results.size(), processingTime);

        return SemanticSearchResponse.builder()
                .query(query)
                .totalResults(results.size())
                .results(results)
                .emailsWithEmbeddings(emailsWithEmbeddings.size())
                .emailsWithoutEmbeddings(emailsWithoutEmbeddings.size())
                .processingTimeMs(processingTime)
                .build();
    }

    /**
     * Generate embeddings for all emails that don't have them.
     */
    @Transactional
    public int generateAllEmbeddings(String userId) {
        List<EmailKanbanStatus> emails = emailStatusRepository.findByUserId(userId);
        List<EmailKanbanStatus> emailsWithoutEmbeddings = emails.stream()
                .filter(e -> e.getEmbedding() == null || e.getEmbedding().isEmpty())
                .collect(Collectors.toList());
        
        return generateMissingEmbeddings(emailsWithoutEmbeddings);
    }

    /**
     * Generate embedding for a single email.
     */
    @Transactional
    public boolean generateEmbeddingForEmail(String userId, String emailId) {
        Optional<EmailKanbanStatus> emailOpt = emailStatusRepository.findByUserIdAndEmailId(userId, emailId);
        if (emailOpt.isEmpty()) {
            return false;
        }

        EmailKanbanStatus email = emailOpt.get();
        List<Double> embedding = embeddingService.generateEmbedding(email.getSubject(), email.getPreview());
        
        if (embedding != null) {
            email.setEmbedding(embedding);
            email.setEmbeddingGeneratedAt(LocalDateTime.now());
            emailStatusRepository.save(email);
            return true;
        }
        return false;
    }

    private int generateMissingEmbeddings(List<EmailKanbanStatus> emails) {
        int generated = 0;
        LocalDateTime now = LocalDateTime.now();
        
        for (EmailKanbanStatus email : emails) {
            try {
                List<Double> embedding = embeddingService.generateEmbedding(
                        email.getSubject(), email.getPreview());
                
                if (embedding != null) {
                    email.setEmbedding(embedding);
                    email.setEmbeddingGeneratedAt(now);
                    emailStatusRepository.save(email);
                    generated++;
                }
            } catch (Exception e) {
                log.error("Failed to generate embedding for email {}: {}", email.getEmailId(), e.getMessage());
            }
        }
        
        log.info("Generated embeddings for {} emails", generated);
        return generated;
    }

    private Map<String, String> getColumnNames(String userId) {
        return columnRepository.findByUserIdOrderByOrderAsc(userId).stream()
                .collect(Collectors.toMap(KanbanColumn::getId, KanbanColumn::getName));
    }

    private SemanticSearchResultItem toResultItem(ScoredEmail scored, Map<String, String> columnNames) {
        EmailKanbanStatus email = scored.email;
        return SemanticSearchResultItem.builder()
                .emailId(email.getEmailId())
                .subject(email.getSubject())
                .fromEmail(email.getFromEmail())
                .fromName(email.getFromName())
                .preview(email.getPreview())
                .columnId(email.getColumnId())
                .columnName(columnNames.getOrDefault(email.getColumnId(), "Unknown"))
                .receivedAt(email.getReceivedAt())
                .isRead(email.isRead())
                .isStarred(email.isStarred())
                .hasAttachments(email.isHasAttachments())
                .similarityScore(scored.score)
                .summary(email.getSummary())
                .build();
    }

    private static class ScoredEmail {
        final EmailKanbanStatus email;
        final double score;

        ScoredEmail(EmailKanbanStatus email, double score) {
            this.email = email;
            this.score = score;
        }
    }
}

