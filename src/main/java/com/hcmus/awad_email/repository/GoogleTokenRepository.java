package com.hcmus.awad_email.repository;

import com.hcmus.awad_email.model.GoogleToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GoogleTokenRepository extends MongoRepository<GoogleToken, String> {
    
    Optional<GoogleToken> findByUserId(String userId);
    
    void deleteByUserId(String userId);
}

