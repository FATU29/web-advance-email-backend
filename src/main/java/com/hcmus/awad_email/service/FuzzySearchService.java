package com.hcmus.awad_email.service;

import com.hcmus.awad_email.dto.kanban.FuzzySearchRequest;
import com.hcmus.awad_email.dto.kanban.FuzzySearchResponse;
import com.hcmus.awad_email.dto.kanban.FuzzySearchResponse.SearchResultItem;
import com.hcmus.awad_email.model.EmailKanbanStatus;
import com.hcmus.awad_email.model.KanbanColumn;
import com.hcmus.awad_email.repository.EmailKanbanStatusRepository;
import com.hcmus.awad_email.repository.KanbanColumnRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for fuzzy search functionality on emails.
 * Implements typo tolerance and partial matching using Levenshtein distance
 * and n-gram based similarity scoring.
 */
@Service
@Slf4j
public class FuzzySearchService {

    @Autowired
    private EmailKanbanStatusRepository emailStatusRepository;

    @Autowired
    private KanbanColumnRepository columnRepository;

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final double MIN_SCORE_THRESHOLD = 0.5; // Increased from 0.3 for stricter matching
    private static final double MIN_NGRAM_OVERLAP = 0.6; // Minimum n-gram overlap required
    private static final int MAX_LEVENSHTEIN_DISTANCE = 2; // Max typos allowed

    /**
     * Perform fuzzy search on emails for a user.
     * Searches over subject and sender (name or email) with typo tolerance and partial matches.
     * Results are ranked by relevance.
     */
    public FuzzySearchResponse search(String userId, FuzzySearchRequest request) {
        String query = request.getQuery();
        if (query == null || query.trim().isEmpty()) {
            return FuzzySearchResponse.builder()
                    .query("")
                    .totalResults(0)
                    .results(Collections.emptyList())
                    .build();
        }

        query = query.trim().toLowerCase();
        int limit = request.getLimit() != null ? Math.min(request.getLimit(), MAX_LIMIT) : DEFAULT_LIMIT;
        boolean includeBody = request.getIncludeBody() != null && request.getIncludeBody();

        // Get all emails for the user
        List<EmailKanbanStatus> allEmails = emailStatusRepository.findByUserId(userId);

        // Get column names for response
        Map<String, String> columnNames = columnRepository.findByUserIdOrderByOrderAsc(userId)
                .stream()
                .collect(Collectors.toMap(KanbanColumn::getId, KanbanColumn::getName));

        // Score and rank emails
        List<ScoredEmail> scoredEmails = new ArrayList<>();
        final String finalQuery = query;

        for (EmailKanbanStatus email : allEmails) {
            ScoredEmail scored = scoreEmail(email, finalQuery, includeBody);
            if (scored.score >= MIN_SCORE_THRESHOLD) {
                scoredEmails.add(scored);
            }
        }

        // Sort by score descending
        scoredEmails.sort((a, b) -> Double.compare(b.score, a.score));

        // Limit results
        List<SearchResultItem> results = scoredEmails.stream()
                .limit(limit)
                .map(scored -> toSearchResultItem(scored, columnNames))
                .collect(Collectors.toList());

        log.info("Fuzzy search for user {} with query '{}' found {} results", userId, request.getQuery(), results.size());

        return FuzzySearchResponse.builder()
                .query(request.getQuery())
                .totalResults(results.size())
                .results(results)
                .build();
    }

    /**
     * Score an email against the search query.
     */
    private ScoredEmail scoreEmail(EmailKanbanStatus email, String query, boolean includeBody) {
        double maxScore = 0;
        List<String> matchedFields = new ArrayList<>();

        // Score subject (highest weight)
        if (email.getSubject() != null) {
            double subjectScore = calculateSimilarity(email.getSubject().toLowerCase(), query) * 1.5;
            if (subjectScore > MIN_SCORE_THRESHOLD) {
                matchedFields.add("subject");
                maxScore = Math.max(maxScore, subjectScore);
            }
        }

        // Score sender name
        if (email.getFromName() != null) {
            double nameScore = calculateSimilarity(email.getFromName().toLowerCase(), query) * 1.3;
            if (nameScore > MIN_SCORE_THRESHOLD) {
                matchedFields.add("fromName");
                maxScore = Math.max(maxScore, nameScore);
            }
        }

        // Score sender email
        if (email.getFromEmail() != null) {
            double emailScore = calculateSimilarity(email.getFromEmail().toLowerCase(), query) * 1.2;
            if (emailScore > MIN_SCORE_THRESHOLD) {
                matchedFields.add("fromEmail");
                maxScore = Math.max(maxScore, emailScore);
            }
        }

        // Optionally score body/preview/summary
        if (includeBody) {
            if (email.getPreview() != null) {
                double previewScore = calculateSimilarity(email.getPreview().toLowerCase(), query) * 0.8;
                if (previewScore > MIN_SCORE_THRESHOLD) {
                    matchedFields.add("preview");
                    maxScore = Math.max(maxScore, previewScore);
                }
            }
            if (email.getSummary() != null) {
                double summaryScore = calculateSimilarity(email.getSummary().toLowerCase(), query) * 0.9;
                if (summaryScore > MIN_SCORE_THRESHOLD) {
                    matchedFields.add("summary");
                    maxScore = Math.max(maxScore, summaryScore);
                }
            }
        }

        return new ScoredEmail(email, maxScore, matchedFields);
    }

    /**
     * Calculate similarity between text and query using multiple techniques.
     * Stricter matching to avoid false positives.
     */
    private double calculateSimilarity(String text, String query) {
        if (text == null || text.isEmpty()) return 0;

        // Exact match (highest score)
        if (text.equals(query)) return 1.0;

        // Contains exact query (high score)
        if (text.contains(query)) return 0.9;

        // Check if query is a prefix of any word (minimum 3 chars to avoid false positives)
        String[] words = text.split("\\s+");
        for (String word : words) {
            if (query.length() >= 3 && word.startsWith(query)) return 0.85;
        }

        // Check if any word starts with query (partial word match)
        for (String word : words) {
            if (query.length() >= 3 && word.length() >= query.length() && word.startsWith(query)) {
                return 0.8;
            }
        }

        // Levenshtein-based similarity for typo tolerance (stricter)
        double levenshteinScore = levenshteinSimilarity(text, query);
        if (levenshteinScore >= MIN_SCORE_THRESHOLD) {
            return levenshteinScore;
        }

        // N-gram similarity only if there's significant overlap
        double ngramScore = ngramSimilarity(text, query, 3); // Use 3-grams for stricter matching
        if (ngramScore >= MIN_NGRAM_OVERLAP) {
            return ngramScore * 0.7; // Reduce weight of n-gram matches
        }

        // Word-level matching for multi-word queries
        if (query.contains(" ")) {
            double wordScore = wordMatchScore(text, query);
            if (wordScore >= MIN_SCORE_THRESHOLD) {
                return wordScore;
            }
        }

        return 0; // No match
    }

    /**
     * Calculate n-gram similarity between two strings.
     */
    private double ngramSimilarity(String text, String query, int n) {
        Set<String> textNgrams = generateNgrams(text, n);
        Set<String> queryNgrams = generateNgrams(query, n);

        if (queryNgrams.isEmpty()) return 0;

        Set<String> intersection = new HashSet<>(textNgrams);
        intersection.retainAll(queryNgrams);

        return (double) intersection.size() / queryNgrams.size();
    }

    /**
     * Generate n-grams from a string.
     */
    private Set<String> generateNgrams(String text, int n) {
        Set<String> ngrams = new HashSet<>();
        if (text.length() < n) {
            ngrams.add(text);
            return ngrams;
        }
        for (int i = 0; i <= text.length() - n; i++) {
            ngrams.add(text.substring(i, i + n));
        }
        return ngrams;
    }

    /**
     * Calculate Levenshtein-based similarity.
     * Checks each word in the text against the query.
     * Only returns high score if the edit distance is small (typo tolerance).
     */
    private double levenshteinSimilarity(String text, String query) {
        String[] words = text.split("\\s+");
        double maxSimilarity = 0;

        for (String word : words) {
            // Skip very short words to avoid false positives
            if (word.length() < 3 || query.length() < 3) continue;

            int distance = levenshteinDistance(word, query);

            // Only consider as match if edit distance is small (typo tolerance)
            // Allow 1 typo for words up to 5 chars, 2 typos for longer words
            int allowedDistance = query.length() <= 5 ? 1 : MAX_LEVENSHTEIN_DISTANCE;

            if (distance <= allowedDistance) {
                int maxLen = Math.max(word.length(), query.length());
                double similarity = 1.0 - ((double) distance / maxLen);
                maxSimilarity = Math.max(maxSimilarity, similarity);
            }
        }

        return maxSimilarity;
    }

    /**
     * Calculate Levenshtein distance between two strings.
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                    dp[i][j] = Math.min(
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                            dp[i - 1][j - 1] + cost
                    );
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * Calculate word-level match score.
     * Useful for multi-word queries. Stricter matching.
     */
    private double wordMatchScore(String text, String query) {
        String[] queryWords = query.split("\\s+");
        String[] textWords = text.split("\\s+");

        int matchedWords = 0;
        for (String qWord : queryWords) {
            // Skip very short query words
            if (qWord.length() < 3) continue;

            for (String tWord : textWords) {
                // Skip very short text words
                if (tWord.length() < 3) continue;

                // Exact word match or contains (query word must be substantial part)
                if (tWord.equals(qWord) ||
                    (tWord.contains(qWord) && qWord.length() >= tWord.length() / 2)) {
                    matchedWords++;
                    break;
                }

                // Check for typo tolerance (stricter: max 1-2 char difference)
                int allowedDistance = qWord.length() <= 5 ? 1 : 2;
                if (levenshteinDistance(tWord, qWord) <= allowedDistance) {
                    matchedWords++;
                    break;
                }
            }
        }

        return queryWords.length > 0 ? (double) matchedWords / queryWords.length : 0;
    }

    /**
     * Convert scored email to search result item.
     */
    private SearchResultItem toSearchResultItem(ScoredEmail scored, Map<String, String> columnNames) {
        EmailKanbanStatus email = scored.email;
        return SearchResultItem.builder()
                .id(email.getId())
                .emailId(email.getEmailId())
                .columnId(email.getColumnId())
                .columnName(columnNames.getOrDefault(email.getColumnId(), "Unknown"))
                .subject(email.getSubject())
                .fromEmail(email.getFromEmail())
                .fromName(email.getFromName())
                .preview(email.getPreview())
                .summary(email.getSummary())
                .receivedAt(email.getReceivedAt())
                .isRead(email.isRead())
                .isStarred(email.isStarred())
                .hasAttachments(email.isHasAttachments())
                .score(scored.score)
                .matchedFields(scored.matchedFields)
                .build();
    }

    /**
     * Internal class to hold scored email results.
     */
    private static class ScoredEmail {
        final EmailKanbanStatus email;
        final double score;
        final List<String> matchedFields;

        ScoredEmail(EmailKanbanStatus email, double score, List<String> matchedFields) {
            this.email = email;
            this.score = score;
            this.matchedFields = matchedFields;
        }
    }
}
