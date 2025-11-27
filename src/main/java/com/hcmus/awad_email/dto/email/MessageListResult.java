package com.hcmus.awad_email.dto.email;

import com.google.api.services.gmail.model.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wrapper class to hold Gmail API list messages response
 * Contains both the messages and pagination token
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageListResult {
    
    private List<Message> messages;
    
    private String nextPageToken;
    
    private Long resultSizeEstimate;
}

