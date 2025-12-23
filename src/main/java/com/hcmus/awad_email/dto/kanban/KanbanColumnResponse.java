package com.hcmus.awad_email.dto.kanban;

import com.hcmus.awad_email.model.KanbanColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KanbanColumnResponse {

    private String id;

    private String name;

    private KanbanColumn.ColumnType type;

    private int order;

    private String color;

    private boolean isDefault;

    private long emailCount;

    // Gmail label mapping
    private String gmailLabelId;

    private String gmailLabelName;

    private List<String> removeLabelsOnMove;

    private List<String> addLabelsOnMove;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

