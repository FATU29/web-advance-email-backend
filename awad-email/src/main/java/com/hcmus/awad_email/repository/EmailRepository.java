package com.hcmus.awad_email.repository;

import com.hcmus.awad_email.model.Email;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailRepository extends MongoRepository<Email, String> {
    
    Page<Email> findByUserIdAndMailboxId(String userId, String mailboxId, Pageable pageable);
    
    Optional<Email> findByIdAndUserId(String id, String userId);
    
    long countByUserIdAndMailboxIdAndIsRead(String userId, String mailboxId, boolean isRead);
}

