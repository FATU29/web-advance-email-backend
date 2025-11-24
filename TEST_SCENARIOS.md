# Test Scenarios: Authentication Flows

This document describes all authentication scenarios and their expected behaviors.

## Scenario 1: Normal Email/Password Signup

### Steps:
1. User signs up with email and password
2. User receives OTP via email
3. User verifies email with OTP
4. User can now login

### API Calls:

```bash
# Step 1: Sign up
POST /api/auth/signup
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123"
}

# Response:
{
  "success": true,
  "message": "Signup successful. Please check your email for verification code.",
  "data": null
}

# Step 2: Check email for OTP (e.g., "123456")

# Step 3: Verify email
POST /api/auth/verify-email
{
  "email": "john@example.com",
  "code": "123456"
}

# Response:
{
  "success": true,
  "message": "Email verified successfully",
  "data": {
    "accessToken": "...",
    "refreshToken": "...",
    "user": { ... }
  }
}

# Step 4: Login (now works)
POST /api/auth/login
{
  "email": "john@example.com",
  "password": "password123"
}

# Response: Success with tokens
```

### Database State:
```javascript
{
  "email": "john@example.com",
  "password": "hashed_password",
  "authProvider": "EMAIL",
  "enabled": true,
  "verified": true,
  "googleId": null
}
```

---

## Scenario 2: Email Signup Without Verification (Cannot Login)

### Steps:
1. User signs up with email and password
2. User does NOT verify email
3. User tries to login → **FAILS**

### API Calls:

```bash
# Step 1: Sign up
POST /api/auth/signup
{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "password": "password123"
}

# Response: Success, OTP sent

# Step 2: User ignores email

# Step 3: Try to login
POST /api/auth/login
{
  "email": "jane@example.com",
  "password": "password123"
}

# Response: ERROR
{
  "success": false,
  "message": "Account is not verified. Please verify your email.",
  "data": null
}
```

### Database State:
```javascript
{
  "email": "jane@example.com",
  "password": "hashed_password",
  "authProvider": "EMAIL",
  "enabled": false,      // ❌ Not enabled
  "verified": false,     // ❌ Not verified
  "googleId": null
}
```

### Solution:
User must verify email first:
```bash
POST /api/auth/resend-verification-otp
{
  "email": "jane@example.com"
}

# Then verify with OTP
POST /api/auth/verify-email
{
  "email": "jane@example.com",
  "code": "123456"
}
```

---

## Scenario 3: Google OAuth Signup (Auto-Verified)

### Steps:
1. User clicks "Login with Google"
2. User authorizes the app
3. User is automatically logged in (no email verification needed)

### API Calls:

```bash
# Step 1-2: Frontend handles Google OAuth flow and gets authorization code

# Step 3: Exchange code for tokens
POST /api/auth/google
{
  "code": "google_authorization_code_here"
}

# Response: Success with tokens (immediate login)
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "...",
    "refreshToken": "...",
    "user": {
      "email": "user@gmail.com",
      "name": "Google User",
      "profilePicture": "https://..."
    }
  }
}
```

### Database State:
```javascript
{
  "email": "user@gmail.com",
  "password": null,          // No password
  "authProvider": "GOOGLE",
  "enabled": true,           // ✅ Auto-enabled
  "verified": true,          // ✅ Auto-verified
  "googleId": "google_user_id"
}
```

---

## Scenario 4: Email Signup → Google Login (Account Linking)

### Steps:
1. User signs up with email but doesn't verify
2. User tries to login with Google using same email
3. Google login **automatically verifies** the account
4. Account is linked (authProvider becomes `BOTH`)

### API Calls:

```bash
# Step 1: Sign up with email
POST /api/auth/signup
{
  "name": "Bob Smith",
  "email": "bob@gmail.com",
  "password": "password123"
}

# Response: Success, OTP sent (but user ignores it)

# Step 2: User tries to login with email/password → FAILS
POST /api/auth/login
{
  "email": "bob@gmail.com",
  "password": "password123"
}

# Response: ERROR - Account not verified

# Step 3: User logs in with Google instead
POST /api/auth/google
{
  "code": "google_authorization_code"
}

# Response: Success! Account is now verified and linked
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "...",
    "user": { ... }
  }
}

# Step 4: Now user can login with EITHER method
POST /api/auth/login
{
  "email": "bob@gmail.com",
  "password": "password123"
}

# Response: Success! (now works because Google verified the account)
```

### Database State After Google Login:
```javascript
{
  "email": "bob@gmail.com",
  "password": "hashed_password",  // Still has password
  "authProvider": "BOTH",         // ✅ Changed from EMAIL to BOTH
  "enabled": true,                // ✅ Now enabled
  "verified": true,               // ✅ Now verified by Google
  "googleId": "google_user_id"    // ✅ Google ID added
}
```

### Key Points:
- ✅ Google login **automatically verifies** the email
- ✅ Any pending OTPs are deleted
- ✅ User can now login with **both** email/password and Google
- ✅ No email verification needed after Google login

---

## Scenario 5: Google Signup → Set Password (Account Linking)

### Steps:
1. User signs up with Google
2. User wants to set a password
3. User uses "Forgot Password" to set password
4. Account becomes `BOTH` type

### API Calls:

```bash
# Step 1: Sign up with Google
POST /api/auth/google
{
  "code": "google_authorization_code"
}

# Response: Success, logged in

# Step 2: User wants to set password, uses forgot password
POST /api/auth/forgot-password
{
  "email": "user@gmail.com"
}

# Response: OTP sent

# Step 3: Reset password (actually setting it for first time)
POST /api/auth/reset-password
{
  "email": "user@gmail.com",
  "code": "123456",
  "newPassword": "newPassword123"
}

# Response: Success

# Step 4: Now user can login with EITHER method
POST /api/auth/login
{
  "email": "user@gmail.com",
  "password": "newPassword123"
}

# Response: Success!
```

### Database State After Setting Password:
```javascript
{
  "email": "user@gmail.com",
  "password": "hashed_password",  // ✅ Password now set
  "authProvider": "BOTH",         // ✅ Changed from GOOGLE to BOTH
  "enabled": true,
  "verified": true,
  "googleId": "google_user_id"
}
```

---

## Scenario 6: Forgot Password Flow

### Steps:
1. User forgets password
2. User requests password reset
3. User receives OTP
4. User resets password with OTP

### API Calls:

```bash
# Step 1-2: Request password reset
POST /api/auth/forgot-password
{
  "email": "john@example.com"
}

# Response: OTP sent

# Step 3: Check email for OTP

# Step 4: Reset password
POST /api/auth/reset-password
{
  "email": "john@example.com",
  "code": "123456",
  "newPassword": "newPassword123"
}

# Response: Success

# Step 5: Login with new password
POST /api/auth/login
{
  "email": "john@example.com",
  "password": "newPassword123"
}

# Response: Success
```

### Security:
- ✅ All refresh tokens are revoked after password reset
- ✅ User must re-login on all devices

---

## Scenario 7: Change Password (Authenticated)

### Steps:
1. User is logged in
2. User requests OTP for password change
3. User changes password with OTP
4. All sessions are invalidated

### API Calls:

```bash
# Step 1: Request OTP (requires authentication)
POST /api/auth/send-change-password-otp
Authorization: Bearer {access_token}

# Response: OTP sent

# Step 2: Change password
POST /api/auth/change-password
Authorization: Bearer {access_token}
{
  "currentPassword": "oldPassword123",
  "newPassword": "newPassword123",
  "code": "123456"
}

# Response: Success

# Step 3: User must re-login (all tokens revoked)
POST /api/auth/login
{
  "email": "john@example.com",
  "password": "newPassword123"
}
```

---

## Summary Table

| Scenario | Email Verified? | Can Login? | Auth Provider |
|----------|----------------|------------|---------------|
| Email signup (verified) | ✅ Yes | ✅ Yes | EMAIL |
| Email signup (not verified) | ❌ No | ❌ No | EMAIL |
| Google signup | ✅ Auto | ✅ Yes | GOOGLE |
| Email signup → Google login | ✅ Auto by Google | ✅ Yes | BOTH |
| Google signup → Set password | ✅ Yes | ✅ Yes (both methods) | BOTH |

## Key Behaviors

### ✅ Email Verification Required for Normal Signup
- Users who sign up with email/password **MUST** verify their email before logging in
- Login attempts before verification will fail with: "Account is not verified. Please verify your email."

### ✅ Google OAuth Auto-Verifies
- Users who sign up or login with Google are **automatically verified**
- No email verification needed
- Can login immediately

### ✅ Google Login Verifies Existing Accounts
- If a user signs up with email but doesn't verify
- Then logs in with Google using the same email
- The account is **automatically verified** by Google
- User can then login with either method

### ✅ Dual Authentication Support
- Users can have both email/password and Google OAuth
- `authProvider` can be: `EMAIL`, `GOOGLE`, or `BOTH`
- Users with `BOTH` can login using either method

### ✅ Security Features
- OTP expires after 60 seconds
- Maximum 5 verification attempts per OTP
- All refresh tokens revoked after password changes
- Pending OTPs deleted when account is verified via Google

---

## Testing Checklist

- [ ] Email signup without verification → Login fails
- [ ] Email signup with verification → Login succeeds
- [ ] Google signup → Login succeeds immediately
- [ ] Email signup (unverified) → Google login → Email login now works
- [ ] Google signup → Set password via forgot password → Email login works
- [ ] Forgot password flow works
- [ ] Change password flow works
- [ ] OTP expiration works (60 seconds)
- [ ] OTP attempt limiting works (5 attempts)
- [ ] Resend OTP works

