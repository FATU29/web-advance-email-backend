package com.hcmus.awad_email.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sendinblue.ApiClient;
import sendinblue.ApiException;
import sendinblue.Configuration;
import sendinblue.auth.ApiKeyAuth;
import sibApi.TransactionalEmailsApi;
import sibModel.SendSmtpEmail;
import sibModel.SendSmtpEmailSender;
import sibModel.SendSmtpEmailTo;

import java.util.Collections;

@Service
public class BrevoEmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(BrevoEmailService.class);
    
    private final TransactionalEmailsApi apiInstance;
    private final String senderEmail;
    private final String senderName;
    
    public BrevoEmailService(
            @Value("${app.api-key.brevo}") String brevoApiKey,
            @Value("${app.email.sender.email}") String senderEmail,
            @Value("${app.email.sender.name}") String senderName) {
        
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        
        // Configure Brevo API client
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        ApiKeyAuth apiKey = (ApiKeyAuth) defaultClient.getAuthentication("api-key");
        apiKey.setApiKey(brevoApiKey);
        
        this.apiInstance = new TransactionalEmailsApi();
    }
    
    /**
     * Send OTP email for account verification
     */
    public void sendOtpEmail(String toEmail, String recipientName, String otpCode, String purpose) {
        try {
            SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
            
            // Set sender
            SendSmtpEmailSender sender = new SendSmtpEmailSender();
            sender.setEmail(senderEmail);
            sender.setName(senderName);
            sendSmtpEmail.setSender(sender);
            
            // Set recipient
            SendSmtpEmailTo recipient = new SendSmtpEmailTo();
            recipient.setEmail(toEmail);
            recipient.setName(recipientName);
            sendSmtpEmail.setTo(Collections.singletonList(recipient));
            
            // Set subject based on purpose
            String subject = getSubjectForPurpose(purpose);
            sendSmtpEmail.setSubject(subject);
            
            // Set HTML content
            String htmlContent = buildOtpEmailHtml(recipientName, otpCode, purpose);
            sendSmtpEmail.setHtmlContent(htmlContent);
            
            // Send email
            apiInstance.sendTransacEmail(sendSmtpEmail);
            logger.info("OTP email sent successfully to: {}", toEmail);
            
        } catch (ApiException e) {
            logger.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage());
        }
    }
    
    /**
     * Send password reset confirmation email
     */
    public void sendPasswordResetConfirmationEmail(String toEmail, String recipientName) {
        try {
            SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
            
            // Set sender
            SendSmtpEmailSender sender = new SendSmtpEmailSender();
            sender.setEmail(senderEmail);
            sender.setName(senderName);
            sendSmtpEmail.setSender(sender);
            
            // Set recipient
            SendSmtpEmailTo recipient = new SendSmtpEmailTo();
            recipient.setEmail(toEmail);
            recipient.setName(recipientName);
            sendSmtpEmail.setTo(Collections.singletonList(recipient));
            
            // Set subject
            sendSmtpEmail.setSubject("Password Reset Successful");
            
            // Set HTML content
            String htmlContent = buildPasswordResetConfirmationHtml(recipientName);
            sendSmtpEmail.setHtmlContent(htmlContent);
            
            // Send email
            apiInstance.sendTransacEmail(sendSmtpEmail);
            logger.info("Password reset confirmation email sent successfully to: {}", toEmail);
            
        } catch (ApiException e) {
            logger.error("Failed to send password reset confirmation email to {}: {}", toEmail, e.getMessage());
            // Don't throw exception for confirmation emails
        }
    }
    
    private String getSubjectForPurpose(String purpose) {
        return switch (purpose.toUpperCase()) {
            case "SIGNUP" -> "Verify Your Email Address";
            case "FORGOT_PASSWORD" -> "Reset Your Password";
            case "CHANGE_PASSWORD" -> "Verify Password Change";
            default -> "Verification Code";
        };
    }
    
    private String buildOtpEmailHtml(String recipientName, String otpCode, String purpose) {
        String purposeText = switch (purpose.toUpperCase()) {
            case "SIGNUP" -> "verify your email address";
            case "FORGOT_PASSWORD" -> "reset your password";
            case "CHANGE_PASSWORD" -> "change your password";
            default -> "verify your account";
        };
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4F46E5; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px; }
                    .otp-code { font-size: 32px; font-weight: bold; color: #4F46E5; text-align: center; padding: 20px; background-color: white; border-radius: 5px; margin: 20px 0; letter-spacing: 5px; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>%s</h1>
                    </div>
                    <div class="content">
                        <p>Hello %s,</p>
                        <p>You requested to %s. Please use the following verification code:</p>
                        <div class="otp-code">%s</div>
                        <p>This code will expire in 60 seconds.</p>
                        <p>If you didn't request this, please ignore this email.</p>
                        <p>Best regards,<br>The %s Team</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated email. Please do not reply.</p>
                    </div>
                </div>
            </body>
            </html>
            """, 
            getSubjectForPurpose(purpose),
            recipientName,
            purposeText,
            otpCode,
            senderName
        );
    }
    
    private String buildPasswordResetConfirmationHtml(String recipientName) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #10B981; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Password Reset Successful</h1>
                    </div>
                    <div class="content">
                        <p>Hello %s,</p>
                        <p>Your password has been successfully reset.</p>
                        <p>If you didn't make this change, please contact our support team immediately.</p>
                        <p>Best regards,<br>The %s Team</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated email. Please do not reply.</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            recipientName,
            senderName
        );
    }
}

