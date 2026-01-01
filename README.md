# 📧 Advanced Web Email Application

A full-stack email management application with AI-powered features, Gmail integration, and Kanban-style email organization.

## 📋 Table of Contents

- [Architecture Overview](#architecture-overview)
- [Technology Stack](#technology-stack)
- [Setup Guide](#setup-guide)
  - [Prerequisites](#prerequisites)
  - [Core Backend Setup](#core-backend-setup)
  - [AI Service Setup](#ai-service-setup)
  - [Environment Variables](#environment-variables)
- [API Endpoints](#api-endpoints)
- [Google OAuth Setup](#google-oauth-setup)
- [Token Storage & Security](#token-storage--security)
- [Security Considerations](#security-considerations)

---

## 🏗️ Architecture Overview

The application consists of two main services:

```
┌─────────────────────────────────────────────────────────────────┐
│                        Frontend (React)                          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Core Backend (Spring Boot)                    │
│  - Authentication (JWT + Google OAuth)                          │
│  - Gmail API Integration                                        │
│  - Kanban Board Management                                      │
│  - Email Operations                                             │
│  - Semantic Search Orchestration                                │
└─────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
        ┌──────────┐   ┌──────────┐   ┌──────────────┐
        │ MongoDB  │   │ Gmail API│   │ AI Service   │
        │          │   │ (Google) │   │ (FastAPI)    │
        └──────────┘   └──────────┘   └──────────────┘
                                             │
                                             ▼
                                      ┌──────────────┐
                                      │  OpenAI API  │
                                      └──────────────┘
```

---

## 🛠️ Technology Stack

### Core Backend (`/core`)
- **Framework**: Spring Boot 3.5.7
- **Language**: Java 21
- **Database**: MongoDB
- **Authentication**: JWT + Google OAuth 2.0
- **Gmail Integration**: Google Gmail API
- **Email Service**: Brevo (Sendinblue) for OTP emails

### AI Service (`/ai`)
- **Framework**: FastAPI
- **Language**: Python 3.12
- **AI Provider**: OpenAI (GPT-4o-mini, text-embedding-3-small)
- **Package Manager**: UV

---

## 🚀 Setup Guide

### Prerequisites

- **Java 21** or higher
- **Python 3.12** or higher
- **MongoDB** (local or cloud instance)
- **Node.js** (for frontend, if applicable)
- **Google Cloud Console** account (for OAuth)
- **OpenAI API Key**

### Core Backend Setup

1. **Navigate to the core backend directory:**
   ```bash
   cd core/web-advance-email-backend-main
   ```

2. **Create a `.env` file** with required environment variables (see [Environment Variables](#environment-variables))

3. **Build and run the application:**
   ```bash
   # Using Maven wrapper
   ./mvnw spring-boot:run
   
   # Or build JAR and run
   ./mvnw clean package
   java -jar target/awad-email-0.0.1-SNAPSHOT.jar
   ```

4. **Using Docker:**
   ```bash
   docker build -t awad-email-backend .
   docker run -p 8080:8080 --env-file .env awad-email-backend
   ```

### AI Service Setup

1. **Navigate to the AI service directory:**
   ```bash
   cd ai/email-final-project-AI-main
   ```

2. **Create a `.env` file:**
   ```env
   OPENAI_API_KEY=your_openai_api_key
   # or
   OPEN_AI_KEY=your_openai_api_key
   ```

3. **Install dependencies and run:**
   ```bash
   # Using UV (recommended)
   uv sync
   uv run fastapi run main.py --port 8000
   
   # Or using pip
   pip install -r requirements.txt
   python main.py
   ```

4. **Using Docker:**
   ```bash
   docker build -t awad-email-ai .
   docker run -p 8000:8000 -e OPENAI_API_KEY=your_key awad-email-ai
   ```

### Environment Variables

#### Core Backend (`.env`)

```env
# MongoDB
MONGODB_URI=mongodb://localhost:27017/awad_email

# Server
SERVER_PORT=8080

# JWT Configuration
JWT_SECRET=your_super_secret_jwt_key_at_least_256_bits_long
JWT_ACCESS_TOKEN_EXPIRATION=3600000      # 1 hour in ms
JWT_REFRESH_TOKEN_EXPIRATION=86400000    # 24 hours in ms

# Cookie Configuration
COOKIE_SECURE=true                        # Set to false for local dev without HTTPS
COOKIE_SAME_SITE=None                     # None for cross-origin, Lax for same-site

# Google OAuth
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
GOOGLE_REDIRECT_URI=http://localhost:3000/auth/callback

# AI Service
AI_SERVICE_URL=http://localhost:8000
AI_SERVICE_TIMEOUT=30

# OpenAI (for embeddings)
OPENAI_API_KEY=your_openai_api_key

# Brevo Email Service (for OTP)
BREVO_API_KEY=your_brevo_api_key
SENDER_EMAIL=noreply@yourdomain.com
SENDER_NAME=YourAppName
OTP_DURATION=300                          # OTP validity in seconds
```

#### AI Service (`.env`)

```env
OPENAI_API_KEY=your_openai_api_key
DEBUG=false
HOST=0.0.0.0
PORT=8000
```

---

## 📡 API Endpoints

### Authentication (`/api/auth`)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/signup` | Register new user with email/password | No |
| POST | `/verify-email` | Verify email with OTP code | No |
| POST | `/resend-verification-otp` | Resend verification OTP | No |
| POST | `/login` | Login with email/password | No |
| POST | `/google` | Google OAuth login (authorization code flow) | No |
| POST | `/refresh` | Refresh access token (uses HttpOnly cookie) | No |
| POST | `/logout` | Logout and revoke refresh token | Yes |
| GET | `/me` | Get current user info | Yes |
| POST | `/introspect` | Validate a token | No |
| POST | `/forgot-password` | Request password reset OTP | No |
| POST | `/reset-password` | Reset password with OTP | No |
| POST | `/send-change-password-otp` | Request OTP for password change | Yes |
| POST | `/change-password` | Change password with OTP verification | Yes |

### Email Operations (`/api`)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/mailboxes` | Get all mailboxes (Gmail labels) | Yes |
| GET | `/mailboxes/{mailboxId}/emails` | Get emails in a mailbox | Yes |
| GET | `/emails/{emailId}` | Get email details | Yes |
| POST | `/emails/actions` | Perform bulk actions (read/unread/star/delete/archive) | Yes |
| PATCH | `/emails/{emailId}/read` | Mark email as read | Yes |
| PATCH | `/emails/{emailId}/unread` | Mark email as unread | Yes |
| PATCH | `/emails/{emailId}/star` | Toggle star status | Yes |
| DELETE | `/emails/{emailId}` | Delete email (move to trash) | Yes |
| POST | `/emails/send` | Send a new email | Yes |
| POST | `/emails/{emailId}/reply` | Reply to an email | Yes |

### Kanban Board (`/api/kanban`)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/board` | Get full Kanban board with all columns | Yes |
| GET | `/board/filter` | Get board with sorting/filtering options | Yes |
| POST | `/sync` | Sync Gmail emails to Kanban board | Yes |
| GET | `/gmail-status` | Check if Gmail is connected | Yes |
| GET | `/gmail-labels` | Get available Gmail labels | Yes |
| GET | `/columns` | Get all Kanban columns | Yes |
| POST | `/columns` | Create a new custom column | Yes |
| PUT | `/columns/{columnId}` | Update a column | Yes |
| DELETE | `/columns/{columnId}` | Delete a custom column | Yes |
| GET | `/columns/{columnId}/emails` | Get emails in a column | Yes |
| POST | `/emails` | Add email to Kanban board | Yes |
| GET | `/emails/{emailId}` | Get email's Kanban status | Yes |
| POST | `/emails/move` | Move email to different column | Yes |
| DELETE | `/emails/{emailId}` | Remove email from Kanban | Yes |
| POST | `/emails/snooze` | Snooze an email | Yes |
| POST | `/emails/{emailId}/unsnooze` | Unsnooze an email | Yes |
| POST | `/emails/{emailId}/summarize` | Generate AI summary | Yes |
| GET | `/search` | Fuzzy search on Kanban emails | Yes |
| POST | `/search` | Fuzzy search (POST variant) | Yes |

### Search (`/api/search`)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/semantic/status` | Check if semantic search is available | Yes |
| POST | `/semantic` | Perform semantic search using AI | Yes |
| POST | `/semantic/generate-embeddings` | Generate embeddings for all emails | Yes |
| POST | `/semantic/generate-embedding/{emailId}` | Generate embedding for single email | Yes |
| GET | `/suggestions` | Get search suggestions (type-ahead) | Yes |
| GET | `/contacts` | Get all unique contacts | Yes |

### AI Service Endpoints (`/api/v1`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Health check |
| GET | `/health` | Health status |
| POST | `/chat/completion` | Chat completion with OpenAI |
| POST | `/chat/generate` | Simple text generation |
| POST | `/email/summarize` | Summarize a single email |
| POST | `/email/summarize/batch` | Batch email summarization |
| POST | `/email/search/semantic` | Semantic search (LLM-based) |
| GET | `/email/embedding/status` | Check embedding service status |
| POST | `/email/embedding/generate` | Generate embedding for single email |
| POST | `/email/embedding/generate/batch` | Batch embedding generation |
| POST | `/email/search/embedding` | Embedding-based semantic search |

---

## 🔐 Google OAuth Setup

### Step 1: Create Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the following APIs:
   - **Gmail API**
   - **Google+ API** (for user profile)

### Step 2: Configure OAuth Consent Screen

1. Navigate to **APIs & Services** → **OAuth consent screen**
2. Select **External** user type (or Internal for organization)
3. Fill in the required information:
   - App name
   - User support email
   - Developer contact email
4. Add the following scopes:
   ```
   https://www.googleapis.com/auth/gmail.readonly
   https://www.googleapis.com/auth/gmail.modify
   https://www.googleapis.com/auth/gmail.send
   https://www.googleapis.com/auth/gmail.labels
   https://www.googleapis.com/auth/userinfo.email
   https://www.googleapis.com/auth/userinfo.profile
   ```

### Step 3: Create OAuth Credentials

1. Navigate to **APIs & Services** → **Credentials**
2. Click **Create Credentials** → **OAuth client ID**
3. Select **Web application**
4. Configure:
   - **Name**: Your app name
   - **Authorized JavaScript origins**:
     - `http://localhost:3000` (development)
     - `https://yourdomain.com` (production)
   - **Authorized redirect URIs**:
     - `http://localhost:3000/auth/callback` (development)
     - `https://yourdomain.com/auth/callback` (production)
5. Copy the **Client ID** and **Client Secret**

### Step 4: Frontend Integration

The frontend should redirect users to Google's OAuth URL:

```javascript
const GOOGLE_AUTH_URL = 'https://accounts.google.com/o/oauth2/v2/auth';
const params = new URLSearchParams({
  client_id: GOOGLE_CLIENT_ID,
  redirect_uri: 'http://localhost:3000/auth/callback',
  response_type: 'code',
  scope: [
    'https://www.googleapis.com/auth/gmail.readonly',
    'https://www.googleapis.com/auth/gmail.modify',
    'https://www.googleapis.com/auth/gmail.send',
    'https://www.googleapis.com/auth/gmail.labels',
    'openid',
    'email',
    'profile'
  ].join(' '),
  access_type: 'offline',
  prompt: 'consent'
});

window.location.href = `${GOOGLE_AUTH_URL}?${params}`;
```

After Google redirects back with the authorization code, send it to the backend:

```javascript
// In your callback handler
const code = new URLSearchParams(window.location.search).get('code');

const response = await fetch('/api/auth/google', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ code }),
  credentials: 'include' // Important for cookies
});
```

---

## 🔑 Token Storage & Security

### Token Types

The application uses multiple types of tokens:

| Token Type | Purpose | Storage | Lifetime |
|------------|---------|---------|----------|
| **Access Token (JWT)** | API authentication | Client memory/localStorage | 1 hour |
| **Refresh Token (JWT)** | Obtain new access tokens | HttpOnly cookie | 24 hours |
| **Google Access Token** | Gmail API calls | Server-side (MongoDB) | 1 hour |
| **Google Refresh Token** | Refresh Google access tokens | Server-side (MongoDB) | Long-lived |

### JWT Access Token Structure

```json
{
  "sub": "user_id",
  "user": {
    "id": "user_id",
    "email": "user@example.com",
    "name": "User Name",
    "profilePicture": "https://..."
  },
  "type": "access",
  "iat": 1234567890,
  "exp": 1234571490
}
```

### Refresh Token Security

**Why HttpOnly Cookies?**
- Prevents XSS attacks from accessing the token via JavaScript
- Automatically sent with requests to `/api/auth/*` endpoints
- Cannot be stolen by malicious scripts

**Server-Side Storage:**
- Refresh tokens are **hashed using SHA-256** before storage
- Only the hash is stored in MongoDB, never the actual token
- Even if the database is compromised, tokens cannot be recovered

```java
// Token hashing implementation
public static String hashToken(String token) {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(hashBytes);
}
```

### Google Token Storage

Google OAuth tokens are stored in the `google_tokens` collection:

```javascript
{
  "_id": "...",
  "userId": "user_id",
  "accessToken": "ya29...",           // Short-lived, cached
  "refreshToken": "1//...",           // Long-lived, stored securely
  "accessTokenExpiresAt": "2024-...", // Expiration timestamp
  "scope": "https://www.googleapis.com/auth/gmail...",
  "createdAt": "...",
  "updatedAt": "..."
}
```

**Automatic Token Refresh:**
- Before each Gmail API call, the system checks if the access token is expired
- If expired (or within 5 minutes of expiry), it automatically refreshes using the refresh token
- The new access token is cached for subsequent requests

---

## 🛡️ Security Considerations

### 1. Authentication Security

#### Password Security
- Passwords are hashed using **BCrypt** with automatic salt generation
- Never stored in plain text
- Minimum password requirements should be enforced on frontend

#### JWT Security
- Tokens are signed using **HMAC-SHA256** with a secret key
- Secret key must be at least 256 bits (32 characters)
- Access tokens have short expiration (1 hour)
- Refresh tokens are stored as hashes only

#### Session Management
- Stateless authentication (no server-side sessions)
- Refresh token rotation can be enabled for additional security
- All refresh tokens are revoked on password change

### 2. Cookie Security

```java
// Cookie configuration
cookieValue.append("; HttpOnly");      // Prevents XSS access
cookieValue.append("; Secure");        // HTTPS only (production)
cookieValue.append("; SameSite=None"); // Cross-origin support
cookieValue.append("; Path=/api/auth"); // Limited scope
```

**Production Settings:**
- `COOKIE_SECURE=true` - Only send over HTTPS
- `COOKIE_SAME_SITE=None` - Required for cross-origin requests with credentials

**Development Settings:**
- `COOKIE_SECURE=false` - Allow HTTP for localhost
- `COOKIE_SAME_SITE=Lax` - More restrictive for same-site

### 3. API Security

#### CORS Configuration
```java
configuration.setAllowedOriginPatterns(List.of("*")); // Configure for production!
configuration.setAllowCredentials(true);
configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
```

**Production Recommendation:**
- Replace `*` with specific allowed origins
- Example: `List.of("https://yourdomain.com", "https://app.yourdomain.com")`

#### Rate Limiting
- Consider implementing rate limiting for:
  - Login attempts (prevent brute force)
  - OTP requests (prevent abuse)
  - API calls (prevent DoS)

### 4. Data Security

#### MongoDB Security
- Use authentication for MongoDB connections
- Enable TLS/SSL for database connections
- Use connection string with credentials:
  ```
  mongodb+srv://user:password@cluster.mongodb.net/awad_email?retryWrites=true
  ```

#### Sensitive Data Handling
- Google refresh tokens are stored server-side only
- User passwords are never logged or exposed
- API keys are loaded from environment variables

### 5. Input Validation

- All DTOs use Jakarta Validation annotations
- Email format validation
- Password strength requirements
- Request body size limits

### 6. Error Handling

- Generic error messages for authentication failures (prevents user enumeration)
- Detailed errors only in debug mode
- Structured error responses:
  ```json
  {
    "success": false,
    "message": "Error description",
    "data": null
  }
  ```

### 7. Logging Security

- Sensitive data (passwords, tokens) are never logged
- Request logging includes:
  - HTTP method and path
  - Response status
  - Processing time
- User actions are logged for audit trails

### 8. AI Service Security

- OpenAI API key stored as environment variable
- CORS configured (should be restricted in production)
- Input sanitization for email content
- Rate limiting on AI endpoints recommended

### 9. Recommendations for Production

1. **Use HTTPS everywhere** - Enable SSL/TLS certificates
2. **Secure environment variables** - Use secrets management (AWS Secrets Manager, HashiCorp Vault)
3. **Enable rate limiting** - Protect against abuse
4. **Set up monitoring** - Track authentication failures, API errors
5. **Regular security audits** - Review dependencies for vulnerabilities
6. **Backup strategy** - Regular MongoDB backups
7. **Update dependencies** - Keep all packages up to date
8. **Restrict CORS origins** - Only allow known frontend domains
9. **Use strong JWT secrets** - Generate cryptographically secure keys
10. **Implement IP blocking** - Block suspicious IPs after multiple failed attempts

---

## 📁 Project Structure

```
├── ai/
│   └── email-final-project-AI-main/
│       ├── main.py              # FastAPI application entry
│       ├── config.py            # Configuration management
│       ├── routers/
│       │   ├── chat.py          # Chat completion endpoints
│       │   └── email.py         # Email AI endpoints
│       ├── services/
│       │   ├── openai_service.py    # OpenAI API integration
│       │   ├── email_service.py     # Email processing logic
│       │   └── embedding_service.py # Vector embeddings
│       ├── schemas/             # Pydantic models
│       ├── utils/               # Utilities and helpers
│       └── Dockerfile
│
├── core/
│   └── web-advance-email-backend-main/
│       ├── src/main/java/com/hcmus/awad_email/
│       │   ├── controller/      # REST controllers
│       │   ├── service/         # Business logic
│       │   ├── repository/      # MongoDB repositories
│       │   ├── model/           # Entity models
│       │   ├── dto/             # Data transfer objects
│       │   ├── security/        # JWT authentication
│       │   ├── config/          # Spring configuration
│       │   ├── exception/       # Custom exceptions
│       │   └── util/            # Utilities
│       ├── src/main/resources/
│       │   └── application.yml  # Application configuration
│       ├── pom.xml              # Maven dependencies
│       └── Dockerfile
│
└── README.md
```

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is developed for educational purposes as part of the Advanced Web Development course at HCMUS.

