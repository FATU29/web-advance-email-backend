# AWAD Email - Backend

A Spring Boot backend application for email management with authentication (Email/Password and Google OAuth) and mock email data. Designed for future Gmail API integration.

## Features

### Authentication
- ✅ Email/Password registration and login
- ✅ Google OAuth 2.0 Sign-In
- ✅ JWT-based authentication (Access Token + Refresh Token)
- ✅ Secure token refresh mechanism
- ✅ Logout with token revocation

### Email Management
- ✅ Multiple mailboxes (Inbox, Sent, Drafts, Trash, Archive, Starred)
- ✅ Paginated email listing
- ✅ Email detail view with full content
- ✅ Mark as read/unread
- ✅ Star/unstar emails
- ✅ Delete emails (move to trash, then permanent delete)
- ✅ Archive emails
- ✅ Bulk actions support
- ✅ Mock email data for testing
- ✅ Attachment support (mock URLs)

### Security
- ✅ Spring Security with JWT
- ✅ Password hashing with BCrypt
- ✅ CORS configuration for frontend integration
- ✅ Protected API endpoints
- ✅ Token validation and refresh

### Future-Ready
- ✅ Gmail API integration ready (fields for gmailMessageId, gmailThreadId)
- ✅ Modular service architecture
- ✅ Easy to replace mock data with real Gmail data

## Technology Stack

- **Java**: 21
- **Spring Boot**: 3.5.7
- **Database**: MongoDB
- **Security**: Spring Security + JWT (JJWT 0.12.6)
- **OAuth**: Google OAuth 2.0
- **Build Tool**: Maven

## Prerequisites

1. **Java 21** or higher
   ```bash
   java -version
   ```

2. **MongoDB** (running on localhost:27017)
   ```bash
   # Install MongoDB (macOS)
   brew install mongodb-community
   brew services start mongodb-community
   
   # Or use Docker
   docker run -d -p 27017:27017 --name mongodb mongo:latest
   ```

3. **Maven** (included via wrapper)

## Setup Instructions

### 1. Clone the Repository

```bash
git clone <repository-url>
cd awad-email
```

### 2. Configure Environment Variables

Copy the example `.env` file:
```bash
cp .env.example .env
```

Edit `.env` file and update the following:

```bash
# MongoDB Configuration
MONGODB_URI=mongodb://localhost:27017/awad_email

# JWT Secret (CHANGE THIS IN PRODUCTION!)
# Generate a secure secret: openssl rand -base64 32
JWT_SECRET=YourVeryLongSecretKeyForJWTTokenGenerationThatShouldBeAtLeast256BitsLongForHS256Algorithm

# Google OAuth Client ID
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com

# Server Port (optional)
SERVER_PORT=8080

# Log Level (optional)
LOG_LEVEL=INFO
```

**Important:** The `.env` file is gitignored and will not be committed to version control. This keeps your sensitive data secure.

### 3. Get Google OAuth Credentials (Optional)

If you want to test Google Sign-In:

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable **Google+ API**
4. Go to **Credentials** → **Create Credentials** → **OAuth 2.0 Client ID**
5. Configure OAuth consent screen
6. Add authorized JavaScript origins:
   - `http://localhost:3000`
   - `http://localhost:5173`
7. Copy the **Client ID** and update `app.google.client-id` in application.yml

### 4. Build and Run

```bash
# Build the project
./mvnw clean package

# Run the application
./mvnw spring-boot:run

# Or run the JAR directly
java -jar target/awad-email-0.0.1-SNAPSHOT.jar
```

The API will be available at: `http://localhost:8080`

### 5. Verify Installation

```bash
# Check if the server is running
curl http://localhost:8080/api/auth/login
```

You should see a response (even if it's an error, it means the server is running).

## API Documentation

See [BACKEND_API.md](BACKEND_API.md) for complete API documentation.

### Quick Start - Test the API

#### 1. Sign Up
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "email": "test@example.com",
    "password": "password123"
  }'
```

#### 2. Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

Save the `accessToken` from the response.

#### 3. Get Mailboxes
```bash
curl -X GET http://localhost:8080/api/mailboxes \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

#### 4. Get Emails
```bash
curl -X GET "http://localhost:8080/api/mailboxes/MAILBOX_ID/emails?page=0&size=20" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## Project Structure

```
src/main/java/com/hcmus/awad_email/
├── config/              # Configuration classes
│   └── SecurityConfig.java
├── controller/          # REST Controllers
│   ├── AuthController.java
│   ├── EmailController.java
│   └── MailboxController.java
├── dto/                 # Data Transfer Objects
│   ├── auth/           # Authentication DTOs
│   ├── email/          # Email DTOs
│   └── common/         # Common DTOs
├── exception/          # Custom exceptions and handlers
│   ├── BadRequestException.java
│   ├── UnauthorizedException.java
│   ├── ResourceNotFoundException.java
│   └── GlobalExceptionHandler.java
├── model/              # Domain models
│   ├── User.java
│   ├── RefreshToken.java
│   ├── Email.java
│   ├── Mailbox.java
│   └── Attachment.java
├── repository/         # MongoDB repositories
│   ├── UserRepository.java
│   ├── RefreshTokenRepository.java
│   ├── EmailRepository.java
│   └── MailboxRepository.java
├── security/           # Security components
│   ├── JwtTokenProvider.java
│   └── JwtAuthenticationFilter.java
└── service/            # Business logic
    ├── AuthService.java
    ├── EmailService.java
    └── MailboxService.java
```

## Token Management

### Access Token
- **Lifetime**: 15 minutes
- **Storage**: In-memory (frontend)
- **Usage**: Authorization header for all protected endpoints

### Refresh Token
- **Lifetime**: 7 days
- **Storage**: localStorage or HttpOnly cookie (frontend)
- **Usage**: Obtain new access tokens

### Token Refresh Flow
1. Frontend receives 401 Unauthorized
2. Frontend calls `/api/auth/refresh` with refresh token
3. Backend validates and returns new access token
4. Frontend retries original request
5. If refresh fails → redirect to login

## Mock Data

When a user signs up or logs in with Google for the first time, the system automatically:
1. Creates default mailboxes (Inbox, Sent, Drafts, Trash, Archive, Starred)
2. Generates 5 mock emails in the Inbox with realistic data
3. Includes various email states (read/unread, starred, with attachments)

## Future Gmail Integration

To integrate with Gmail API:

1. **Add Gmail Dependencies** (pom.xml):
```xml
<dependency>
    <groupId>com.google.apis</groupId>
    <artifactId>google-api-services-gmail</artifactId>
    <version>v1-rev20230925-2.0.0</version>
</dependency>
```

2. **Update OAuth Scopes**: Add Gmail scopes to Google OAuth
3. **Replace Mock Methods**: Update `EmailService.initializeMockEmails()` with Gmail API calls
4. **Implement Sync**: Add periodic sync or webhook-based updates
5. **Use Gmail IDs**: Utilize `gmailMessageId` and `gmailThreadId` fields

## Testing

```bash
# Run all tests
./mvnw test

# Run with coverage
./mvnw test jacoco:report
```

## Troubleshooting

### MongoDB Connection Issues
```bash
# Check if MongoDB is running
brew services list | grep mongodb

# Start MongoDB
brew services start mongodb-community

# Check MongoDB logs
tail -f /usr/local/var/log/mongodb/mongo.log
```

### Port Already in Use
```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>

# Or change port in application.yml
server.port=8081
```

### JWT Secret Too Short
Make sure your JWT secret is at least 256 bits (32 characters) for HS256 algorithm.

## Security Considerations

⚠️ **Important for Production:**

1. **Change JWT Secret**: Use a strong, random 256-bit key
2. **Use HTTPS**: Never send tokens over HTTP in production
3. **Secure MongoDB**: Use authentication and encryption
4. **Environment Variables**: Store secrets in environment variables, not in code
5. **CORS**: Restrict allowed origins to your frontend domain
6. **Rate Limiting**: Implement rate limiting for authentication endpoints
7. **HttpOnly Cookies**: Consider using HttpOnly cookies for refresh tokens

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

[Your License Here]

## Support

For issues and questions, please open an issue on GitHub.

