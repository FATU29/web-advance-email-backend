# Backend API Documentation

## Overview

This is the backend API for the AWAD Email application. It provides authentication (email/password and Google OAuth) and email management functionality with mock data. The system is designed to be easily integrated with Gmail API in the future.

## Technology Stack

- **Framework**: Spring Boot 3.5.7
- **Database**: MongoDB
- **Authentication**: JWT (Access Token + Refresh Token)
- **Security**: Spring Security
- **Java Version**: 21

## Setup Instructions

### Prerequisites

1. Java 21 or higher
2. MongoDB running on `localhost:27017`
3. Maven (included via wrapper)

### Configuration

Edit `src/main/resources/application.yml`:

```properties
# MongoDB Configuration
spring.data.mongodb.uri=mongodb://localhost:27017/awad_email

# JWT Configuration (Change the secret in production!)
app.jwt.secret=YourVeryLongSecretKeyForJWTTokenGenerationThatShouldBeAtLeast256BitsLongForHS256Algorithm
app.jwt.access-token-expiration-ms=900000
app.jwt.refresh-token-expiration-ms=604800000

# Google OAuth Configuration
app.google.client-id=your-google-client-id.apps.googleusercontent.com
```

### Running the Application

```bash
# Using Maven wrapper
./mvnw spring-boot:run

# Or build and run
./mvnw clean package
java -jar target/awad-email-0.0.1-SNAPSHOT.jar
```

The API will be available at `http://localhost:8080`

## API Endpoints

### Authentication Endpoints

#### 1. Sign Up (Email/Password)
```
POST /api/auth/signup
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123"
}

Response:
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "id": "user_id",
      "email": "john@example.com",
      "name": "John Doe",
      "profilePicture": null
    }
  }
}
```

#### 2. Login (Email/Password)
```
POST /api/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "password123"
}

Response: Same as signup
```

#### 3. Google Sign-In (Authorization Code Flow)
```
POST /api/auth/google
Content-Type: application/json

{
  "code": "4/0AeanS0ZZ9..."
}

Response: Same as signup
```

**Note**: This uses the OAuth 2.0 authorization code flow where:
1. Frontend redirects user to Google OAuth
2. Google redirects back with an authorization code
3. Frontend sends the code to this endpoint
4. Backend exchanges the code for tokens with Google (using client secret and configured redirect URI)
5. Backend verifies the ID token and creates/updates user

**Configuration**: The `redirectUri` is configured on the backend in `application.yml` and `.env` for security.

See [GOOGLE_OAUTH_IMPLEMENTATION.md](GOOGLE_OAUTH_IMPLEMENTATION.md) for complete setup guide.

#### 4. Refresh Token
```
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGc..."
}

Response: Same as signup (with new access token)
```

#### 5. Logout
```
POST /api/auth/logout
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "refreshToken": "eyJhbGc..."
}

Response:
{
  "success": true,
  "message": "Logout successful",
  "data": null
}
```

### Mailbox Endpoints

#### 1. Get All Mailboxes
```
GET /api/mailboxes
Authorization: Bearer {access_token}

Response:
{
  "success": true,
  "data": [
    {
      "id": "mailbox_id",
      "name": "Inbox",
      "type": "INBOX",
      "unreadCount": 5,
      "totalCount": 20,
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-01T00:00:00"
    },
    ...
  ]
}
```

#### 2. Get Mailbox by ID
```
GET /api/mailboxes/{mailboxId}
Authorization: Bearer {access_token}

Response: Single mailbox object
```

### Email Endpoints

#### 1. Get Emails in Mailbox (Paginated)
```
GET /api/mailboxes/{mailboxId}/emails?page=0&size=20
Authorization: Bearer {access_token}

Response:
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "email_id",
        "from": "sender@example.com",
        "fromName": "Sender Name",
        "subject": "Email Subject",
        "preview": "Email preview text...",
        "isRead": false,
        "isStarred": true,
        "isImportant": false,
        "hasAttachments": true,
        "receivedAt": "2024-01-01T12:00:00"
      },
      ...
    ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "last": false
  }
}
```

#### 2. Get Email Detail
```
GET /api/emails/{emailId}
Authorization: Bearer {access_token}

Response:
{
  "success": true,
  "data": {
    "id": "email_id",
    "from": "sender@example.com",
    "fromName": "Sender Name",
    "to": ["recipient@example.com"],
    "cc": ["cc@example.com"],
    "bcc": [],
    "subject": "Email Subject",
    "body": "<p>Email HTML body</p>",
    "isRead": true,
    "isStarred": false,
    "isImportant": false,
    "attachments": [
      {
        "id": "att_id",
        "filename": "document.pdf",
        "mimeType": "application/pdf",
        "size": 1024000,
        "url": "/api/attachments/att_id"
      }
    ],
    "receivedAt": "2024-01-01T12:00:00",
    "sentAt": "2024-01-01T12:00:00"
  }
}
```

#### 3. Perform Bulk Actions
```
POST /api/emails/actions
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "emailIds": ["email_id_1", "email_id_2"],
  "action": "read"  // Options: read, unread, star, unstar, delete, archive
}

Response:
{
  "success": true,
  "message": "Action performed successfully",
  "data": null
}
```

#### 4. Mark Email as Read
```
PATCH /api/emails/{emailId}/read
Authorization: Bearer {access_token}

Response: Success message
```

#### 5. Mark Email as Unread
```
PATCH /api/emails/{emailId}/unread
Authorization: Bearer {access_token}

Response: Success message
```

#### 6. Toggle Star
```
PATCH /api/emails/{emailId}/star?starred=true
Authorization: Bearer {access_token}

Response: Success message
```

#### 7. Delete Email
```
DELETE /api/emails/{emailId}
Authorization: Bearer {access_token}

Response: Success message
```

## Token Management

### Access Token
- **Expiration**: 15 minutes (900,000 ms)
- **Storage**: In-memory (not in localStorage)
- **Usage**: Sent in Authorization header for all protected endpoints

### Refresh Token
- **Expiration**: 7 days (604,800,000 ms)
- **Storage**: localStorage (or HttpOnly cookie for better security)
- **Usage**: Used to obtain new access tokens when they expire

### Token Refresh Flow
1. Frontend detects 401 Unauthorized response
2. Frontend calls `/api/auth/refresh` with refresh token
3. Backend validates refresh token and returns new access token
4. Frontend retries original request with new access token
5. If refresh fails, redirect user to login

## Mock Data

The system automatically generates mock emails when a new user signs up or logs in with Google for the first time. This includes:

- 5 sample emails in the Inbox
- Various email states (read/unread, starred, with attachments)
- Realistic email content and metadata

## Future Gmail Integration

The system is designed for easy Gmail API integration:

1. **Email Model**: Includes `gmailMessageId` and `gmailThreadId` fields
2. **Service Layer**: Mock email generation is isolated in `EmailService.initializeMockEmails()`
3. **Replace Mock Data**: Implement Gmail API calls in `EmailService` methods
4. **OAuth Scopes**: Add Gmail scopes to Google OAuth configuration
5. **Sync Strategy**: Implement periodic sync or webhook-based updates

## Error Handling

All errors return a consistent format:

```json
{
  "success": false,
  "message": "Error message",
  "data": null
}
```

HTTP Status Codes:
- `200`: Success
- `400`: Bad Request (validation errors, business logic errors)
- `401`: Unauthorized (invalid/expired token)
- `403`: Forbidden (access denied)
- `404`: Resource Not Found
- `500`: Internal Server Error

## Security Considerations

1. **JWT Secret**: Change the default secret in production
2. **CORS**: Configure allowed origins in `SecurityConfig.java`
3. **Password Hashing**: BCrypt with default strength (10 rounds)
4. **Token Storage**: Access tokens should be stored in memory, refresh tokens in secure storage
5. **HTTPS**: Use HTTPS in production
6. **Google OAuth**: Validate Google ID tokens server-side

## Development Notes

- MongoDB auto-creates indexes for better query performance
- All timestamps use UTC timezone
- Pagination defaults: page=0, size=20
- Soft delete: Emails moved to Trash before permanent deletion

