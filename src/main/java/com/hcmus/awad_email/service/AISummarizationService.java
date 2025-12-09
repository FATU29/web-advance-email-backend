package com.hcmus.awad_email.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for generating email summaries using AI (Gemini API).
 * Provides concise summaries of email content for quick decision-making.
 */
@Service
@Slf4j
public class AISummarizationService {
    
    @Value("${app.gemini.api-key:}")
    private String geminiApiKey;
    
    @Value("${app.gemini.model:gemini-1.5-flash}")
    private String geminiModel;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";
    
    private static final String SUMMARY_PROMPT = """
        You are an email assistant. Summarize the following email in 2-3 concise sentences.
        Focus on the main purpose, any action items, and key information.
        Keep the summary professional and easy to scan quickly.
        
        Email Subject: %s
        From: %s
        
        Email Content:
        %s
        
        Summary:
        """;
    
    public AISummarizationService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Generate a summary for an email using Gemini API.
     * 
     * @param subject Email subject
     * @param from Sender information
     * @param body Email body content
     * @return Generated summary or null if generation fails
     */
    public String generateSummary(String subject, String from, String body) {
        if (geminiApiKey == null || geminiApiKey.isEmpty()) {
            log.warn("Gemini API key not configured. Skipping summary generation.");
            return null;
        }
        
        try {
            // Clean and truncate body if too long (Gemini has token limits)
            String cleanBody = cleanEmailBody(body);
            if (cleanBody.length() > 10000) {
                cleanBody = cleanBody.substring(0, 10000) + "...";
            }
            
            String prompt = String.format(SUMMARY_PROMPT, 
                    subject != null ? subject : "(No Subject)",
                    from != null ? from : "Unknown",
                    cleanBody);
            
            String url = String.format(GEMINI_API_URL, geminiModel, geminiApiKey);
            
            // Build request body
            Map<String, Object> requestBody = buildGeminiRequest(prompt);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return extractSummaryFromResponse(response.getBody());
            }
            
            log.error("Failed to generate summary. Status: {}", response.getStatusCode());
            return null;
            
        } catch (Exception e) {
            log.error("Error generating email summary: {}", e.getMessage());
            return null;
        }
    }
    
    private Map<String, Object> buildGeminiRequest(String prompt) {
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);
        
        Map<String, Object> parts = new HashMap<>();
        parts.put("parts", List.of(textPart));
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(parts));
        
        // Add generation config for better summaries
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.3);
        generationConfig.put("maxOutputTokens", 200);
        requestBody.put("generationConfig", generationConfig);
        
        return requestBody;
    }
    
    private String extractSummaryFromResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).path("content");
                JsonNode parts = content.path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    return parts.get(0).path("text").asText().trim();
                }
            }
        } catch (Exception e) {
            log.error("Error parsing Gemini response: {}", e.getMessage());
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

