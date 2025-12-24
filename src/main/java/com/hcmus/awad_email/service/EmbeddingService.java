package com.hcmus.awad_email.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for embedding-related operations.
 *
 * NOTE: Embedding generation is now handled by the AI service (Python FastAPI).
 * This service is kept for backward compatibility and utility methods.
 *
 * @see SemanticSearchService for the main semantic search implementation
 */
@Service
@Slf4j
public class EmbeddingService {

    @Value("${app.ai-service.base-url:http://localhost:8000}")
    private String aiServiceBaseUrl;

    /**
     * Check if the embedding service is available.
     * Now checks if AI service URL is configured.
     */
    public boolean isAvailable() {
        return aiServiceBaseUrl != null && !aiServiceBaseUrl.isEmpty();
    }

    /**
     * Calculate cosine similarity between two embedding vectors.
     * This utility method is kept for local similarity calculations if needed.
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
}

