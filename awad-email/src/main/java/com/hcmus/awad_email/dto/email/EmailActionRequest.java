package com.hcmus.awad_email.dto.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailActionRequest {
    
    private List<String> emailIds;
    
    private String action; // "read", "unread", "star", "unstar", "delete", "archive"
}

