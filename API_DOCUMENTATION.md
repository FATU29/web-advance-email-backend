# AWAD Email Backend - API Documentation

Base URL: `http://localhost:8080`

## Table of Contents
1. [Authentication](#authentication)
2. [Mailboxes](#mailboxes)
3. [Emails](#emails)
4. [Attachments](#attachments)
5. [Health Check](#health-check)

---

## Authentication

### 1. Signup with Email/Password

**Endpoint:** `POST /api/auth/signup`

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "name": "John Doe"
}
```

**Validation:**
- `name`: Required, not blank
- `email`: Required, valid email format
- `password`: Required, minimum 6 characters

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Signup successful. Please check your email for verification code.",
  "data": null
}
```

**Note:** After signup, user must verify email with OTP before logging in.

---

### 2. Verify Email with OTP

**Endpoint:** `POST /api/auth/verify-email`

**Request Body:**
```json
{
  "email": "user@example.com",
  "code": "123456"
}
```

**Validation:**
- `email`: Required, valid email format
- `code`: Required, not blank

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Email verified successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": "user123",
      "email": "user@example.com",
      "name": "John Doe",
      "profilePicture": null
    }
  }
}
```

---

### 3. Resend Verification OTP

**Endpoint:** `POST /api/auth/resend-verification-otp`

**Request Body:**
```json
{
  "email": "user@example.com"
}
```

**Validation:**
- `email`: Required, valid email format

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Verification code sent to your email",
  "data": null
}
```

---

### 4. Login with Email/Password

**Endpoint:** `POST /api/auth/login`

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**Validation:**
- `email`: Required, valid email format
- `password`: Required, not blank

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": "user123",
      "email": "user@example.com",
      "name": "John Doe",
      "profilePicture": null
    }
  }
}
```

---

### 5. Google OAuth Login

**Endpoint:** `POST /api/auth/google`

**Request Body:**
```json
{
  "code": "4/0AY0e-g7..."
}
```

**Validation:**
- `code`: Required, not blank (authorization code from Google OAuth redirect)

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Google authentication successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": "user123",
      "email": "user@gmail.com",
      "name": "John Doe",
      "profilePicture": "https://lh3.googleusercontent.com/..."
    }
  }
}
```

**Note:** This endpoint also stores Gmail OAuth tokens server-side for Gmail API integration.

---

### 6. Refresh Access Token

**Endpoint:** `POST /api/auth/refresh`

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Validation:**
- `refreshToken`: Required, not blank

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

---

### 7. Logout

**Endpoint:** `POST /api/auth/logout`

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Request Body (Optional):**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Logout successful",
  "data": null
}
```

**Note:** If refresh token is provided, only that token is revoked. Otherwise, all refresh tokens for the user are revoked.

---

### 8. Get Current User Info

**Endpoint:** `GET /api/auth/me`

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "User info retrieved successfully",
  "data": {
    "id": "user123",
    "email": "user@example.com",
    "name": "John Doe",
    "profilePicture": null
  }
}
```

---

### 9. Token Introspection

**Endpoint:** `POST /api/auth/introspect`

**Request Body:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Validation:**
- `token`: Required, not blank

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Token is valid",
  "data": {
    "isValid": true
  }
}
```

**Response (Invalid Token):** `200 OK`
```json
{
  "success": true,
  "message": "Token is invalid",
  "data": {
    "isValid": false
  }
}
```

---

### 10. Forgot Password

**Endpoint:** `POST /api/auth/forgot-password`

**Request Body:**
```json
{
  "email": "user@example.com"
}
```

**Validation:**
- `email`: Required, valid email format

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Password reset code sent to your email",
  "data": null
}
```

---

### 11. Reset Password

**Endpoint:** `POST /api/auth/reset-password`

**Request Body:**
```json
{
  "email": "user@example.com",
  "code": "123456",
  "newPassword": "NewSecurePassword123!"
}
```

**Validation:**
- `email`: Required, valid email format
- `code`: Required, not blank
- `newPassword`: Required, minimum 6 characters

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Password reset successfully",
  "data": null
}
```

---

### 12. Send Change Password OTP

**Endpoint:** `POST /api/auth/send-change-password-otp`

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Verification code sent to your email",
  "data": null
}
```

**Note:** This endpoint is for authenticated users who want to change their password. It sends an OTP to their registered email.

---

### 13. Change Password (Authenticated)

**Endpoint:** `POST /api/auth/change-password`

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Request Body:**
```json
{
  "currentPassword": "OldPassword123!",
  "newPassword": "NewSecurePassword123!",
  "code": "123456"
}
```

**Validation:**
- `currentPassword`: Required, not blank
- `newPassword`: Required, minimum 6 characters
- `code`: Required, not blank (OTP from send-change-password-otp)

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Password changed successfully",
  "data": null
}
```

---

## Mailboxes

### 1. Get All Mailboxes

**Endpoint:** `GET /api/mailboxes`

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": "INBOX",
      "name": "INBOX",
      "type": "INBOX",
      "unreadCount": 5,
      "totalCount": 120,
      "createdAt": "2024-11-20T10:00:00",
      "updatedAt": "2024-11-26T10:30:00"
    },
    {
      "id": "SENT",
      "name": "SENT",
      "type": "SENT",
      "unreadCount": 0,
      "totalCount": 45,
      "createdAt": "2024-11-20T10:00:00",
      "updatedAt": "2024-11-26T10:30:00"
    },
    {
      "id": "STARRED",
      "name": "STARRED",
      "type": "STARRED",
      "unreadCount": 2,
      "totalCount": 10,
      "createdAt": "2024-11-20T10:00:00",
      "updatedAt": "2024-11-26T10:30:00"
    }
  ]
}
```

**Mailbox Types:**
- `INBOX` - Inbox folder
- `SENT` - Sent emails
- `DRAFTS` - Draft emails
- `TRASH` - Deleted emails
- `SPAM` - Spam emails
- `STARRED` - Starred emails
- `IMPORTANT` - Important emails
- `CUSTOM` - Custom labels/folders

**Note:** This endpoint requires Gmail to be connected. Returns Gmail labels as mailboxes.

---

## Emails

### 1. Get Emails in Mailbox

**Endpoint:** `GET /api/mailboxes/{mailboxId}/emails`

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Path Parameters:**
- `mailboxId` - The mailbox/label ID (e.g., "INBOX", "SENT", "STARRED")

**Query Parameters:**
- `page` (optional, default: 0) - Page number (zero-based)
- `size` (optional, default: 20) - Page size (number of emails per page)

**Example:** `GET /api/mailboxes/INBOX/emails?page=0&size=20`

**Response:** `200 OK`
```json
{
  "success": true,
  "message": null,
  "data": {
    "content": [
      {
        "id": "18c2f3a1b2d4e5f6",
        "from": "sender@example.com",
        "fromName": "John Sender",
        "subject": "Meeting Tomorrow",
        "preview": "Hi, just wanted to confirm our meeting tomorrow at 10 AM...",
        "isRead": false,
        "isStarred": true,
        "isImportant": false,
        "hasAttachments": true,
        "receivedAt": "2024-11-25T10:30:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 120,
    "totalPages": 6,
    "last": false
  }
}
```

---

### 2. Get Email Details

**Endpoint:** `GET /api/emails/{emailId}`

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Path Parameters:**
- `emailId` - The email/message ID

**Response:** `200 OK`
```json
{
  "success": true,
  "message": null,
  "data": {
    "id": "18c2f3a1b2d4e5f6",
    "from": "sender@example.com",
    "fromName": "John Sender",
    "to": ["recipient@example.com"],
    "cc": ["cc@example.com"],
    "bcc": [],
    "subject": "Meeting Tomorrow",
    "body": "<p>Hi,</p><p>Just wanted to confirm our meeting tomorrow at 10 AM.</p>",
    "isRead": false,
    "isStarred": true,
    "isImportant": false,
    "attachments": [
      {
        "id": "att_123",
        "filename": "agenda.pdf",
        "mimeType": "application/pdf",
        "size": 245678,
        "url": "/api/attachments/18c2f3a1b2d4e5f6/att_123"
      }
    ],
    "receivedAt": "2024-11-25T10:30:00",
    "sentAt": "2024-11-25T10:30:00"
  }
}
```

---

### 3. Send Email

**Endpoint:** `POST /api/emails/send`

**Headers:**
```
Authorization: Bearer <accessToken>
Content-Type: application/json
```

**Request Body:**
```json
{
  "to": ["recipient@example.com"],
  "cc": ["cc@example.com"],
  "bcc": ["bcc@example.com"],
  "subject": "Hello from AWAD Email",
  "body": "<p>This is the email body in HTML format.</p>",
  "attachmentIds": []
}
```

**Validation:**
- `to`: Required, at least one recipient
- `cc`: Optional, list of CC recipients
- `bcc`: Optional, list of BCC recipients
- `subject`: Required, not blank
- `body`: Required, not blank (supports HTML)
- `attachmentIds`: Optional, list of attachment IDs (for future use)

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Email sent successfully",
  "data": null
}
```

---

### 4. Reply to Email

**Endpoint:** `POST /api/emails/{emailId}/reply`

**Headers:**
```
Authorization: Bearer <accessToken>
Content-Type: application/json
```

**Path Parameters:**
- `emailId` - The email/message ID to reply to

**Request Body:**
```json
{
  "body": "<p>Thank you for your email. I confirm the meeting.</p>",
  "replyAll": false,
  "attachmentIds": []
}
```

**Validation:**
- `body`: Required, not blank (supports HTML)
- `replyAll`: Optional, boolean (default: false) - Reply to all recipients or just sender
- `attachmentIds`: Optional, list of attachment IDs (for future use)

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Reply sent successfully",
  "data": null
}
```

---

### 5. Perform Email Actions (Bulk Operations)

**Endpoint:** `POST /api/emails/actions`

**Headers:**
```
Authorization: Bearer <accessToken>
Content-Type: application/json
```

**Request Body:**
```json
{
  "emailIds": ["18c2f3a1b2d4e5f6", "28d3g4b2c3e5f7g8"],
  "action": "read"
}
```

**Available Actions:**
- `read` - Mark emails as read
- `unread` - Mark emails as unread
- `star` - Star emails
- `unstar` - Unstar emails
- `delete` - Move to trash
- `archive` - Archive emails

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Action performed successfully",
  "data": null
}
```

---

### 6. Mark Email as Read

**Endpoint:** `PATCH /api/emails/{emailId}/read`

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Path Parameters:**
- `emailId` - The email/message ID

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Email marked as read",
  "data": null
}
```

---

### 7. Mark Email as Unread

**Endpoint:** `PATCH /api/emails/{emailId}/unread`

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Path Parameters:**
- `emailId` - The email/message ID

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Email marked as unread",
  "data": null
}
```

---

### 8. Toggle Star on Email

**Endpoint:** `PATCH /api/emails/{emailId}/star`

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Path Parameters:**
- `emailId` - The email/message ID

**Query Parameters:**
- `starred` - Boolean value (true to star, false to unstar)

**Example:** `PATCH /api/emails/18c2f3a1b2d4e5f6/star?starred=true`

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Email starred",
  "data": null
}
```

---

### 9. Delete Email

**Endpoint:** `DELETE /api/emails/{emailId}`

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Path Parameters:**
- `emailId` - The email/message ID

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Email deleted",
  "data": null
}
```

---

### 10. Modify Email (Alternative Endpoint)

**Endpoint:** `POST /api/emails/{emailId}/modify`

**Headers:**
```
Authorization: Bearer <accessToken>
Content-Type: application/json
```

**Path Parameters:**
- `emailId` - The email/message ID (not used in current implementation)

**Request Body:**
```json
{
  "emailIds": ["18c2f3a1b2d4e5f6", "28d3g4b2c3e5f7g8"],
  "action": "read"
}
```

**Validation:**
- `emailIds`: Required, at least one email ID
- `action`: Required, not blank (one of: read, unread, star, unstar, delete, archive)

**Available Actions:**
- `read` - Mark emails as read
- `unread` - Mark emails as unread
- `star` - Star emails
- `unstar` - Unstar emails
- `delete` - Move to trash
- `archive` - Archive emails

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Action performed successfully",
  "data": null
}
```

---

## Attachments

### 1. Download Attachment

**Endpoint:** `GET /api/attachments/{messageId}/{attachmentId}`

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Path Parameters:**
- `messageId` - The email/message ID
- `attachmentId` - The attachment ID

**Query Parameters:**
- `filename` (optional) - Suggested filename for download

**Example:** `GET /api/attachments/18c2f3a1b2d4e5f6/att_123?filename=agenda.pdf`

**Response:** `200 OK`
- Content-Type: Based on attachment MIME type (or application/octet-stream)
- Content-Disposition: attachment; filename="agenda.pdf"
- Body: Binary file data

**Note:** This endpoint is currently a placeholder. Full implementation requires fetching attachment data from Gmail API.

---

## Health Check

### 1. Health Check

**Endpoint:** `GET /api/health`

**Response:** `200 OK`
```json
{
  "status": "UP",
  "timestamp": "2024-11-26T10:30:00",
  "service": "awad-email-backend",
  "version": "1.0.0"
}
```

**Note:** This endpoint does not require authentication and can be used to check if the service is running.

---

## Error Responses

All endpoints may return the following error responses:

### 400 Bad Request
```json
{
  "success": false,
  "message": "Invalid request parameters",
  "data": null
}
```

**Common Causes:**
- Missing required fields
- Invalid email format
- Invalid action type
- Validation errors

### 401 Unauthorized
```json
{
  "success": false,
  "message": "Invalid or expired token",
  "data": null
}
```

**Common Causes:**
- Missing Authorization header
- Invalid JWT token
- Expired access token
- User not found

### 403 Forbidden
```json
{
  "success": false,
  "message": "Access denied",
  "data": null
}
```

**Common Causes:**
- Insufficient permissions
- Email not verified
- Account suspended

### 404 Not Found
```json
{
  "success": false,
  "message": "Resource not found",
  "data": null
}
```

**Common Causes:**
- Email not found
- Mailbox not found
- User not found

### 500 Internal Server Error
```json
{
  "success": false,
  "message": "An unexpected error occurred",
  "data": null
}
```

**Common Causes:**
- Database connection error
- Gmail API error
- Server configuration error

---

## Authentication Flow

### For Email/Password Users:
1. **Signup:** `POST /api/auth/signup`
   - User provides email, password, and name
   - System sends OTP to email
2. **Verify Email:** `POST /api/auth/verify-email`
   - User enters OTP received via email
   - System returns JWT tokens (access token and refresh token)
3. **Login:** `POST /api/auth/login`
   - User provides email and password
   - System returns JWT tokens
4. **Use Access Token:** Include in Authorization header for all requests
   ```
   Authorization: Bearer <accessToken>
   ```
5. **Refresh Token:** `POST /api/auth/refresh` when access token expires
6. **Connect Gmail:** User must login with Google OAuth to access email features

### For Google OAuth Users:
1. **Frontend initiates OAuth:**
   ```javascript
   const googleAuthUrl = `https://accounts.google.com/o/oauth2/v2/auth?` +
     `client_id=${GOOGLE_CLIENT_ID}&` +
     `redirect_uri=${encodeURIComponent('http://localhost:3000/auth/callback')}&` +
     `response_type=code&` +
     `scope=${encodeURIComponent('https://www.googleapis.com/auth/gmail.readonly https://www.googleapis.com/auth/gmail.modify https://www.googleapis.com/auth/gmail.send https://www.googleapis.com/auth/gmail.labels https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile')}&` +
     `access_type=offline&` +
     `prompt=consent`;

   window.location.href = googleAuthUrl;
   ```
2. **User grants permissions** on Google consent screen
3. **Google redirects back** with authorization code
4. **Frontend sends code to backend:** `POST /api/auth/google`
   ```json
   {
     "code": "4/0AY0e-g7..."
   }
   ```
5. **Backend exchanges code for tokens:**
   - Exchanges authorization code for Google OAuth tokens
   - Stores Gmail tokens server-side (refresh token, access token)
   - Creates/updates user account
   - Returns JWT tokens to frontend
6. **Use Access Token:** Include in Authorization header for all requests
7. **Gmail API integration is automatically enabled**
8. **Refresh Token:** `POST /api/auth/refresh` when access token expires

### Password Reset Flow (Unauthenticated):
1. **Request Reset:** `POST /api/auth/forgot-password`
   - User provides email
   - System sends OTP to email
2. **Reset Password:** `POST /api/auth/reset-password`
   - User provides email, OTP (code), and new password
   - System updates password

### Change Password Flow (Authenticated):
1. **Request OTP:** `POST /api/auth/send-change-password-otp`
   - User must be authenticated
   - System sends OTP to user's registered email
2. **Change Password:** `POST /api/auth/change-password`
   - User provides current password, new password, and OTP (code)
   - System validates current password and OTP
   - System updates password

---

## Notes

### General
- All timestamps are in ISO 8601 format (e.g., `2024-11-26T10:30:00`)
- Email body supports HTML content
- All successful responses follow the `ApiResponse<T>` structure with `success`, `message`, and `data` fields
- All paginated responses use the `PageResponse<T>` structure

### Authentication
- JWT tokens are used for authentication
- Access tokens expire after 1 hour (configurable)
- Refresh tokens expire after 24 hours (configurable)
- Refresh tokens are stored server-side and can be revoked
- Include access token in Authorization header: `Authorization: Bearer <accessToken>`

### Gmail Integration
- Gmail integration is automatic when user logs in with Google OAuth
- Google OAuth tokens (refresh token, access token) are stored server-side
- Gmail API access tokens are automatically refreshed when expired
- Required Gmail API scopes:
  - `https://www.googleapis.com/auth/gmail.readonly`
  - `https://www.googleapis.com/auth/gmail.modify`
  - `https://www.googleapis.com/auth/gmail.send`
  - `https://www.googleapis.com/auth/gmail.labels`
  - `https://www.googleapis.com/auth/userinfo.email`
  - `https://www.googleapis.com/auth/userinfo.profile`

### Email Operations
- Email IDs are Gmail message IDs
- Mailbox IDs are Gmail label IDs (e.g., "INBOX", "SENT", "STARRED")
- Bulk operations are supported via the `/api/emails/actions` endpoint
- Individual operations are available via dedicated endpoints (e.g., `/api/emails/{emailId}/read`)

### Validation
- All request bodies are validated using Jakarta Bean Validation
- Validation errors return 400 Bad Request with error details
- Email addresses must be in valid format
- Passwords must be at least 6 characters

### Security
- All endpoints except `/api/health` and authentication endpoints require authentication
- CORS is configured to allow requests from the frontend
- Passwords are hashed using BCrypt
- OTP codes are time-limited and single-use

### Rate Limiting
- Gmail API has rate limits (refer to Google's documentation)
- Backend implements token refresh to handle expired tokens
- Consider implementing rate limiting on authentication endpoints

---

## API Response Structure

All API responses follow a consistent structure:

### Success Response
```json
{
  "success": true,
  "message": "Operation successful",
  "data": {
    // Response data here
  }
}
```

### Error Response
```json
{
  "success": false,
  "message": "Error message",
  "data": null
}
```

### Paginated Response
```json
{
  "success": true,
  "message": null,
  "data": {
    "content": [
      // Array of items
    ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "last": false
  }
}
```

---

## Common HTTP Status Codes

- `200 OK` - Request successful
- `400 Bad Request` - Invalid request parameters or validation error
- `401 Unauthorized` - Missing or invalid authentication token
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

---

## Development and Testing

### Base URLs
- **Local Development:** `http://localhost:8080`
- **Production:** (Configure based on deployment)

### Testing Tools
- Use Postman collection provided in `postman_collection.json`
- Health check endpoint: `GET /api/health`

### Environment Variables
Refer to `application.yml` for required environment variables:
- Database connection (MongoDB)
- JWT secret and expiration times
- Google OAuth credentials
- Email service configuration (for OTP)

---

## Additional Resources

- **Setup Guide:** See `SETUP_GUIDE.md`
- **Gmail API Setup:** See `GMAIL_API_SETUP.md`
- **Authentication Flows:** See `AUTHENTICATION_FLOWS.md`
- **Test Scenarios:** See `TEST_SCENARIOS.md`
- **API Logging:** See `API_LOGGING_GUIDE.md`

