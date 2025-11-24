# Email Verification Behavior

## Quick Summary

### ❌ Email/Password Signup → **CANNOT LOGIN** until email is verified
### ✅ Google OAuth Signup → **CAN LOGIN IMMEDIATELY** (auto-verified)
### ✅ Email Signup (unverified) → Google Login → **ACCOUNT AUTO-VERIFIED** → Can login with both methods

---

## Detailed Behavior

### 1. Email/Password Signup Flow

**User Action:**
```bash
POST /api/auth/signup
{
  "email": "user@example.com",
  "password": "password123"
}
```

**What Happens:**
1. User account is created with:
   - `enabled: false` ❌
   - `verified: false` ❌
   - `authProvider: "EMAIL"`
2. OTP is generated and sent to user's email
3. User receives 6-digit code (valid for 60 seconds)

**Database State:**
```javascript
{
  "email": "user@example.com",
  "password": "hashed_password",
  "authProvider": "EMAIL",
  "enabled": false,      // ❌ Cannot login yet
  "verified": false,     // ❌ Email not verified
  "googleId": null
}
```

**Login Attempt (BEFORE Verification):**
```bash
POST /api/auth/login
{
  "email": "user@example.com",
  "password": "password123"
}

# Response: ERROR ❌
{
  "success": false,
  "message": "Account is not verified. Please verify your email.",
  "data": null
}
```

**After Email Verification:**
```bash
POST /api/auth/verify-email
{
  "email": "user@example.com",
  "code": "123456"
}

# Response: Success ✅
# User is now logged in and receives tokens
```

**Database State After Verification:**
```javascript
{
  "email": "user@example.com",
  "password": "hashed_password",
  "authProvider": "EMAIL",
  "enabled": true,       // ✅ Can login now
  "verified": true,      // ✅ Email verified
  "googleId": null
}
```

**Login Attempt (AFTER Verification):**
```bash
POST /api/auth/login
{
  "email": "user@example.com",
  "password": "password123"
}

# Response: Success ✅
# User receives access and refresh tokens
```

---

### 2. Google OAuth Signup Flow

**User Action:**
```bash
POST /api/auth/google
{
  "code": "google_authorization_code"
}
```

**What Happens:**
1. User account is created with:
   - `enabled: true` ✅ (immediately enabled)
   - `verified: true` ✅ (Google verified the email)
   - `authProvider: "GOOGLE"`
2. User is **immediately logged in**
3. **No email verification needed**

**Database State:**
```javascript
{
  "email": "user@gmail.com",
  "password": null,          // No password set
  "authProvider": "GOOGLE",
  "enabled": true,           // ✅ Can login immediately
  "verified": true,          // ✅ Auto-verified by Google
  "googleId": "google_user_id"
}
```

**Result:**
- ✅ User can login immediately with Google
- ✅ No email verification step required
- ✅ Account is fully active from the start

---

### 3. Email Signup (Unverified) → Google Login (Auto-Verification)

This is a special case where Google login **automatically verifies** an existing unverified account.

**Step 1: User signs up with email but doesn't verify**
```bash
POST /api/auth/signup
{
  "email": "user@gmail.com",
  "password": "password123"
}

# User receives OTP but ignores it
```

**Database State:**
```javascript
{
  "email": "user@gmail.com",
  "password": "hashed_password",
  "authProvider": "EMAIL",
  "enabled": false,      // ❌ Not enabled
  "verified": false,     // ❌ Not verified
  "googleId": null
}
```

**Step 2: User tries to login with email/password → FAILS**
```bash
POST /api/auth/login
{
  "email": "user@gmail.com",
  "password": "password123"
}

# Response: ERROR ❌
{
  "success": false,
  "message": "Account is not verified. Please verify your email.",
  "data": null
}
```

**Step 3: User logs in with Google instead**
```bash
POST /api/auth/google
{
  "code": "google_authorization_code"
}

# Response: Success ✅
# User is logged in immediately
```

**What Happens Behind the Scenes:**
1. Backend finds existing user by email
2. Links Google account to existing user
3. **Automatically verifies the account:**
   - `enabled: true` ✅
   - `verified: true` ✅
   - `authProvider: "BOTH"` ✅
   - `googleId: "google_user_id"` ✅
4. Deletes any pending OTPs
5. User is logged in

**Database State After Google Login:**
```javascript
{
  "email": "user@gmail.com",
  "password": "hashed_password",  // Still has password
  "authProvider": "BOTH",         // ✅ Changed to BOTH
  "enabled": true,                // ✅ Now enabled
  "verified": true,               // ✅ Verified by Google
  "googleId": "google_user_id"    // ✅ Google linked
}
```

**Step 4: User can now login with EITHER method**
```bash
# Option 1: Login with email/password
POST /api/auth/login
{
  "email": "user@gmail.com",
  "password": "password123"
}
# Response: Success ✅

# Option 2: Login with Google
POST /api/auth/google
{
  "code": "google_authorization_code"
}
# Response: Success ✅
```

**Result:**
- ✅ Google login **automatically verified** the email
- ✅ User can now login with **both** methods
- ✅ No email OTP verification needed
- ✅ Account is fully active

---

## Comparison Table

| Signup Method | Email Verified? | Can Login Immediately? | Auth Provider | Password | Google ID |
|---------------|----------------|----------------------|---------------|----------|-----------|
| Email (verified) | ✅ Yes (via OTP) | ✅ Yes | EMAIL | ✅ Yes | ❌ No |
| Email (not verified) | ❌ No | ❌ No | EMAIL | ✅ Yes | ❌ No |
| Google | ✅ Auto | ✅ Yes | GOOGLE | ❌ No | ✅ Yes |
| Email → Google | ✅ Auto by Google | ✅ Yes (after Google) | BOTH | ✅ Yes | ✅ Yes |

---

## Code Implementation

### Login Method (AuthService.java)

```java
@Transactional
public AuthResponse login(LoginRequest request) {
    User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

    // Check if user has password
    if (user.getPassword() == null) {
        throw new BadRequestException("Please use Google to login");
    }

    // Verify password
    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
        throw new UnauthorizedException("Invalid email or password");
    }

    // ⚠️ CHECK IF ACCOUNT IS VERIFIED
    if (!user.isEnabled()) {
        throw new UnauthorizedException("Account is not verified. Please verify your email.");
    }

    return generateAuthResponse(user);
}
```

### Google OAuth Method (AuthService.java)

```java
// When linking Google to existing account
existingUser.setGoogleId(googleId);
if (existingUser.getPassword() != null) {
    existingUser.setAuthProvider(User.AuthProvider.BOTH);
} else {
    existingUser.setAuthProvider(User.AuthProvider.GOOGLE);
}
existingUser.setProfilePicture(pictureUrl);
// ✅ AUTOMATICALLY VERIFY THE ACCOUNT
existingUser.setVerified(true);
existingUser.setEnabled(true);
existingUser.setUpdatedAt(LocalDateTime.now());

// ✅ DELETE PENDING OTPs
otpService.deleteOtp(email);

return userRepository.save(existingUser);
```

---

## Error Messages

### Unverified Account (Email/Password Login)
```json
{
  "success": false,
  "message": "Account is not verified. Please verify your email.",
  "data": null
}
```

**Solution:** User must verify email with OTP or login with Google

### Expired OTP
```json
{
  "success": false,
  "message": "Invalid or expired OTP",
  "data": null
}
```

**Solution:** Request new OTP via `/api/auth/resend-verification-otp`

### Max Attempts Exceeded
```json
{
  "success": false,
  "message": "Maximum verification attempts exceeded. Please request a new OTP.",
  "data": null
}
```

**Solution:** Request new OTP via `/api/auth/resend-verification-otp`

---

## Best Practices

### For Users

1. **Email/Password Signup:**
   - Check your email immediately after signup
   - Verify within 60 seconds (OTP expires)
   - If OTP expires, request a new one
   - You cannot login until verified

2. **Google OAuth:**
   - No email verification needed
   - Can login immediately
   - Recommended for faster onboarding

3. **Forgot to Verify Email?**
   - Just login with Google (same email)
   - Your account will be automatically verified
   - You can then use both login methods

### For Developers

1. **Always check `enabled` field** before allowing login
2. **Set `verified: true` and `enabled: true`** for Google OAuth users
3. **Auto-verify existing accounts** when linking Google
4. **Delete pending OTPs** when account is verified via Google
5. **Provide clear error messages** for unverified accounts

---

## Testing Checklist

- [ ] Email signup without verification → Login fails with clear error
- [ ] Email signup with verification → Login succeeds
- [ ] Google signup → Login succeeds immediately (no verification)
- [ ] Email signup (unverified) → Google login → Email login now works
- [ ] Resend OTP works for unverified accounts
- [ ] OTP expires after 60 seconds
- [ ] Max 5 verification attempts per OTP
- [ ] Clear error messages for all scenarios

---

## Summary

### The Golden Rules:

1. **Email/Password users MUST verify their email before login** ❌→✅
2. **Google OAuth users are auto-verified and can login immediately** ✅
3. **Google login can auto-verify existing unverified accounts** ❌→✅
4. **Users with BOTH auth methods can login either way** ✅

This design provides:
- ✅ Security (email verification for password accounts)
- ✅ Convenience (Google auto-verification)
- ✅ Flexibility (dual authentication support)
- ✅ User-friendly (Google can rescue unverified accounts)

