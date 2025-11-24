# Setup Guide: OTP Verification System

## Prerequisites

- Java 21
- Maven 3.6+
- MongoDB 4.4+
- Brevo (Sendinblue) account

## Step 1: Get Brevo API Key

1. Go to [Brevo](https://www.brevo.com/) and create an account (free tier available)
2. Navigate to **Settings** → **SMTP & API** → **API Keys**
3. Click **Generate a new API key**
4. Copy the API key (you'll need it for the `.env` file)

## Step 2: Configure Environment Variables

Create a `.env` file in the project root with the following variables:

```env
# MongoDB Configuration
MONGODB_URI=mongodb://localhost:27017/awad_email

# JWT Configuration
JWT_SECRET=your_super_secret_jwt_key_here_make_it_long_and_random
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000

# Google OAuth Configuration
GOOGLE_CLIENT_ID=your_google_client_id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your_google_client_secret
GOOGLE_REDIRECT_URI=http://localhost:8080/api/auth/google/callback

# Brevo Email Service Configuration
BREVO_API_KEY=xkeysib-your_brevo_api_key_here
SENDER_EMAIL=noreply@yourdomain.com
SENDER_NAME=Your App Name

# Application Configuration
SERVER_PORT=8080
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

## Step 3: Update application.yml

The `application.yml` file should already be configured, but verify these settings:

```yaml
app:
  email:
    sender:
      email: ${SENDER_EMAIL:noreply@example.com}
      name: ${SENDER_NAME:AWAD Email}
  api-key:
    brevo: ${BREVO_API_KEY}
  otp:
    length: 6
    duration: 60  # seconds
```

## Step 4: Install Dependencies

```bash
mvn clean install
```

This will download all required dependencies including:
- Brevo SDK (sib-api-v3-sdk:6.0.0)
- Spring Boot dependencies
- MongoDB driver
- JWT libraries
- Google OAuth libraries

## Step 5: Start MongoDB

Make sure MongoDB is running on your local machine:

```bash
# macOS (with Homebrew)
brew services start mongodb-community

# Linux (systemd)
sudo systemctl start mongod

# Windows
net start MongoDB

# Or use Docker
docker run -d -p 27017:27017 --name mongodb mongo:latest
```

## Step 6: Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## Step 7: Verify Setup

### Check Application Health

```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

### Test Signup Flow

```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "email": "test@example.com",
    "password": "password123"
  }'
```

Expected response:
```json
{
  "success": true,
  "message": "Signup successful. Please check your email for verification code.",
  "data": null
}
```

### Check Email

You should receive an email at `test@example.com` with a 6-digit OTP code.

### Verify Email

```bash
curl -X POST http://localhost:8080/api/auth/verify-email \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "code": "123456"
  }'
```

Expected response:
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
      "id": "...",
      "email": "test@example.com",
      "name": "Test User",
      "profilePicture": null
    }
  }
}
```

## Troubleshooting

### Issue: "Cannot connect to MongoDB"

**Solution:**
- Verify MongoDB is running: `mongosh` or `mongo`
- Check MongoDB URI in `.env` file
- Ensure MongoDB port (27017) is not blocked

### Issue: "Brevo API error"

**Solution:**
- Verify Brevo API key is correct
- Check Brevo account status (free tier has limits)
- Ensure sender email is verified in Brevo dashboard

### Issue: "Email not received"

**Solution:**
- Check spam/junk folder
- Verify sender email is verified in Brevo
- Check Brevo dashboard for email delivery status
- Ensure recipient email is valid

### Issue: "OTP expired"

**Solution:**
- OTPs expire after 60 seconds
- Request a new OTP using `/api/auth/resend-verification-otp`

### Issue: "Maximum attempts exceeded"

**Solution:**
- Each OTP allows 5 verification attempts
- Request a new OTP using the resend endpoint

### Issue: "Google OAuth not working"

**Solution:**
- Verify Google Client ID and Secret in `.env`
- Check redirect URI matches Google Console configuration
- Ensure Google OAuth consent screen is configured

## Development Tips

### Hot Reload

For development, use Spring Boot DevTools (already included):

```bash
mvn spring-boot:run
```

Changes to Java files will trigger automatic restart.

### View Logs

Logs are configured in `application.yml`. To see detailed logs:

```yaml
logging:
  level:
    com.hcmus.awad_email: DEBUG
```

### Database GUI

Use MongoDB Compass to view and manage data:
- Download: https://www.mongodb.com/products/compass
- Connect to: `mongodb://localhost:27017`
- Database: `awad_email`
- Collections: `users`, `otps`, `refresh_tokens`

### API Testing

Use Postman or similar tools:
1. Import the API collection (if available)
2. Set environment variables
3. Test each endpoint

### Email Testing

For development, you can use:
- **Brevo Free Tier**: 300 emails/day
- **Mailtrap**: Email testing service
- **MailHog**: Local email testing server

## Production Deployment

### Environment Variables

Set all environment variables in your production environment:
- Use strong, random JWT secrets
- Use production MongoDB URI
- Use production Brevo API key
- Set appropriate CORS origins

### Security Checklist

- [ ] Change JWT secret to a strong random value
- [ ] Use HTTPS for all endpoints
- [ ] Enable rate limiting
- [ ] Set up monitoring and logging
- [ ] Configure firewall rules
- [ ] Use environment-specific configurations
- [ ] Enable CORS only for trusted origins
- [ ] Set up backup for MongoDB
- [ ] Monitor Brevo email quota

### Recommended Services

- **Hosting**: AWS, Google Cloud, Azure, Heroku
- **Database**: MongoDB Atlas (managed MongoDB)
- **Email**: Brevo (Sendinblue) production plan
- **Monitoring**: New Relic, Datadog, or CloudWatch

## Additional Resources

- [Brevo API Documentation](https://developers.brevo.com/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [MongoDB Documentation](https://docs.mongodb.com/)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)

## Support

For issues or questions:
1. Check the `OTP_AND_PASSWORD_API.md` for API documentation
2. Review `IMPLEMENTATION_SUMMARY.md` for implementation details
3. Check application logs for error messages
4. Verify all environment variables are set correctly

## Quick Start Commands

```bash
# Clone repository
git clone <repository-url>
cd web-advance-email-backend-main

# Create .env file
cp .env.example .env
# Edit .env with your values

# Install dependencies
mvn clean install

# Start MongoDB (if not running)
brew services start mongodb-community

# Run application
mvn spring-boot:run

# Test signup
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","email":"test@example.com","password":"pass123"}'
```

## Next Steps

After setup is complete:
1. Test all authentication flows
2. Customize email templates in `BrevoEmailService.java`
3. Adjust OTP duration in `application.yml` if needed
4. Set up frontend integration
5. Configure production environment
6. Set up monitoring and alerts

---

**Note**: This setup guide assumes a development environment. For production deployment, additional security measures and configurations are required.

