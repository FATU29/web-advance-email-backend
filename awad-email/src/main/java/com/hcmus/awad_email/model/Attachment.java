package com.hcmus.awad_email.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {
    
    private String id;
    
    private String filename;
    
    private String mimeType;
    
    private long size; // in bytes
    
    private String url; // URL to download the attachment (mock for now)
}

