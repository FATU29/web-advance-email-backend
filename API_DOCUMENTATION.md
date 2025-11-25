# AWAD Email Backend - API Documentation

Base URL: `http://localhost:8080`

## Table of Contents
1. [Authentication](#authentication)
2. [Mailboxes](#mailboxes)
3. [Emails](#emails)
4. [Attachments](#attachments)

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
  "otp": "123456"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Email verified successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
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

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
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
  "authorizationCode": "4/0AY0e-g7..."
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Google authentication successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
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

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
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

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Logout successful",
  "data": null
}
```

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

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Token is valid",
  "data": {
    "valid": true,
    "userId": "user123",
    "email": "user@example.com",
    "expiresAt": "2024-11-26T10:30:00"
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

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Password reset code sent to your email",
  "data": null
}
```

---

### 11. Verify Password Reset OTP

**Endpoint:** `POST /api/auth/verify-reset-otp`

**Request Body:**
```json
{
  "email": "user@example.com",
  "otp": "123456"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "OTP verified successfully",
  "data": null
}
```

---

### 12. Reset Password

**Endpoint:** `POST /api/auth/reset-password`

**Request Body:**
```json
{
  "email": "user@example.com",
  "otp": "123456",
  "newPassword": "NewSecurePassword123!"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Password reset successful",
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
      "totalCount": 120
    },
    {
      "id": "SENT",
      "name": "SENT",
      "type": "SENT",
      "unreadCount": 0,
      "totalCount": 45
    },
    {
      "id": "STARRED",
      "name": "STARRED",
      "type": "STARRED",
      "unreadCount": 2,
      "totalCount": 10
    }
  ]
}
```

**Note:** This endpoint requires Gmail to be connected. Returns Gmail labels as mailboxes.

---

## Emails

### 1. Get Emails in Mailbox

**Endpoint:** `GET /api/mailboxes/{mailboxId}/emails`

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Query Parameters:**
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 20) - Page size

**Example:** `GET /api/mailboxes/INBOX/emails?page=0&size=20`

**Response:** `200 OK`
```json
{
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
```

---

### 2. Get Email Details

**Endpoint:** `GET /api/emails/{emailId}`

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Response:** `200 OK`
```json
{
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
  "bcc": [],
  "subject": "Hello from AWAD Email",
  "body": "<p>This is the email body in HTML format.</p>",
  "attachmentIds": []
}
```

**Response:** `200 OK`
```json
{
  "message": "Email sent successfully"
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

**Request Body:**
```json
{
  "body": "<p>Thank you for your email. I confirm the meeting.</p>",
  "replyAll": false,
  "attachmentIds": []
}
```

**Response:** `200 OK`
```json
{
  "message": "Reply sent successfully"
}
```

---

### 5. Modify Email (Mark Read/Unread, Star, Delete, Archive)

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
  "action": "MARK_READ"
}
```

**Available Actions:**
- `MARK_READ` - Mark emails as read
- `MARK_UNREAD` - Mark emails as unread
- `STAR` - Star emails
- `UNSTAR` - Unstar emails
- `DELETE` - Move to trash
- `ARCHIVE` - Archive emails

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

**Query Parameters:**
- `filename` (optional) - Suggested filename for download

**Example:** `GET /api/attachments/18c2f3a1b2d4e5f6/att_123?filename=agenda.pdf`

**Response:** `200 OK`
- Content-Type: Based on attachment MIME type
- Content-Disposition: attachment; filename="agenda.pdf"
- Body: Binary file data

---

## Error Responses

All endpoints may return the following error responses:

### 400 Bad Request
```json
{
  "error": "Bad Request",
  "message": "Invalid request parameters"
}
```

### 401 Unauthorized
```json
{
  "error": "Unauthorized",
  "message": "Invalid or expired token"
}
```

### 404 Not Found
```json
{
  "error": "Not Found",
  "message": "Resource not found"
}
```

### 500 Internal Server Error
```json
{
  "error": "Internal Server Error",
  "message": "An unexpected error occurred"
}
```

---

## Authentication Flow

### For Email/Password Users:
1. **Signup:** `POST /api/auth/signup`
   - User provides email, password, and name
   - System sends OTP to email
2. **Verify Email:** `POST /api/auth/verify-email`
   - User enters OTP received via email
   - System returns JWT tokens
3. **Login:** `POST /api/auth/login`
   - User provides email and password
   - System returns JWT tokens
4. **Use Access Token:** Include in Authorization header for all requests
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
5. **Backend exchanges code for tokens:**
   - Exchanges authorization code for Google OAuth tokens
   - Stores Gmail tokens server-side (refresh token, access token)
   - Creates/updates user account
   - Returns JWT tokens to frontend
6. **Use Access Token:** Include in Authorization header for all requests
7. **Gmail API integration is automatically enabled**
8. **Refresh Token:** `POST /api/auth/refresh` when access token expires

### Password Reset Flow:
1. **Request Reset:** `POST /api/auth/forgot-password`
   - User provides email
   - System sends OTP to email
2. **Verify OTP:** `POST /api/auth/verify-reset-otp`
   - User enters OTP received via email
3. **Reset Password:** `POST /api/auth/reset-password`
   - User provides email, OTP, and new password
   - System updates password

---

## Notes

- All timestamps are in ISO 8601 format
- Email body supports HTML content
- Gmail integration is automatic when user logs in with Google OAuth
- Refresh tokens are stored server-side and automatically refreshed
- Access tokens expire after 1 hour (configurable)
- Refresh tokens expire after 24 hours (configurable)

