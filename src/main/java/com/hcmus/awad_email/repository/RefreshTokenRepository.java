package com.hcmus.awad_email.repository;

import com.hcmus.awad_email.model.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {
    
    Optional<RefreshToken> findByToken(String token);
    
    void deleteByUserId(String userId);
    
    void deleteByToken(String token);
}

