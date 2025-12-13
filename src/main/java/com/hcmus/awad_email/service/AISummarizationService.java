package com.hcmus.awad_email.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for generating email summaries using AI (OpenAI via external AI service).
 * Provides concise summaries of email content for quick decision-making.
 */
@Service
@Slf4j
public class AISummarizationService {

    @Value("${app.ai-service.base-url:http://localhost:8000}")
    private String aiServiceBaseUrl;

    @Value("${app.ai-service.timeout-seconds:30}")
    private int timeoutSeconds;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String SUMMARIZE_ENDPOINT = "/api/v1/email/summarize";

    public AISummarizationService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generate a summary for an email using the external AI service (OpenAI).
     *
     * @param subject Email subject
     * @param from Sender information
     * @param body Email body content
     * @return Generated summary or null if generation fails
     */
    public String generateSummary(String subject, String from, String body) {
        if (aiServiceBaseUrl == null || aiServiceBaseUrl.isEmpty()) {
            log.warn("AI service URL not configured. Skipping summary generation.");
            return null;
        }

        try {
            // Clean and truncate body if too long
            String cleanBody = cleanEmailBody(body);
            if (cleanBody.length() > 10000) {
                cleanBody = cleanBody.substring(0, 10000) + "...";
            }

            String url = aiServiceBaseUrl + SUMMARIZE_ENDPOINT;

            // Build request body for the AI service
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("subject", subject != null ? subject : "(No Subject)");
            requestBody.put("from_email", from != null ? from : "Unknown");
            requestBody.put("body", cleanBody);
            // Don't set max_length to let AI generate complete summary without truncation

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.debug("Calling AI service at {} for email summarization", url);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return extractSummaryFromResponse(response.getBody());
            }

            log.error("Failed to generate summary. Status: {}", response.getStatusCode());
            return null;

        } catch (Exception e) {
            log.error("Error generating email summary: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extract summary from AI service response.
     * Expected response format: {"summary": "..."}
     */
    private String extractSummaryFromResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode summaryNode = root.path("summary");
            if (!summaryNode.isMissingNode() && summaryNode.isTextual()) {
                return summaryNode.asText().trim();
            }
            log.warn("Unexpected response format from AI service: {}", responseBody);
        } catch (Exception e) {
            log.error("Error parsing AI service response: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Clean HTML tags and excessive whitespace from email body.
     */
    private String cleanEmailBody(String body) {
        if (body == null) return "";
        // Remove HTML tags
        String cleaned = body.replaceAll("<[^>]+>", " ");
        // Remove excessive whitespace
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned;
    }
}

