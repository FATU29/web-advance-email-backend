package com.hcmus.awad_email.service;

import com.hcmus.awad_email.exception.BadRequestException;
import com.hcmus.awad_email.model.Otp;
import com.hcmus.awad_email.repository.OtpRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OtpService {
    
    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom random = new SecureRandom();
    
    @Autowired
    private OtpRepository otpRepository;
    
    @Autowired
    private BrevoEmailService brevoEmailService;
    
    @Value("${app.otp.length}")
    private int otpLength;
    
    @Value("${app.otp.duration}")
    private int otpDurationSeconds;
    
    /**
     * Generate and send OTP to email
     */
    @Transactional
    public void generateAndSendOtp(String email, String recipientName, Otp.OtpType type) {
        // Delete any existing OTPs for this email and type
        Optional<Otp> existingOtp = otpRepository.findByEmailAndTypeAndUsedFalseAndExpiryTimeAfter(
                email, type, LocalDateTime.now()
        );
        
        existingOtp.ifPresent(otp -> otpRepository.delete(otp));
        
        // Generate new OTP code
        String otpCode = generateOtpCode();
        
        // Create and save OTP
        Otp otp = Otp.builder()
                .email(email)
                .code(otpCode)
                .type(type)
                .expiryTime(LocalDateTime.now().plusSeconds(otpDurationSeconds))
                .createdAt(LocalDateTime.now())
                .used(false)
                .attempts(0)
                .build();
        
        otpRepository.save(otp);
        
        // Send OTP email via Brevo
        try {
            brevoEmailService.sendOtpEmail(email, recipientName, otpCode, type.name());
            logger.info("OTP sent successfully to {} for {}", email, type);
        } catch (Exception e) {
            logger.error("Failed to send OTP email to {}: {}", email, e.getMessage());
            throw new BadRequestException("Failed to send OTP email. Please try again.");
        }
    }
    
    /**
     * Verify OTP code
     */
    @Transactional
    public boolean verifyOtp(String email, String code, Otp.OtpType type) {
        Optional<Otp> otpOptional = otpRepository.findByEmailAndTypeAndUsedFalseAndExpiryTimeAfter(
                email, type, LocalDateTime.now()
        );
        
        if (otpOptional.isEmpty()) {
            throw new BadRequestException("Invalid or expired OTP");
        }
        
        Otp otp = otpOptional.get();
        
        // Check if max attempts exceeded
        if (otp.getAttempts() >= MAX_ATTEMPTS) {
            otpRepository.delete(otp);
            throw new BadRequestException("Maximum verification attempts exceeded. Please request a new OTP.");
        }
        
        // Increment attempts
        otp.setAttempts(otp.getAttempts() + 1);
        otpRepository.save(otp);
        
        // Verify code
        if (!otp.getCode().equals(code)) {
            throw new BadRequestException("Invalid OTP code. Attempts remaining: " + (MAX_ATTEMPTS - otp.getAttempts()));
        }
        
        // Mark as used
        otp.setUsed(true);
        otpRepository.save(otp);
        
        logger.info("OTP verified successfully for {} - {}", email, type);
        return true;
    }
    
    /**
     * Check if valid OTP exists for email and type
     */
    public boolean hasValidOtp(String email, Otp.OtpType type) {
        return otpRepository.findByEmailAndTypeAndUsedFalseAndExpiryTimeAfter(
                email, type, LocalDateTime.now()
        ).isPresent();
    }
    
    /**
     * Delete OTP for email
     */
    @Transactional
    public void deleteOtp(String email) {
        otpRepository.deleteByEmail(email);
    }
    
    /**
     * Generate random OTP code
     */
    private String generateOtpCode() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }
    
    /**
     * Scheduled task to clean up expired OTPs
     * Runs every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    @Transactional
    public void cleanupExpiredOtps() {
        try {
            otpRepository.deleteByExpiryTimeBefore(LocalDateTime.now());
            logger.info("Cleaned up expired OTPs");
        } catch (Exception e) {
            logger.error("Failed to cleanup expired OTPs: {}", e.getMessage());
        }
    }
}

