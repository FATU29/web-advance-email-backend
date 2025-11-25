# API Logging Guide

## Overview

The backend now includes comprehensive logging for all API requests, showing:
- Which API endpoint was called
- Which user made the request
- Request parameters and details
- Response status and duration
- Client IP address and User-Agent

## Log Format

### Request Logs
```
📥 API Request | Method: POST | URL: /api/emails/actions | User: 673e5f8a1234567890abcdef | IP: 127.0.0.1 | UserAgent: Mozilla/5.0...
```

### Response Logs
```
📤 API Response | Method: POST | URL: /api/emails/actions | User: 673e5f8a1234567890abcdef | Status: 200 | Duration: 145ms
```

### Controller-Level Logs
```
📧 Email action for user: 673e5f8a1234567890abcdef | action: read | emailIds: [email-id-1, email-id-2]
✅ Email action 'read' completed successfully for user: 673e5f8a1234567890abcdef
```

## Log Symbols

| Symbol | Meaning |
|--------|---------|
| 📥 | Incoming API request |
| 📤 | Outgoing API response |
| 🔐 | Authentication/Authorization action |
| 📧 | Email-related action |
| 📬 | Mailbox-related action |
| 📎 | Attachment-related action |
| ✅ | Successful operation |
| ❌ | Failed operation |
| ⚠️ | Warning |

## Log Levels

### INFO (Default)
Shows all API requests and responses with user information:
- All incoming requests
- All outgoing responses
- Major operations (login, send email, etc.)

### DEBUG
Shows additional details:
- Request headers
- Detailed operation information
- Internal method calls

### WARN
Shows warnings and errors:
- 4xx status codes (client errors)
- Invalid actions
- Missing resources

### ERROR
Shows critical errors:
- 5xx status codes (server errors)
- Exceptions
- Failed operations

## Configuration

### Current Configuration (application.yml)
```yaml
logging:
  level:
    com.hcmus.awad_email: INFO
    com.hcmus.awad_email.config.RequestLoggingInterceptor: INFO
    com.hcmus.awad_email.controller: INFO
    org.springframework.security: WARN
    org.springframework.web: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} %highlight(%-5level) %magenta([%thread]) %cyan(%logger{36}) : %msg%n"
```

### Change Log Level

#### Option 1: Environment Variable (.env file)
```bash
# Set to DEBUG for more detailed logs
LOG_LEVEL=DEBUG

# Or set to WARN for less verbose logs
LOG_LEVEL=WARN
```

#### Option 2: application.yml
```yaml
logging:
  level:
    com.hcmus.awad_email: DEBUG  # Change this line
```

## Example Logs

### 1. User Login
```
2025-11-26 00:30:15.123 INFO  [http-nio-8080-exec-1] RequestLoggingInterceptor : 📥 API Request | Method: POST | URL: /api/auth/login | User: anonymous | IP: 127.0.0.1 | UserAgent: Mozilla/5.0...
2025-11-26 00:30:15.234 INFO  [http-nio-8080-exec-1] AuthController : 🔐 Login attempt for email: user@example.com
2025-11-26 00:30:15.456 INFO  [http-nio-8080-exec-1] AuthController : ✅ Login successful for email: user@example.com
2025-11-26 00:30:15.457 INFO  [http-nio-8080-exec-1] RequestLoggingInterceptor : 📤 API Response | Method: POST | URL: /api/auth/login | User: anonymous | Status: 200 | Duration: 334ms
```

### 2. Get Mailboxes
```
2025-11-26 00:30:20.123 INFO  [http-nio-8080-exec-2] RequestLoggingInterceptor : 📥 API Request | Method: GET | URL: /api/mailboxes | User: 673e5f8a1234567890abcdef | IP: 127.0.0.1 | UserAgent: Mozilla/5.0...
2025-11-26 00:30:20.234 INFO  [http-nio-8080-exec-2] MailboxController : 📬 Get mailboxes for user: 673e5f8a1234567890abcdef
2025-11-26 00:30:20.456 INFO  [http-nio-8080-exec-2] MailboxController : ✅ Retrieved 5 mailboxes for user: 673e5f8a1234567890abcdef
2025-11-26 00:30:20.457 INFO  [http-nio-8080-exec-2] RequestLoggingInterceptor : 📤 API Response | Method: GET | URL: /api/mailboxes | User: 673e5f8a1234567890abcdef | Status: 200 | Duration: 334ms
```

### 3. Mark Email as Read
```
2025-11-26 00:30:25.123 INFO  [http-nio-8080-exec-3] RequestLoggingInterceptor : 📥 API Request | Method: POST | URL: /api/emails/actions | User: 673e5f8a1234567890abcdef | IP: 127.0.0.1 | UserAgent: Mozilla/5.0...
2025-11-26 00:30:25.234 INFO  [http-nio-8080-exec-3] EmailController : 📧 Email action for user: 673e5f8a1234567890abcdef | action: read | emailIds: [email-id-1]
2025-11-26 00:30:25.456 INFO  [http-nio-8080-exec-3] EmailController : ✅ Email action 'read' completed successfully for user: 673e5f8a1234567890abcdef
2025-11-26 00:30:25.457 INFO  [http-nio-8080-exec-3] RequestLoggingInterceptor : 📤 API Response | Method: POST | URL: /api/emails/actions | User: 673e5f8a1234567890abcdef | Status: 200 | Duration: 334ms
```

### 4. Invalid Action (Error)
```
2025-11-26 00:30:30.123 INFO  [http-nio-8080-exec-4] RequestLoggingInterceptor : 📥 API Request | Method: POST | URL: /api/emails/actions | User: 673e5f8a1234567890abcdef | IP: 127.0.0.1 | UserAgent: Mozilla/5.0...
2025-11-26 00:30:30.234 INFO  [http-nio-8080-exec-4] EmailController : 📧 Email action for user: 673e5f8a1234567890abcdef | action: MARK_READ | emailIds: [email-id-1]
2025-11-26 00:30:30.235 WARN  [http-nio-8080-exec-4] EmailController : ❌ Invalid email action: MARK_READ for user: 673e5f8a1234567890abcdef
2025-11-26 00:30:30.236 WARN  [http-nio-8080-exec-4] RequestLoggingInterceptor : 📤 API Response | Method: POST | URL: /api/emails/actions | User: 673e5f8a1234567890abcdef | Status: 400 | Duration: 113ms
```

### 5. Send Email
```
2025-11-26 00:30:35.123 INFO  [http-nio-8080-exec-5] RequestLoggingInterceptor : 📥 API Request | Method: POST | URL: /api/emails/send | User: 673e5f8a1234567890abcdef | IP: 127.0.0.1 | UserAgent: Mozilla/5.0...
2025-11-26 00:30:35.234 INFO  [http-nio-8080-exec-5] EmailController : 📧 Send email for user: 673e5f8a1234567890abcdef | to: [recipient@example.com] | subject: Test Email
2025-11-26 00:30:35.789 INFO  [http-nio-8080-exec-5] EmailController : ✅ Email sent successfully from user: 673e5f8a1234567890abcdef to: [recipient@example.com]
2025-11-26 00:30:35.790 INFO  [http-nio-8080-exec-5] RequestLoggingInterceptor : 📤 API Response | Method: POST | URL: /api/emails/send | User: 673e5f8a1234567890abcdef | Status: 200 | Duration: 667ms
```

## Logged Information by Endpoint

### Authentication Endpoints (`/api/auth/*`)
- **POST /api/auth/signup**: Email address
- **POST /api/auth/login**: Email address
- **POST /api/auth/google**: Google OAuth login
- **POST /api/auth/logout**: User ID
- **GET /api/auth/me**: User ID

### Mailbox Endpoints (`/api/mailboxes/*`)
- **GET /api/mailboxes**: User ID, number of mailboxes retrieved

### Email Endpoints (`/api/emails/*`)
- **GET /api/mailboxes/{mailboxId}/emails**: User ID, mailbox ID, page, size
- **GET /api/emails/{emailId}**: User ID, email ID, subject, sender
- **POST /api/emails/actions**: User ID, action type, email IDs
- **POST /api/emails/send**: User ID, recipients, subject
- **POST /api/emails/{emailId}/reply**: User ID, email ID, reply all flag

### Attachment Endpoints (`/api/attachments/*`)
- **GET /api/attachments/{messageId}/{attachmentId}**: User ID, message ID, attachment ID, filename

## Filtering Logs

### View Only API Requests
```bash
./mvnw spring-boot:run | grep "📥 API Request"
```

### View Only Errors
```bash
./mvnw spring-boot:run | grep "ERROR"
```

### View Logs for Specific User
```bash
./mvnw spring-boot:run | grep "673e5f8a1234567890abcdef"
```

### View Logs for Specific Endpoint
```bash
./mvnw spring-boot:run | grep "/api/emails/actions"
```

## Debugging Tips

### Enable Debug Logging for Specific Package
```yaml
logging:
  level:
    com.hcmus.awad_email.controller.EmailController: DEBUG
```

### View Request Headers
Set log level to DEBUG to see all request headers:
```yaml
logging:
  level:
    com.hcmus.awad_email.config.RequestLoggingInterceptor: DEBUG
```

### Disable Request Logging
To disable the request logging interceptor, comment out the registration in `WebMvcConfig.java`:
```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    // registry.addInterceptor(requestLoggingInterceptor)
    //         .addPathPatterns("/api/**")
    //         .excludePathPatterns("/api/health");
}
```

## Performance Considerations

The logging interceptor adds minimal overhead:
- Request logging: ~1-2ms
- Response logging: ~1-2ms
- Total overhead: ~2-4ms per request

For production environments, consider:
1. Setting log level to WARN or ERROR
2. Using async logging
3. Excluding high-frequency endpoints (like health checks)

## Security Considerations

The logging system automatically:
- ✅ Masks Authorization headers (shows only first 20 characters)
- ✅ Logs user IDs instead of sensitive user data
- ✅ Does not log request/response bodies by default
- ✅ Logs client IP for security auditing

**Never log:**
- Passwords
- Full JWT tokens
- Credit card numbers
- Personal identification numbers

## Troubleshooting

### Logs Not Appearing
1. Check log level in `application.yml`
2. Verify `.env` file has correct `LOG_LEVEL`
3. Restart the application

### Too Many Logs
1. Increase log level to WARN or ERROR
2. Exclude specific packages from logging
3. Disable DEBUG logging

### Want More Details
1. Set log level to DEBUG
2. Enable request header logging
3. Add custom logging in service layer

## Summary

The logging system provides:
- ✅ Complete visibility into API usage
- ✅ User tracking for all requests
- ✅ Performance monitoring (request duration)
- ✅ Error tracking and debugging
- ✅ Security auditing (IP addresses)
- ✅ Easy filtering and searching

All logs include user information, making it easy to track which user is calling which API and troubleshoot issues.

