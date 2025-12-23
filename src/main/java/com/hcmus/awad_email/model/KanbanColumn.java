package com.hcmus.awad_email.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a Kanban column for organizing emails.
 * Each user can have their own set of columns.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "kanban_columns")
@CompoundIndex(name = "user_order_idx", def = "{'userId': 1, 'order': 1}")
public class KanbanColumn {
    
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    private String name; // e.g., "Inbox", "To Do", "In Progress", "Done", "Snoozed"
    
    private ColumnType type;
    
    private int order; // Display order of the column
    
    private String color; // Optional color for the column

    private boolean isDefault; // Whether this is a default system column

    // Gmail label mapping for automatic label sync
    private String gmailLabelId; // Gmail label ID to apply when email is moved to this column

    private String gmailLabelName; // Gmail label name for display

    // Labels to remove when email is moved to this column
    private List<String> removeLabelsOnMove;

    // Labels to add when email is moved to this column
    private List<String> addLabelsOnMove;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public enum ColumnType {
        INBOX,      // Default inbox column
        BACKLOG,    // Backlog column for new Gmail emails
        TODO,       // To Do column
        IN_PROGRESS, // In Progress column
        DONE,       // Done/Completed column
        SNOOZED,    // Snoozed emails column
        CUSTOM      // User-created custom column
    }
}

