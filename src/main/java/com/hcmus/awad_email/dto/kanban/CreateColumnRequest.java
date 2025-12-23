package com.hcmus.awad_email.dto.kanban;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateColumnRequest {

    @NotBlank(message = "Column name is required")
    @Size(min = 1, max = 50, message = "Column name must be between 1 and 50 characters")
    private String name;

    private String color;

    private Integer order; // Optional, will be placed at the end if not specified

    // Gmail label mapping
    private String gmailLabelId; // Gmail label ID to map to this column

    private String gmailLabelName; // Gmail label name for display

    // Labels to remove when email is moved to this column
    private List<String> removeLabelsOnMove;

    // Labels to add when email is moved to this column
    private List<String> addLabelsOnMove;
}

