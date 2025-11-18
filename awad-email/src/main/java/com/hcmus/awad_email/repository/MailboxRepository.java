package com.hcmus.awad_email.repository;

import com.hcmus.awad_email.model.Mailbox;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MailboxRepository extends MongoRepository<Mailbox, String> {
    
    List<Mailbox> findByUserId(String userId);
    
    Optional<Mailbox> findByUserIdAndType(String userId, Mailbox.MailboxType type);
    
    Optional<Mailbox> findByIdAndUserId(String id, String userId);
}

