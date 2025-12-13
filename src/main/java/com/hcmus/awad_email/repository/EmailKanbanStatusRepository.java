package com.hcmus.awad_email.repository;

import com.hcmus.awad_email.model.EmailKanbanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailKanbanStatusRepository extends MongoRepository<EmailKanbanStatus, String> {

    Optional<EmailKanbanStatus> findByUserIdAndEmailId(String userId, String emailId);

    List<EmailKanbanStatus> findByUserIdAndColumnIdOrderByOrderInColumnAsc(String userId, String columnId);

    Page<EmailKanbanStatus> findByUserIdAndColumnId(String userId, String columnId, Pageable pageable);

    List<EmailKanbanStatus> findBySnoozedTrueAndSnoozeUntilBefore(LocalDateTime dateTime);

    long countByUserIdAndColumnId(String userId, String columnId);

    void deleteByUserIdAndEmailId(String userId, String emailId);

    void deleteByUserIdAndColumnId(String userId, String columnId);

    boolean existsByUserIdAndEmailId(String userId, String emailId);

    /**
     * Find all email IDs that are already in the Kanban board for a user.
     */
    List<EmailKanbanStatus> findByUserId(String userId);

    /**
     * Find all email IDs in a specific column for a user.
     */
    List<EmailKanbanStatus> findByUserIdAndEmailIdIn(String userId, List<String> emailIds);
}

