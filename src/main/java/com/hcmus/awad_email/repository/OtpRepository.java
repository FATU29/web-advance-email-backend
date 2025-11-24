package com.hcmus.awad_email.repository;

import com.hcmus.awad_email.model.Otp;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpRepository extends MongoRepository<Otp, String> {
    
    Optional<Otp> findByEmailAndTypeAndUsedFalseAndExpiryTimeAfter(
            String email, 
            Otp.OtpType type, 
            LocalDateTime currentTime
    );
    
    void deleteByEmail(String email);
    
    void deleteByExpiryTimeBefore(LocalDateTime currentTime);
}

