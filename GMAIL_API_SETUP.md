# Gmail API Integration Setup Guide

This guide will walk you through setting up Gmail API integration for the AWAD Email Backend application.

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Google Cloud Console Setup](#google-cloud-console-setup)
3. [Environment Configuration](#environment-configuration)
4. [Testing the Integration](#testing-the-integration)
5. [Security Considerations](#security-considerations)
6. [Troubleshooting](#troubleshooting)

---

## Prerequisites

- A Google account
- Access to [Google Cloud Console](https://console.cloud.google.com/)
- MongoDB instance running (local or cloud)
- Java 21 or higher
- Maven 3.6+

---

## Google Cloud Console Setup

### Step 1: Create a New Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Click on the project dropdown at the top
3. Click "New Project"
4. Enter project name: `AWAD Email Client` (or your preferred name)
5. Click "Create"

### Step 2: Enable Gmail API

1. In your project, go to **APIs & Services** > **Library**
2. Search for "Gmail API"
3. Click on "Gmail API"
4. Click "Enable"

### Step 3: Configure OAuth Consent Screen

1. Go to **APIs & Services** > **OAuth consent screen**
2. Select **External** user type (unless you have a Google Workspace)
3. Click "Create"

#### Fill in the required information:

**App Information:**
- App name: `AWAD Email Client`
- User support email: Your email address
- Developer contact email: Your email address

**App Domain (Optional for development):**
- Leave blank for local development

**Authorized domains:**
- For production, add your domain (e.g., `yourdomain.com`)
- For local development, you can skip this

4. Click "Save and Continue"

#### Add Scopes:

1. Click "Add or Remove Scopes"
2. Add the following scopes:
   - `https://www.googleapis.com/auth/gmail.readonly` - Read emails
   - `https://www.googleapis.com/auth/gmail.modify` - Modify emails (mark read, star, etc.)
   - `https://www.googleapis.com/auth/gmail.send` - Send emails
   - `https://www.googleapis.com/auth/gmail.labels` - Manage labels
   - `https://www.googleapis.com/auth/userinfo.email` - Get user email
   - `https://www.googleapis.com/auth/userinfo.profile` - Get user profile

3. Click "Update" and then "Save and Continue"

#### Add Test Users (for development):

1. Click "Add Users"
2. Add your Gmail address and any other test accounts
3. Click "Save and Continue"

### Step 4: Create OAuth 2.0 Credentials

1. Go to **APIs & Services** > **Credentials**
2. Click "Create Credentials" > "OAuth client ID"
3. Select "Web application"
4. Name: `AWAD Email Web Client`

#### Configure Authorized JavaScript origins:

For local development:
```
http://localhost:3000
http://localhost:8080
```

For production:
```
https://your-frontend-domain.com
https://your-backend-domain.com
```

#### Configure Authorized redirect URIs:

For local development:
```
http://localhost:3000/auth/callback
http://localhost:3000/auth/google/callback
```

For production:
```
https://your-frontend-domain.com/auth/callback
https://your-frontend-domain.com/auth/google/callback
```

5. Click "Create"
6. **IMPORTANT:** Copy the **Client ID** and **Client Secret** - you'll need these for environment variables

---

## Environment Configuration

### Step 1: Create `.env` File

Create a `.env` file in the root of your backend project:

```bash
# MongoDB Configuration
MONGODB_URI=mongodb://localhost:27017/awad_email

# Server Configuration
SERVER_PORT=8080

# JWT Configuration
JWT_SECRET=your-super-secret-jwt-key-change-this-in-production-min-256-bits
JWT_ACCESS_TOKEN_EXPIRATION=3600000
JWT_REFRESH_TOKEN_EXPIRATION=86400000

# Google OAuth Configuration
GOOGLE_CLIENT_ID=your-client-id-from-google-console.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-client-secret-from-google-console
GOOGLE_REDIRECT_URI=http://localhost:3000/auth/callback

# Brevo Email Service (for OTP emails)
BREVO_API_KEY=your-brevo-api-key
SENDER_EMAIL=noreply@yourdomain.com
SENDER_NAME=AWAD Email

# Logging
LOG_LEVEL=INFO
LOG_LEVEL_SECURITY=WARN
LOG_LEVEL_WEB=INFO
```

### Step 2: Update Values

Replace the following placeholders:

1. **GOOGLE_CLIENT_ID**: Paste the Client ID from Google Cloud Console
2. **GOOGLE_CLIENT_SECRET**: Paste the Client Secret from Google Cloud Console
3. **GOOGLE_REDIRECT_URI**: Update if your frontend runs on a different URL
4. **JWT_SECRET**: Generate a strong secret key (at least 256 bits)
5. **MONGODB_URI**: Update if using a cloud MongoDB instance

### Step 3: Generate JWT Secret

You can generate a secure JWT secret using:

```bash
# Using OpenSSL
openssl rand -base64 64

# Using Node.js
node -e "console.log(require('crypto').randomBytes(64).toString('base64'))"
```

---

## Testing the Integration

### Step 1: Start the Backend

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The backend should start on `http://localhost:8080`

### Step 2: Test OAuth Flow

1. Start your frontend application
2. Click "Sign in with Google"
3. You should be redirected to Google's OAuth consent screen
4. Grant the requested permissions
5. You should be redirected back to your application with authentication

### Step 3: Verify Gmail Integration

After successful authentication:

1. **Check Mailboxes**: `GET http://localhost:8080/api/mailboxes`
   - Should return Gmail labels (Inbox, Sent, etc.)

2. **Check Emails**: `GET http://localhost:8080/api/mailboxes/INBOX/emails`
   - Should return real emails from your Gmail inbox

3. **Send Email**: `POST http://localhost:8080/api/emails/send`
   ```json
   {
     "to": ["recipient@example.com"],
     "subject": "Test Email",
     "body": "<p>This is a test email from AWAD Email Client</p>"
   }
   ```

---

## Security Considerations

### Token Storage

✅ **What we do (SECURE):**
- **Refresh tokens** are stored server-side in MongoDB
- **Access tokens** are cached server-side and automatically refreshed
- Frontend only receives JWT tokens for app authentication
- Gmail tokens never exposed to frontend

❌ **What NOT to do:**
- Never store Google refresh tokens in localStorage
- Never expose Google tokens to the frontend
- Never commit credentials to version control

### Production Checklist

- [ ] Use HTTPS for all endpoints
- [ ] Set strong JWT secret (256+ bits)
- [ ] Use environment variables for all secrets
- [ ] Enable MongoDB authentication
- [ ] Set up proper CORS policies
- [ ] Implement rate limiting
- [ ] Add request logging and monitoring
- [ ] Regular security audits
- [ ] Keep dependencies updated

### OAuth Scopes

Only request the minimum scopes needed:
- `gmail.readonly` - If you only need to read emails
- `gmail.modify` - If you need to mark read/unread, star, etc.
- `gmail.send` - If you need to send emails
- `gmail.labels` - If you need to manage labels

---

## Troubleshooting

### Error: "redirect_uri_mismatch"

**Problem:** The redirect URI doesn't match what's configured in Google Cloud Console.

**Solution:**
1. Check the `GOOGLE_REDIRECT_URI` in your `.env` file
2. Verify it matches exactly in Google Cloud Console > Credentials
3. Make sure to include the protocol (`http://` or `https://`)
4. No trailing slashes

### Error: "invalid_grant"

**Problem:** The authorization code has expired or been used already.

**Solution:**
- Authorization codes are single-use and expire quickly
- Make sure your frontend sends the code immediately after receiving it
- Check that system clocks are synchronized

### Error: "Access blocked: This app's request is invalid"

**Problem:** OAuth consent screen not properly configured.

**Solution:**
1. Go to OAuth consent screen in Google Cloud Console
2. Make sure all required fields are filled
3. Add your email as a test user
4. Verify all scopes are added

### Error: "Gmail API has not been used in project"

**Problem:** Gmail API not enabled for your project.

**Solution:**
1. Go to APIs & Services > Library
2. Search for "Gmail API"
3. Click Enable

### Tokens Not Refreshing

**Problem:** Access token expires and doesn't refresh automatically.

**Solution:**
1. Verify refresh token is stored in database
2. Check `GoogleToken` collection in MongoDB
3. Ensure `access_type=offline` is set in OAuth request
4. Make sure `approval_prompt=force` is set to get refresh token

### No Emails Showing

**Problem:** Mailboxes load but no emails appear.

**Solution:**
1. Check if Gmail is connected: Look for `gmailConnected: true` in user document
2. Verify scopes include `gmail.readonly`
3. Check backend logs for API errors
4. Test with Postman to isolate frontend issues

---

## API Endpoints Reference

### Authentication
- `POST /api/auth/google` - Google OAuth login (exchange code for tokens)
- `POST /api/auth/logout` - Logout and revoke tokens

### Mailboxes
- `GET /api/mailboxes` - List all mailboxes/labels
- `GET /api/mailboxes/{id}` - Get specific mailbox

### Emails
- `GET /api/mailboxes/{mailboxId}/emails` - List emails in mailbox
- `GET /api/emails/{id}` - Get email details
- `POST /api/emails/send` - Send new email
- `POST /api/emails/{id}/reply` - Reply to email
- `POST /api/emails/{id}/modify` - Modify email (read, star, delete, etc.)

### Attachments
- `GET /api/attachments/{messageId}/{attachmentId}` - Download attachment

---

## Additional Resources

- [Gmail API Documentation](https://developers.google.com/gmail/api)
- [Google OAuth 2.0 Documentation](https://developers.google.com/identity/protocols/oauth2)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [MongoDB Documentation](https://docs.mongodb.com/)

---

## Support

If you encounter issues not covered in this guide:

1. Check the backend logs for detailed error messages
2. Verify all environment variables are set correctly
3. Ensure MongoDB is running and accessible
4. Test OAuth flow with Google's OAuth Playground
5. Review Google Cloud Console audit logs

---

## License

This project is part of the AWAD Email Client application.

