package com.hcmus.awad_email.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import com.hcmus.awad_email.dto.email.MessageListResult;
import com.hcmus.awad_email.exception.BadRequestException;
import com.hcmus.awad_email.exception.UnauthorizedException;
import com.hcmus.awad_email.model.GoogleToken;
import com.hcmus.awad_email.repository.GoogleTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for interacting with Gmail API
 * Handles token refresh, API calls, and data transformation
 */
@Service
@Slf4j
public class GmailService {
    
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Arrays.asList(
            "https://www.googleapis.com/auth/gmail.readonly",
            "https://www.googleapis.com/auth/gmail.modify",
            "https://www.googleapis.com/auth/gmail.send",
            "https://www.googleapis.com/auth/gmail.labels"
    );
    
    @Value("${app.google.client-id}")
    private String clientId;
    
    @Value("${app.google.client-secret}")
    private String clientSecret;
    
    @Value("${app.google.redirect-uri}")
    private String redirectUri;
    
    @Autowired
    private GoogleTokenRepository googleTokenRepository;
    
    /**
     * Store tokens from GoogleTokenResponse (already exchanged)
     * This method should be used when you already have a GoogleTokenResponse
     * to avoid trying to exchange the authorization code twice
     */
    public GoogleToken storeTokensFromResponse(String userId, GoogleTokenResponse tokenResponse) {
        try {
            // Validate that the token has Gmail scopes
            String grantedScope = tokenResponse.getScope();
            if (grantedScope == null || grantedScope.isEmpty()) {
                log.warn("No scopes granted in token response. Gmail API access may not work.");
            } else {
                log.info("Granted scopes: {}", grantedScope);

                // Check if Gmail scopes are present
                boolean hasGmailScopes = SCOPES.stream()
                        .anyMatch(scope -> grantedScope.contains(scope));

                if (!hasGmailScopes) {
                    log.warn("Gmail scopes not found in granted scopes. User may not have granted Gmail permissions.");
                    log.warn("Required scopes: {}", String.join(", ", SCOPES));
                    log.warn("Granted scopes: {}", grantedScope);
                }
            }

            // Calculate expiration time
            LocalDateTime expiresAt = LocalDateTime.now()
                    .plusSeconds(tokenResponse.getExpiresInSeconds());

            // Store or update tokens
            GoogleToken googleToken = googleTokenRepository.findByUserId(userId)
                    .orElse(GoogleToken.builder()
                            .userId(userId)
                            .createdAt(LocalDateTime.now())
                            .build());

            googleToken.setAccessToken(tokenResponse.getAccessToken());
            googleToken.setRefreshToken(tokenResponse.getRefreshToken());
            googleToken.setAccessTokenExpiresAt(expiresAt);
            // Store the actual granted scopes, not the requested ones
            googleToken.setScope(grantedScope != null ? grantedScope : "");
            googleToken.setUpdatedAt(LocalDateTime.now());

            return googleTokenRepository.save(googleToken);

        } catch (Exception e) {
            log.error("Failed to store Gmail tokens", e);
            throw new UnauthorizedException("Failed to store Gmail tokens: " + e.getMessage());
        }
    }

    /**
     * Exchange authorization code for tokens and store them
     * @deprecated Use storeTokensFromResponse() instead when you already have a GoogleTokenResponse
     * to avoid exchanging the authorization code twice (codes are single-use only)
     */
    @Deprecated
    public GoogleToken exchangeCodeForTokens(String userId, String authorizationCode) {
        try {
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport, JSON_FACTORY, clientId, clientSecret, SCOPES)
                    .setAccessType("offline")
                    .setApprovalPrompt("force")
                    .build();

            GoogleTokenResponse tokenResponse = flow.newTokenRequest(authorizationCode)
                    .setRedirectUri(redirectUri)
                    .execute();

            return storeTokensFromResponse(userId, tokenResponse);

        } catch (Exception e) {
            log.error("Failed to exchange authorization code for tokens", e);
            throw new UnauthorizedException("Failed to exchange authorization code: " + e.getMessage());
        }
    }
    
    /**
     * Get Gmail service instance with valid access token
     * Automatically refreshes token if expired
     */
    public Gmail getGmailService(String userId) {
        try {
            GoogleToken googleToken = googleTokenRepository.findByUserId(userId)
                    .orElseThrow(() -> new UnauthorizedException("Gmail not connected for this user"));
            
            // Refresh token if expired
            if (googleToken.isAccessTokenExpired()) {
                googleToken = refreshAccessToken(googleToken);
            }
            
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            
            GoogleCredential credential = new GoogleCredential.Builder()
                    .setTransport(httpTransport)
                    .setJsonFactory(JSON_FACTORY)
                    .setClientSecrets(clientId, clientSecret)
                    .build()
                    .setAccessToken(googleToken.getAccessToken())
                    .setRefreshToken(googleToken.getRefreshToken());
            
            return new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName("AWAD Email Client")
                    .build();
            
        } catch (Exception e) {
            log.error("Failed to create Gmail service", e);
            throw new UnauthorizedException("Failed to access Gmail: " + e.getMessage());
        }
    }
    
    /**
     * Refresh access token using refresh token
     */
    private GoogleToken refreshAccessToken(GoogleToken googleToken) {
        try {
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            
            GoogleCredential credential = new GoogleCredential.Builder()
                    .setTransport(httpTransport)
                    .setJsonFactory(JSON_FACTORY)
                    .setClientSecrets(clientId, clientSecret)
                    .build()
                    .setRefreshToken(googleToken.getRefreshToken());
            
            credential.refreshToken();
            
            googleToken.setAccessToken(credential.getAccessToken());
            googleToken.setAccessTokenExpiresAt(LocalDateTime.now().plusSeconds(3600)); // 1 hour
            googleToken.setUpdatedAt(LocalDateTime.now());
            
            return googleTokenRepository.save(googleToken);
            
        } catch (Exception e) {
            log.error("Failed to refresh access token", e);
            throw new UnauthorizedException("Failed to refresh Gmail access token: " + e.getMessage());
        }
    }
    
    /**
     * List Gmail labels (mailboxes)
     */
    public List<Label> listLabels(String userId) {
        try {
            Gmail service = getGmailService(userId);
            ListLabelsResponse response = service.users().labels().list("me").execute();
            return response.getLabels();
        } catch (IOException e) {
            log.error("Failed to list Gmail labels", e);
            throw new BadRequestException("Failed to fetch mailboxes: " + e.getMessage());
        }
    }
    
    /**
     * List messages in a label/mailbox
     * Returns MessageListResult with messages, nextPageToken, and resultSizeEstimate
     */
    public MessageListResult listMessages(String userId, String labelId, Long maxResults, String pageToken) {
        try {
            log.debug("📧 Gmail API listMessages | userId: {} | labelId: {} | maxResults: {} | pageToken: {}",
                    userId, labelId, maxResults, pageToken != null ? pageToken : "null");

            Gmail service = getGmailService(userId);

            Gmail.Users.Messages.List request = service.users().messages().list("me");

            if (labelId != null && !labelId.isEmpty()) {
                request.setLabelIds(Collections.singletonList(labelId));
            }

            if (maxResults != null) {
                request.setMaxResults(maxResults);
            }

            if (pageToken != null && !pageToken.isEmpty()) {
                request.setPageToken(pageToken);
            }

            ListMessagesResponse response = request.execute();

            log.debug("📬 Gmail API response | messages: {} | nextPageToken: {} | resultSizeEstimate: {}",
                    response.getMessages() != null ? response.getMessages().size() : 0,
                    response.getNextPageToken() != null ? response.getNextPageToken() : "null",
                    response.getResultSizeEstimate());

            if (response.getMessages() == null) {
                return MessageListResult.builder()
                        .messages(Collections.emptyList())
                        .nextPageToken(null)
                        .resultSizeEstimate(0L)
                        .build();
            }

            // Fetch full message details
            List<Message> messages = response.getMessages().stream()
                    .map(msg -> {
                        try {
                            return service.users().messages().get("me", msg.getId())
                                    .setFormat("full")
                                    .execute();
                        } catch (IOException e) {
                            log.error("Failed to fetch message: " + msg.getId(), e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return MessageListResult.builder()
                    .messages(messages)
                    .nextPageToken(response.getNextPageToken())
                    .resultSizeEstimate(response.getResultSizeEstimate())
                    .build();

        } catch (IOException e) {
            log.error("Failed to list Gmail messages", e);
            throw new BadRequestException("Failed to fetch emails: " + e.getMessage());
        }
    }
    
    /**
     * Get a single message by ID
     */
    public Message getMessage(String userId, String messageId) {
        try {
            Gmail service = getGmailService(userId);
            return service.users().messages().get("me", messageId)
                    .setFormat("full")
                    .execute();
        } catch (IOException e) {
            log.error("Failed to get Gmail message: " + messageId, e);
            throw new BadRequestException("Failed to fetch email: " + e.getMessage());
        }
    }
    
    /**
     * Modify message labels (mark read/unread, star, etc.)
     */
    public void modifyMessage(String userId, String messageId, List<String> addLabelIds, List<String> removeLabelIds) {
        try {
            Gmail service = getGmailService(userId);
            ModifyMessageRequest request = new ModifyMessageRequest()
                    .setAddLabelIds(addLabelIds)
                    .setRemoveLabelIds(removeLabelIds);
            
            service.users().messages().modify("me", messageId, request).execute();
        } catch (IOException e) {
            log.error("Failed to modify Gmail message: " + messageId, e);
            throw new BadRequestException("Failed to modify email: " + e.getMessage());
        }
    }
    
    /**
     * Send an email
     */
    public Message sendMessage(String userId, String to, String subject, String body) {
        try {
            Gmail service = getGmailService(userId);
            
            // Create email content
            String rawMessage = createEmail(to, subject, body);
            Message message = new Message();
            message.setRaw(rawMessage);
            
            return service.users().messages().send("me", message).execute();
        } catch (Exception e) {
            log.error("Failed to send Gmail message", e);
            throw new BadRequestException("Failed to send email: " + e.getMessage());
        }
    }
    
    /**
     * Create raw email content
     */
    private String createEmail(String to, String subject, String body) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        buffer.write(("To: " + to + "\r\n").getBytes());
        buffer.write(("Subject: " + subject + "\r\n").getBytes());
        buffer.write("Content-Type: text/html; charset=utf-8\r\n".getBytes());
        buffer.write("\r\n".getBytes());
        buffer.write(body.getBytes());
        
        byte[] bytes = buffer.toByteArray();
        return Base64.getUrlEncoder().encodeToString(bytes);
    }
    
    /**
     * Delete message (move to trash)
     */
    public void trashMessage(String userId, String messageId) {
        try {
            Gmail service = getGmailService(userId);
            service.users().messages().trash("me", messageId).execute();
        } catch (IOException e) {
            log.error("Failed to trash Gmail message: " + messageId, e);
            throw new BadRequestException("Failed to delete email: " + e.getMessage());
        }
    }
    
    /**
     * Check if user has Gmail connected
     */
    public boolean isGmailConnected(String userId) {
        return googleTokenRepository.findByUserId(userId).isPresent();
    }
    
    /**
     * Disconnect Gmail (remove tokens)
     */
    public void disconnectGmail(String userId) {
        googleTokenRepository.deleteByUserId(userId);
    }
}

