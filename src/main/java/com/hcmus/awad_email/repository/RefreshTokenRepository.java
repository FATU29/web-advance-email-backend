package com.hcmus.awad_email.repository;

import com.hcmus.awad_email.model.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {

    /**
     * Find refresh token by its hashed value.
     * The token field now stores SHA-256 hash of the actual token.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void deleteByUserId(String userId);

    void deleteByTokenHash(String tokenHash);
}

