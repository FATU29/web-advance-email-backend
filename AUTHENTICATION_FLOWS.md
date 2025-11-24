# Authentication Flows Diagram

## Flow 1: Email/Password Signup (Requires Verification)

```
┌─────────────────────────────────────────────────────────────────┐
│                    EMAIL/PASSWORD SIGNUP FLOW                    │
└─────────────────────────────────────────────────────────────────┘

User                    Backend                  Email Service
  │                        │                           │
  │  POST /signup          │                           │
  ├───────────────────────>│                           │
  │  {email, password}     │                           │
  │                        │                           │
  │                        │  Generate OTP             │
  │                        │  Save to DB               │
  │                        │  (enabled: false)         │
  │                        │                           │
  │                        │  Send OTP Email           │
  │                        ├──────────────────────────>│
  │                        │                           │
  │  Response: Success     │                           │
  │  "Check your email"    │                           │
  │<───────────────────────┤                           │
  │                        │                           │
  │                        │      Email with OTP       │
  │<──────────────────────────────────────────────────┤
  │                        │                           │
  │  POST /verify-email    │                           │
  ├───────────────────────>│                           │
  │  {email, code}         │                           │
  │                        │                           │
  │                        │  Verify OTP               │
  │                        │  Set enabled: true        │
  │                        │  Set verified: true       │
  │                        │                           │
  │  Response: Success     │                           │
  │  + Auth Tokens         │                           │
  │<───────────────────────┤                           │
  │                        │                           │
  │  POST /login           │                           │
  ├───────────────────────>│                           │
  │  {email, password}     │                           │
  │                        │                           │
  │                        │  Check enabled: true ✅   │
  │                        │  Check verified: true ✅  │
  │                        │                           │
  │  Response: Success     │                           │
  │  + Auth Tokens         │                           │
  │<───────────────────────┤                           │
  │                        │                           │

Result: User can login with email/password
Database: {authProvider: "EMAIL", enabled: true, verified: true}
```

## Flow 2: Email Signup WITHOUT Verification (Login Fails)

```
┌─────────────────────────────────────────────────────────────────┐
│              EMAIL SIGNUP WITHOUT VERIFICATION                   │
└─────────────────────────────────────────────────────────────────┘

User                    Backend
  │                        │
  │  POST /signup          │
  ├───────────────────────>│
  │                        │
  │  Response: Success     │
  │  "Check your email"    │
  │<───────────────────────┤
  │                        │
  │  (User ignores email)  │
  │                        │
  │  POST /login           │
  ├───────────────────────>│
  │  {email, password}     │
  │                        │
  │                        │  Check enabled: false ❌
  │                        │
  │  Response: ERROR       │
  │  "Account not verified"│
  │<───────────────────────┤
  │                        │

Result: Login FAILS - User must verify email first
Database: {authProvider: "EMAIL", enabled: false, verified: false}
```

## Flow 3: Google OAuth Signup (Auto-Verified)

```
┌─────────────────────────────────────────────────────────────────┐
│              GOOGLE OAUTH SIGNUP (AUTO-VERIFIED)                │
└─────────────────────────────────────────────────────────────────┘

User                    Backend                  Google
  │                        │                        │
  │  Click "Login with     │                        │
  │  Google"               │                        │
  ├───────────────────────>│                        │
  │                        │                        │
  │                        │  Redirect to Google    │
  │<───────────────────────┤                        │
  │                        │                        │
  │  Authorize App         │                        │
  ├────────────────────────────────────────────────>│
  │                        │                        │
  │  Authorization Code    │                        │
  │<────────────────────────────────────────────────┤
  │                        │                        │
  │  POST /google          │                        │
  ├───────────────────────>│                        │
  │  {code}                │                        │
  │                        │                        │
  │                        │  Exchange code         │
  │                        ├───────────────────────>│
  │                        │                        │
  │                        │  User info + tokens    │
  │                        │<───────────────────────┤
  │                        │                        │
  │                        │  Create user:          │
  │                        │  - enabled: true ✅    │
  │                        │  - verified: true ✅   │
  │                        │  - authProvider: GOOGLE│
  │                        │                        │
  │  Response: Success     │                        │
  │  + Auth Tokens         │                        │
  │  (Immediate login!)    │                        │
  │<───────────────────────┤                        │
  │                        │                        │

Result: User is logged in immediately (no email verification needed)
Database: {authProvider: "GOOGLE", enabled: true, verified: true, googleId: "..."}
```

## Flow 4: Email Signup → Google Login (Account Linking & Auto-Verification)

```
┌─────────────────────────────────────────────────────────────────┐
│         EMAIL SIGNUP → GOOGLE LOGIN (AUTO-VERIFICATION)         │
└─────────────────────────────────────────────────────────────────┘

User                    Backend
  │                        │
  │  POST /signup          │
  ├───────────────────────>│  Create user:
  │  {email, password}     │  - enabled: false ❌
  │                        │  - verified: false ❌
  │                        │  - authProvider: EMAIL
  │  Response: Success     │
  │<───────────────────────┤
  │                        │
  │  (User ignores email)  │
  │                        │
  │  POST /login           │
  ├───────────────────────>│
  │                        │  Check enabled: false ❌
  │  Response: ERROR       │
  │  "Not verified"        │
  │<───────────────────────┤
  │                        │
  │  POST /google          │
  ├───────────────────────>│
  │  {code}                │  Find user by email
  │                        │  Link Google account:
  │                        │  - googleId: "..." ✅
  │                        │  - enabled: true ✅
  │                        │  - verified: true ✅
  │                        │  - authProvider: BOTH ✅
  │                        │  - Delete pending OTPs
  │  Response: Success     │
  │  + Auth Tokens         │
  │<───────────────────────┤
  │                        │
  │  POST /login           │
  ├───────────────────────>│
  │  {email, password}     │  Check enabled: true ✅
  │                        │
  │  Response: Success     │
  │  + Auth Tokens         │
  │<───────────────────────┤
  │                        │

Result: Google login automatically verified the account!
        User can now login with BOTH email/password AND Google
Database: {authProvider: "BOTH", enabled: true, verified: true, googleId: "...", password: "..."}
```

## Flow 5: Google Signup → Set Password (Forgot Password)

```
┌─────────────────────────────────────────────────────────────────┐
│           GOOGLE SIGNUP → SET PASSWORD VIA FORGOT PASSWORD      │
└─────────────────────────────────────────────────────────────────┘

User                    Backend
  │                        │
  │  POST /google          │
  ├───────────────────────>│  Create user:
  │                        │  - authProvider: GOOGLE
  │                        │  - password: null
  │  Response: Success     │  - enabled: true
  │<───────────────────────┤  - verified: true
  │                        │
  │  POST /forgot-password │
  ├───────────────────────>│
  │  {email}               │  Send OTP
  │                        │
  │  Response: Success     │
  │<───────────────────────┤
  │                        │
  │  POST /reset-password  │
  ├───────────────────────>│
  │  {email, code, newPwd} │  Set password
  │                        │  authProvider: BOTH ✅
  │  Response: Success     │
  │<───────────────────────┤
  │                        │
  │  POST /login           │
  ├───────────────────────>│
  │  {email, password}     │  Now works! ✅
  │                        │
  │  Response: Success     │
  │<───────────────────────┤
  │                        │

Result: User can now login with BOTH Google AND email/password
Database: {authProvider: "BOTH", password: "...", googleId: "...", enabled: true, verified: true}
```

## Summary Table

| Scenario | Initial State | Action | Final State | Can Login? |
|----------|--------------|--------|-------------|------------|
| Email signup + verify | enabled: false | Verify OTP | enabled: true, verified: true | ✅ Yes (email/pwd) |
| Email signup (no verify) | enabled: false | None | enabled: false, verified: false | ❌ No |
| Google signup | N/A | Google OAuth | enabled: true, verified: true | ✅ Yes (Google) |
| Email (unverified) → Google | enabled: false | Google OAuth | enabled: true, verified: true, authProvider: BOTH | ✅ Yes (both methods) |
| Google → Set password | password: null | Forgot password | password: set, authProvider: BOTH | ✅ Yes (both methods) |

## Key Points

### ✅ Email Verification is REQUIRED for Email/Password Signup
- Users **CANNOT** login until they verify their email
- Login will fail with: "Account is not verified. Please verify your email."

### ✅ Google OAuth Auto-Verifies
- Google users are **immediately verified** and can login
- No email verification step needed

### ✅ Google Login Can Verify Existing Accounts
- If user signs up with email but doesn't verify
- Then logs in with Google (same email)
- Account is **automatically verified** by Google
- User can then use **both** login methods

### ✅ Dual Authentication Support
- Users can have `authProvider: "BOTH"`
- Can login with **either** email/password **or** Google
- Flexible authentication options

## Authentication Provider States

```
┌──────────┐
│  EMAIL   │  User signed up with email/password
│          │  - Has password
│          │  - No Google ID
└──────────┘

┌──────────┐
│  GOOGLE  │  User signed up with Google
│          │  - No password (null)
│          │  - Has Google ID
└──────────┘

┌──────────┐
│   BOTH   │  User has both authentication methods
│          │  - Has password
│          │  - Has Google ID
│          │  - Can login with either method
└──────────┘
```

## Transitions

```
EMAIL ──────────────────────> BOTH
      (Google login links account)

GOOGLE ─────────────────────> BOTH
       (Set password via forgot password)

EMAIL ──────────────────────> EMAIL
      (No change, just verify email)

GOOGLE ─────────────────────> GOOGLE
       (No change, already verified)
```

