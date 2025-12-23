package com.hcmus.awad_email.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for generating text embeddings using OpenAI's embedding API.
 * Used for semantic search functionality.
 */
@Service
@Slf4j
public class EmbeddingService {

    @Value("${app.openai.api-key:}")
    private String openaiApiKey;

    @Value("${app.openai.embedding-model:text-embedding-3-small}")
    private String embeddingModel;

    @Value("${app.openai.embedding-dimensions:1536}")
    private int embeddingDimensions;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String OPENAI_EMBEDDING_URL = "https://api.openai.com/v1/embeddings";
    private static final int MAX_TEXT_LENGTH = 8000; // OpenAI limit for text-embedding-3-small

    public EmbeddingService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Check if the embedding service is configured and available.
     */
    public boolean isAvailable() {
        return openaiApiKey != null && !openaiApiKey.isEmpty();
    }

    /**
     * Generate embedding for email content (subject + body/preview).
     *
     * @param subject Email subject
     * @param body Email body or preview
     * @return List of embedding values, or null if generation fails
     */
    public List<Double> generateEmbedding(String subject, String body) {
        if (!isAvailable()) {
            log.warn("OpenAI API key not configured. Skipping embedding generation.");
            return null;
        }

        try {
            // Combine subject and body for embedding
            String text = buildEmbeddingText(subject, body);
            return callOpenAIEmbeddingAPI(text);
        } catch (Exception e) {
            log.error("Error generating embedding: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Generate embeddings for multiple texts in batch.
     *
     * @param texts List of texts to embed
     * @return List of embeddings (same order as input)
     */
    public List<List<Double>> generateEmbeddingsBatch(List<String> texts) {
        if (!isAvailable()) {
            log.warn("OpenAI API key not configured. Skipping batch embedding generation.");
            return null;
        }

        try {
            return callOpenAIEmbeddingAPIBatch(texts);
        } catch (Exception e) {
            log.error("Error generating batch embeddings: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Build text for embedding from email subject and body.
     */
    private String buildEmbeddingText(String subject, String body) {
        StringBuilder sb = new StringBuilder();
        
        if (subject != null && !subject.isEmpty()) {
            sb.append("Subject: ").append(subject).append("\n\n");
        }
        
        if (body != null && !body.isEmpty()) {
            // Clean HTML tags
            String cleanBody = body.replaceAll("<[^>]+>", " ");
            cleanBody = cleanBody.replaceAll("\\s+", " ").trim();
            sb.append("Content: ").append(cleanBody);
        }
        
        String text = sb.toString().trim();
        
        // Truncate if too long
        if (text.length() > MAX_TEXT_LENGTH) {
            text = text.substring(0, MAX_TEXT_LENGTH);
        }
        
        return text;
    }

    /**
     * Call OpenAI Embedding API for a single text.
     */
    private List<Double> callOpenAIEmbeddingAPI(String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openaiApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("input", text);
        requestBody.put("model", embeddingModel);
        requestBody.put("dimensions", embeddingDimensions);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        log.debug("Calling OpenAI Embedding API with model: {}", embeddingModel);

        ResponseEntity<String> response = restTemplate.exchange(
                OPENAI_EMBEDDING_URL, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return extractEmbeddingFromResponse(response.getBody());
        }

        log.error("Failed to generate embedding. Status: {}", response.getStatusCode());
        return null;
    }

    /**
     * Call OpenAI Embedding API for batch texts.
     */
    private List<List<Double>> callOpenAIEmbeddingAPIBatch(List<String> texts) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openaiApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("input", texts);
        requestBody.put("model", embeddingModel);
        requestBody.put("dimensions", embeddingDimensions);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                OPENAI_EMBEDDING_URL, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return extractEmbeddingsFromBatchResponse(response.getBody());
        }

        return null;
    }

    /**
     * Extract embedding from OpenAI API response.
     */
    private List<Double> extractEmbeddingFromResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode dataArray = root.path("data");
            
            if (dataArray.isArray() && dataArray.size() > 0) {
                JsonNode embeddingNode = dataArray.get(0).path("embedding");
                if (embeddingNode.isArray()) {
                    List<Double> embedding = new ArrayList<>();
                    for (JsonNode value : embeddingNode) {
                        embedding.add(value.asDouble());
                    }
                    return embedding;
                }
            }
        } catch (Exception e) {
            log.error("Error parsing embedding response: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extract embeddings from batch response.
     */
    private List<List<Double>> extractEmbeddingsFromBatchResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode dataArray = root.path("data");
            
            if (dataArray.isArray()) {
                List<List<Double>> embeddings = new ArrayList<>();
                for (JsonNode item : dataArray) {
                    JsonNode embeddingNode = item.path("embedding");
                    if (embeddingNode.isArray()) {
                        List<Double> embedding = new ArrayList<>();
                        for (JsonNode value : embeddingNode) {
                            embedding.add(value.asDouble());
                        }
                        embeddings.add(embedding);
                    }
                }
                return embeddings;
            }
        } catch (Exception e) {
            log.error("Error parsing batch embedding response: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Calculate cosine similarity between two embedding vectors.
     *
     * @param embedding1 First embedding vector
     * @param embedding2 Second embedding vector
     * @return Cosine similarity score (0 to 1, higher is more similar)
     */
    public double cosineSimilarity(List<Double> embedding1, List<Double> embedding2) {
        if (embedding1 == null || embedding2 == null || 
            embedding1.size() != embedding2.size() || embedding1.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < embedding1.size(); i++) {
            double v1 = embedding1.get(i);
            double v2 = embedding2.get(i);
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * Get the configured embedding dimensions.
     */
    public int getEmbeddingDimensions() {
        return embeddingDimensions;
    }
}

