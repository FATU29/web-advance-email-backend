package com.hcmus.awad_email.service;

import com.hcmus.awad_email.dto.search.SearchSuggestionResponse;
import com.hcmus.awad_email.dto.search.SearchSuggestionResponse.ContactSuggestion;
import com.hcmus.awad_email.dto.search.SearchSuggestionResponse.KeywordSuggestion;
import com.hcmus.awad_email.model.EmailKanbanStatus;
import com.hcmus.awad_email.repository.EmailKanbanStatusRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating search auto-suggestions.
 * Provides contact suggestions and keyword suggestions based on user's emails.
 */
@Service
@Slf4j
public class SearchSuggestionService {

    private static final int MAX_CONTACT_SUGGESTIONS = 5;
    private static final int MAX_KEYWORD_SUGGESTIONS = 5;
    private static final int MIN_QUERY_LENGTH = 1;

    @Autowired
    private EmailKanbanStatusRepository emailStatusRepository;

    /**
     * Get search suggestions based on the query prefix.
     * Returns matching contacts and keywords.
     */
    public SearchSuggestionResponse getSuggestions(String userId, String query) {
        if (query == null || query.trim().length() < MIN_QUERY_LENGTH) {
            return SearchSuggestionResponse.builder()
                    .query(query != null ? query : "")
                    .contacts(Collections.emptyList())
                    .keywords(Collections.emptyList())
                    .recentSearches(Collections.emptyList())
                    .build();
        }

        String normalizedQuery = query.trim().toLowerCase();
        log.debug("🔍 Getting suggestions for user: {} | query: '{}'", userId, normalizedQuery);

        // Get all emails for the user
        List<EmailKanbanStatus> allEmails = emailStatusRepository.findByUserId(userId);

        // Get contact suggestions
        List<ContactSuggestion> contacts = getContactSuggestions(allEmails, normalizedQuery);

        // Get keyword suggestions from subjects
        List<KeywordSuggestion> keywords = getKeywordSuggestions(allEmails, normalizedQuery);

        return SearchSuggestionResponse.builder()
                .query(query)
                .contacts(contacts)
                .keywords(keywords)
                .recentSearches(Collections.emptyList()) // Can be implemented with user search history
                .build();
    }

    /**
     * Get all unique contacts (senders) for the user.
     * Useful for populating contact autocomplete.
     */
    public List<ContactSuggestion> getAllContacts(String userId) {
        List<EmailKanbanStatus> allEmails = emailStatusRepository.findByUserId(userId);
        return extractAllContacts(allEmails);
    }

    private List<ContactSuggestion> getContactSuggestions(List<EmailKanbanStatus> emails, String query) {
        // Group emails by sender and count
        Map<String, ContactInfo> contactMap = new HashMap<>();

        for (EmailKanbanStatus email : emails) {
            String senderEmail = email.getFromEmail();
            String senderName = email.getFromName();

            if (senderEmail == null) continue;

            String key = senderEmail.toLowerCase();
            ContactInfo info = contactMap.computeIfAbsent(key, k -> new ContactInfo(senderEmail, senderName));
            info.count++;
            
            // Update name if we have a better one
            if (senderName != null && !senderName.isEmpty() && 
                (info.name == null || info.name.isEmpty())) {
                info.name = senderName;
            }
        }

        // Filter by query and sort by count
        return contactMap.values().stream()
                .filter(info -> matchesQuery(info, query))
                .sorted((a, b) -> Integer.compare(b.count, a.count))
                .limit(MAX_CONTACT_SUGGESTIONS)
                .map(info -> ContactSuggestion.builder()
                        .email(info.email)
                        .name(info.name)
                        .emailCount(info.count)
                        .build())
                .collect(Collectors.toList());
    }

    private List<ContactSuggestion> extractAllContacts(List<EmailKanbanStatus> emails) {
        Map<String, ContactInfo> contactMap = new HashMap<>();

        for (EmailKanbanStatus email : emails) {
            String senderEmail = email.getFromEmail();
            String senderName = email.getFromName();

            if (senderEmail == null) continue;

            String key = senderEmail.toLowerCase();
            ContactInfo info = contactMap.computeIfAbsent(key, k -> new ContactInfo(senderEmail, senderName));
            info.count++;
            
            if (senderName != null && !senderName.isEmpty() && 
                (info.name == null || info.name.isEmpty())) {
                info.name = senderName;
            }
        }

        return contactMap.values().stream()
                .sorted((a, b) -> Integer.compare(b.count, a.count))
                .map(info -> ContactSuggestion.builder()
                        .email(info.email)
                        .name(info.name)
                        .emailCount(info.count)
                        .build())
                .collect(Collectors.toList());
    }

    private boolean matchesQuery(ContactInfo info, String query) {
        String email = info.email != null ? info.email.toLowerCase() : "";
        String name = info.name != null ? info.name.toLowerCase() : "";
        
        return email.contains(query) || name.contains(query);
    }

    private List<KeywordSuggestion> getKeywordSuggestions(List<EmailKanbanStatus> emails, String query) {
        // Extract keywords from subjects
        Map<String, Integer> keywordCounts = new HashMap<>();

        for (EmailKanbanStatus email : emails) {
            String subject = email.getSubject();
            if (subject == null || subject.isEmpty()) continue;

            // Extract words from subject
            String[] words = subject.toLowerCase()
                    .replaceAll("[^a-zA-Z0-9\\s]", " ")
                    .split("\\s+");

            for (String word : words) {
                // Skip short words and common words
                if (word.length() < 3 || isCommonWord(word)) continue;
                
                // Only include words that start with the query
                if (word.startsWith(query) || word.contains(query)) {
                    keywordCounts.merge(word, 1, Integer::sum);
                }
            }
        }

        // Sort by count and return top suggestions
        return keywordCounts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(MAX_KEYWORD_SUGGESTIONS)
                .map(entry -> KeywordSuggestion.builder()
                        .keyword(entry.getKey())
                        .occurrences(entry.getValue())
                        .type("subject")
                        .build())
                .collect(Collectors.toList());
    }

    private boolean isCommonWord(String word) {
        Set<String> commonWords = Set.of(
                "the", "and", "for", "are", "but", "not", "you", "all",
                "can", "had", "her", "was", "one", "our", "out", "has",
                "his", "how", "its", "may", "new", "now", "old", "see",
                "way", "who", "did", "get", "let", "put", "say", "she",
                "too", "use", "from", "have", "this", "that", "with",
                "your", "will", "been", "more", "when", "some", "them",
                "into", "than", "then", "what", "just", "only", "come",
                "made", "find", "here", "many", "make", "like", "time",
                "very", "after", "most", "also", "know", "back", "first",
                "fwd", "re", "fw"
        );
        return commonWords.contains(word);
    }

    private static class ContactInfo {
        String email;
        String name;
        int count;

        ContactInfo(String email, String name) {
            this.email = email;
            this.name = name;
            this.count = 0;
        }
    }
}

