package com.hcmus.awad_email.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcmus.awad_email.dto.search.SemanticSearchRequest;
import com.hcmus.awad_email.dto.search.SemanticSearchResponse;
import com.hcmus.awad_email.dto.search.SemanticSearchResultItem;
import com.hcmus.awad_email.model.EmailKanbanStatus;
import com.hcmus.awad_email.model.KanbanColumn;
import com.hcmus.awad_email.repository.EmailKanbanStatusRepository;
import com.hcmus.awad_email.repository.KanbanColumnRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for semantic search using vector embeddings via AI service.
 * Finds conceptually related emails, not just exact text matches.
 */
@Service
@Slf4j
public class SemanticSearchService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final double DEFAULT_MIN_SCORE = 0.2;

    @Value("${app.ai-service.base-url:http://localhost:8000}")
    private String aiServiceBaseUrl;

    @Autowired
    private EmailKanbanStatusRepository emailStatusRepository;

    @Autowired
    private KanbanColumnRepository columnRepository;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String EMBEDDING_SEARCH_ENDPOINT = "/api/v1/email/search/embedding";
    private static final String EMBEDDING_STATUS_ENDPOINT = "/api/v1/email/embedding/status";
    private static final String BATCH_EMBEDDING_ENDPOINT = "/api/v1/email/embedding/generate/batch";

    public SemanticSearchService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Check if semantic search is available (AI service configured and running).
     */
    public boolean isAvailable() {
        if (aiServiceBaseUrl == null || aiServiceBaseUrl.isEmpty()) {
            return false;
        }
        try {
            String url = aiServiceBaseUrl + EMBEDDING_STATUS_ENDPOINT;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                return root.path("available").asBoolean(false);
            }
        } catch (Exception e) {
            log.warn("AI service not available: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Perform semantic search on user's emails via AI service.
     * Sends emails to AI service which generates embeddings and performs similarity search.
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
            generateMissingEmbeddingsViaAI(emailsWithoutEmbeddings);
            // Re-fetch to get updated embeddings
            allEmails = emailStatusRepository.findByUserId(userId);
            emailsWithEmbeddings = allEmails.stream()
                    .filter(e -> e.getEmbedding() != null && !e.getEmbedding().isEmpty())
                    .collect(Collectors.toList());
            emailsWithoutEmbeddings = allEmails.stream()
                    .filter(e -> e.getEmbedding() == null || e.getEmbedding().isEmpty())
                    .collect(Collectors.toList());
        }

        // Call AI service for semantic search
        try {
            List<SemanticSearchResultItem> results = performAISearch(
                    query, allEmails, limit, minScore, columnNames);

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
        } catch (Exception e) {
            log.error("AI service search failed: {}", e.getMessage(), e);
            return SemanticSearchResponse.builder()
                    .query(query)
                    .totalResults(0)
                    .results(Collections.emptyList())
                    .emailsWithEmbeddings(emailsWithEmbeddings.size())
                    .emailsWithoutEmbeddings(emailsWithoutEmbeddings.size())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * Perform semantic search via AI service.
     */
    private List<SemanticSearchResultItem> performAISearch(
            String query,
            List<EmailKanbanStatus> emails,
            int limit,
            double minScore,
            Map<String, String> columnNames) {

        String url = aiServiceBaseUrl + EMBEDDING_SEARCH_ENDPOINT;

        // Build request body
        List<Map<String, Object>> emailsList = new ArrayList<>();
        Map<String, EmailKanbanStatus> emailMap = new HashMap<>();

        for (EmailKanbanStatus email : emails) {
            Map<String, Object> emailData = new HashMap<>();
            emailData.put("email_id", email.getEmailId());
            emailData.put("subject", email.getSubject());
            emailData.put("body", email.getPreview());
            emailData.put("from_email", email.getFromEmail());
            emailData.put("from_name", email.getFromName());

            // Include pre-computed embedding if available
            if (email.getEmbedding() != null && !email.getEmbedding().isEmpty()) {
                emailData.put("embedding", email.getEmbedding());
            }

            emailsList.add(emailData);
            emailMap.put(email.getEmailId(), email);
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", query);
        requestBody.put("emails", emailsList);
        requestBody.put("top_k", limit);
        requestBody.put("min_score", minScore);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        log.debug("Calling AI service at {} for semantic search", url);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return parseSearchResults(response.getBody(), emailMap, columnNames);
        }

        log.error("AI service search failed. Status: {}", response.getStatusCode());
        return Collections.emptyList();
    }

    /**
     * Parse search results from AI service response.
     */
    private List<SemanticSearchResultItem> parseSearchResults(
            String responseBody,
            Map<String, EmailKanbanStatus> emailMap,
            Map<String, String> columnNames) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode resultsNode = root.path("results");

            List<SemanticSearchResultItem> results = new ArrayList<>();

            if (resultsNode.isArray()) {
                for (JsonNode resultNode : resultsNode) {
                    String emailId = resultNode.path("email_id").asText();
                    double score = resultNode.path("similarity_score").asDouble();

                    EmailKanbanStatus email = emailMap.get(emailId);
                    if (email != null) {
                        results.add(SemanticSearchResultItem.builder()
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
                                .similarityScore(score)
                                .summary(email.getSummary())
                                .build());
                    }
                }
            }

            return results;
        } catch (Exception e) {
            log.error("Error parsing AI service response: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Generate embeddings for all emails that don't have them via AI service.
     */
    @Transactional
    public int generateAllEmbeddings(String userId) {
        List<EmailKanbanStatus> emails = emailStatusRepository.findByUserId(userId);
        List<EmailKanbanStatus> emailsWithoutEmbeddings = emails.stream()
                .filter(e -> e.getEmbedding() == null || e.getEmbedding().isEmpty())
                .collect(Collectors.toList());

        return generateMissingEmbeddingsViaAI(emailsWithoutEmbeddings);
    }

    /**
     * Generate embedding for a single email via AI service.
     */
    @Transactional
    public boolean generateEmbeddingForEmail(String userId, String emailId) {
        Optional<EmailKanbanStatus> emailOpt = emailStatusRepository.findByUserIdAndEmailId(userId, emailId);
        if (emailOpt.isEmpty()) {
            return false;
        }

        EmailKanbanStatus email = emailOpt.get();
        List<Double> embedding = generateEmbeddingViaAI(email.getSubject(), email.getPreview());

        if (embedding != null) {
            email.setEmbedding(embedding);
            email.setEmbeddingGeneratedAt(LocalDateTime.now());
            emailStatusRepository.save(email);
            return true;
        }
        return false;
    }

    /**
     * Generate embedding for a single text via AI service.
     */
    private List<Double> generateEmbeddingViaAI(String subject, String body) {
        try {
            String url = aiServiceBaseUrl + "/api/v1/email/embedding/generate";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("subject", subject);
            requestBody.put("body", body);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode embeddingNode = root.path("embedding");

                if (embeddingNode.isArray()) {
                    List<Double> embedding = new ArrayList<>();
                    for (JsonNode value : embeddingNode) {
                        embedding.add(value.asDouble());
                    }
                    return embedding;
                }
            }
        } catch (Exception e) {
            log.error("Error generating embedding via AI service: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Generate embeddings for multiple emails via AI service batch endpoint.
     */
    private int generateMissingEmbeddingsViaAI(List<EmailKanbanStatus> emails) {
        if (emails.isEmpty()) {
            return 0;
        }

        try {
            String url = aiServiceBaseUrl + BATCH_EMBEDDING_ENDPOINT;

            // Build request body
            List<Map<String, Object>> emailsList = new ArrayList<>();
            for (EmailKanbanStatus email : emails) {
                Map<String, Object> emailData = new HashMap<>();
                emailData.put("email_id", email.getEmailId());
                emailData.put("subject", email.getSubject());
                emailData.put("body", email.getPreview());
                emailsList.add(emailData);
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("emails", emailsList);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.debug("Calling AI service at {} for batch embedding generation", url);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseAndSaveEmbeddings(response.getBody(), emails);
            }

            log.error("AI service batch embedding failed. Status: {}", response.getStatusCode());
            return 0;

        } catch (Exception e) {
            log.error("Error generating batch embeddings via AI service: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Parse batch embedding response and save to database.
     */
    private int parseAndSaveEmbeddings(String responseBody, List<EmailKanbanStatus> emails) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode embeddingsNode = root.path("embeddings");

            // Create a map for quick lookup
            Map<String, EmailKanbanStatus> emailMap = emails.stream()
                    .collect(Collectors.toMap(EmailKanbanStatus::getEmailId, e -> e));

            int generated = 0;
            LocalDateTime now = LocalDateTime.now();

            if (embeddingsNode.isArray()) {
                for (JsonNode embeddingData : embeddingsNode) {
                    String emailId = embeddingData.path("email_id").asText();
                    JsonNode embeddingNode = embeddingData.path("embedding");

                    EmailKanbanStatus email = emailMap.get(emailId);
                    if (email != null && embeddingNode.isArray()) {
                        List<Double> embedding = new ArrayList<>();
                        for (JsonNode value : embeddingNode) {
                            embedding.add(value.asDouble());
                        }

                        email.setEmbedding(embedding);
                        email.setEmbeddingGeneratedAt(now);
                        emailStatusRepository.save(email);
                        generated++;
                    }
                }
            }

            log.info("Generated embeddings for {} emails via AI service", generated);
            return generated;

        } catch (Exception e) {
            log.error("Error parsing batch embedding response: {}", e.getMessage());
            return 0;
        }
    }

    private Map<String, String> getColumnNames(String userId) {
        return columnRepository.findByUserIdOrderByOrderAsc(userId).stream()
                .collect(Collectors.toMap(KanbanColumn::getId, KanbanColumn::getName));
    }
}

