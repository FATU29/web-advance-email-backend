# Getting Started with AWAD Email Backend

## 🎉 Implementation Complete!

All backend features from REQUIREMENTS.md have been successfully implemented. The system is ready for testing and frontend integration.

## 📋 Quick Start

### 1. Prerequisites Check

Make sure you have:
- ✅ Java 21 or higher
- ✅ MongoDB running on localhost:27017
- ✅ Maven (included via wrapper)

### 2. Configure Environment Variables

```bash
# Copy the example .env file
cp .env.example .env

# Edit the .env file
nano .env
```

**Important settings to update in `.env`:**
- `JWT_SECRET` - Change to a strong random key (at least 32 characters)
  ```bash
  # Generate a secure secret:
  openssl rand -base64 32
  ```
- `GOOGLE_CLIENT_ID` - Add your Google OAuth client ID (optional for testing)
- `MONGODB_URI` - Update if MongoDB is not on localhost

**Note:** The `.env` file is gitignored and will not be committed to version control.

### 3. Start MongoDB

```bash
# macOS with Homebrew
brew services start mongodb-community

# Or using Docker
docker run -d -p 27017:27017 --name mongodb mongo:latest

# Verify MongoDB is running
mongosh --eval "db.version()"
```

### 4. Run the Application

**Option A: Using the quick start script**
```bash
./start.sh
```

**Option B: Using Maven directly**
```bash
./mvnw spring-boot:run
```

**Option C: Build and run JAR**
```bash
./mvnw clean package
java -jar target/awad-email-0.0.1-SNAPSHOT.jar
```

### 5. Verify the Server is Running

```bash
# Health check
curl http://localhost:8080/api/health

# Expected response:
# {
#   "status": "UP",
#   "timestamp": "2024-01-01T12:00:00",
#   "service": "awad-email-backend",
#   "version": "1.0.0"
# }
```

## 🧪 Testing the API

### Option 1: Using Postman (Recommended)

1. Import `postman_collection.json` into Postman
2. The collection includes all API endpoints with automatic token management
3. Run requests in order:
   - **Sign Up** → Creates user and saves tokens
   - **Get All Mailboxes** → Lists mailboxes and saves mailboxId
   - **Get Emails in Mailbox** → Lists emails and saves emailId
   - Test other endpoints with saved variables

### Option 2: Using cURL

#### Sign Up
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "email": "test@example.com",
    "password": "password123"
  }'
```

Save the `accessToken` from the response.

#### Get Mailboxes
```bash
curl -X GET http://localhost:8080/api/mailboxes \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

Save a `mailboxId` from the response.

#### Get Emails
```bash
curl -X GET "http://localhost:8080/api/mailboxes/MAILBOX_ID/emails?page=0&size=20" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## 📚 Documentation

- **[README_BACKEND.md](README_BACKEND.md)** - Complete setup and usage guide
- **[BACKEND_API.md](BACKEND_API.md)** - Detailed API documentation with examples
- **[IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md)** - Implementation overview and architecture
- **[postman_collection.json](postman_collection.json)** - Postman collection for API testing

## 🔑 Key Features Implemented

### Authentication
- ✅ Email/Password registration and login
- ✅ Google OAuth 2.0 Sign-In
- ✅ JWT-based authentication (Access Token + Refresh Token)
- ✅ Token refresh mechanism
- ✅ Logout with token revocation

### Email Management
- ✅ 6 default mailboxes (Inbox, Sent, Drafts, Trash, Archive, Starred)
- ✅ Paginated email listing
- ✅ Email detail view
- ✅ Mark as read/unread
- ✅ Star/unstar emails
- ✅ Delete emails
- ✅ Archive emails
- ✅ Bulk actions support
- ✅ Mock email data for testing

### Security
- ✅ Spring Security with JWT
- ✅ Password hashing with BCrypt
- ✅ CORS configuration
- ✅ Protected API endpoints
- ✅ Global exception handling

## 🎯 What's Included

### Mock Data
When a user signs up or logs in with Google for the first time:
- 6 default mailboxes are created
- 5 sample emails are generated in the Inbox
- Emails include various states (read/unread, starred, with attachments)

### API Endpoints

**Public Endpoints:**
- `POST /api/auth/signup` - Register new user
- `POST /api/auth/login` - Email/password login
- `POST /api/auth/google` - Google OAuth login
- `POST /api/auth/refresh` - Refresh access token
- `GET /api/health` - Health check

**Protected Endpoints (require authentication):**
- `GET /api/mailboxes` - List all mailboxes
- `GET /api/mailboxes/{id}` - Get specific mailbox
- `GET /api/mailboxes/{id}/emails` - List emails (paginated)
- `GET /api/emails/{id}` - Get email details
- `POST /api/emails/actions` - Bulk actions
- `PATCH /api/emails/{id}/read` - Mark as read
- `PATCH /api/emails/{id}/unread` - Mark as unread
- `PATCH /api/emails/{id}/star` - Toggle star
- `DELETE /api/emails/{id}` - Delete email
- `POST /api/auth/logout` - Logout

## 🔄 Next Steps

### For Backend Development
1. ✅ All features implemented
2. ✅ Build successful
3. ✅ Ready for testing
4. 🔜 Write unit tests (optional)
5. 🔜 Add integration tests (optional)

### For Frontend Integration
1. Install frontend dependencies:
   ```bash
   npm install axios @react-oauth/google
   ```

2. Configure API base URL:
   ```javascript
   const API_BASE_URL = 'http://localhost:8080/api';
   ```

3. Implement token management:
   - Store access token in memory (React state)
   - Store refresh token in localStorage
   - Implement token refresh interceptor

4. Implement Google Sign-In:
   - Use `@react-oauth/google` package
   - Send credential to `/api/auth/google`

5. Build the email dashboard:
   - Fetch mailboxes on mount
   - Display email list when mailbox selected
   - Show email detail when email selected
   - Implement actions (read, star, delete, archive)

### For Gmail Integration (Future)
1. Add Gmail API dependencies
2. Update OAuth scopes to include Gmail
3. Replace mock email generation with Gmail API calls
4. Implement sync strategy (periodic or webhook-based)
5. Map email operations to Gmail API

## 🐛 Troubleshooting

### MongoDB Connection Issues
```bash
# Check if MongoDB is running
brew services list | grep mongodb

# Start MongoDB
brew services start mongodb-community

# Check logs
tail -f /usr/local/var/log/mongodb/mongo.log
```

### Port Already in Use
```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>

# Or change port in application.properties
server.port=8081
```

### Build Errors
```bash
# Clean and rebuild
./mvnw clean compile

# Skip tests if needed
./mvnw clean package -DskipTests
```

### JWT Token Issues
- Make sure JWT secret is at least 256 bits (32 characters)
- Check token expiration times in application.properties
- Verify Authorization header format: `Bearer <token>`

## 📞 Support

For issues and questions:
1. Check the documentation files
2. Review the Postman collection examples
3. Check application logs for error details
4. Verify MongoDB is running and accessible

## 🎊 Success!

Your backend is now ready! The system includes:
- ✅ Complete authentication system
- ✅ Email management with mock data
- ✅ RESTful API with proper error handling
- ✅ Security best practices
- ✅ Comprehensive documentation
- ✅ Ready for frontend integration
- ✅ Prepared for Gmail API integration

**Happy coding! 🚀**

