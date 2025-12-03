# Frontend Authentication Integration Guide

This document describes how to integrate with the backend authentication system.

## Overview

The authentication system uses:
- **Access Token**: Short-lived JWT (1 hour) - stored in memory on frontend
- **Refresh Token**: Long-lived token (24 hours) - stored in **HttpOnly cookie** (handled automatically by browser)

> **Important**: The refresh token is never exposed to JavaScript. It's sent and received automatically by the browser via cookies.

---

## Key Changes from Previous Implementation

| Before | After |
|--------|-------|
| Refresh token returned in JSON response body | Refresh token set as HttpOnly cookie (not in response body) |
| Frontend stores refresh token (localStorage/memory) | Frontend does NOT store refresh token |
| Frontend sends refresh token in request body | Browser sends refresh token automatically via cookie |
| Manual token management | Automatic cookie management by browser |

---

## Configuration Requirements

### 1. Axios Global Configuration

```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  withCredentials: true, // CRITICAL: This enables cookies to be sent/received
});

export default api;
```

### 2. Fetch API Configuration

```javascript
fetch('http://localhost:8080/api/auth/login', {
  method: 'POST',
  credentials: 'include', // CRITICAL: This enables cookies
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({ email, password }),
});
```

---

## Authentication Flow

### 1. Login (Email/Password)

**Request:**
```javascript
const response = await api.post('/auth/login', {
  email: 'user@example.com',
  password: 'password123'
});
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": null,
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": "user123",
      "email": "user@example.com",
      "name": "John Doe",
      "profilePicture": "https://..."
    }
  }
}
```

> **Note**: `refreshToken` is `null` in the response. The actual refresh token is set as an HttpOnly cookie automatically.

**Frontend Action:**
```javascript
// Store access token in memory (NOT localStorage for security)
const { accessToken, user } = response.data.data;
setAccessToken(accessToken);  // Store in React state/context/Zustand
setUser(user);
```

---

### 2. Google OAuth Login

**Request:**
```javascript
const response = await api.post('/auth/google', {
  code: 'authorization_code_from_google'
});
```

**Response:** Same format as login. Refresh token is set via HttpOnly cookie.

---

### 3. Sign Up + Email Verification

**Step 1: Sign Up**
```javascript
await api.post('/auth/signup', {
  name: 'John Doe',
  email: 'user@example.com',
  password: 'password123'
});
// Response: { success: true, message: "...", data: null }
// User receives OTP via email
```

**Step 2: Verify Email**
```javascript
const response = await api.post('/auth/verify-email', {
  email: 'user@example.com',
  code: '123456'  // OTP from email
});
// Response includes accessToken and sets refresh token cookie
```

---

### 4. Token Refresh

When the access token expires, call the refresh endpoint. The browser automatically sends the refresh token cookie.

**Request:**
```javascript
const response = await api.post('/auth/refresh');
// No body needed! Cookie is sent automatically.
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": null,
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": "user123",
      "email": "user@example.com",
      "name": "John Doe",
      "profilePicture": "https://..."
    }
  }
}
```

**Error Response:** `401 Unauthorized`
```json
{
  "success": false,
  "message": "No refresh token provided",
  "data": null
}
```

---

### 5. Logout

**Request:**
```javascript
await api.post('/auth/logout');
// Requires Authorization header with access token
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Logout successful",
  "data": null
}
```

The server will:
1. Revoke the refresh token in the database
2. Clear the HttpOnly cookie

**Frontend Action:**
```javascript
// Clear access token from memory
setAccessToken(null);
setUser(null);
// Redirect to login page
```

---

## Implementing Auto Token Refresh

### Axios Interceptor Example

```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  withCredentials: true,
});

// Store for access token (use your state management)
let accessToken = null;
export const setAccessToken = (token) => { accessToken = token; };
export const getAccessToken = () => accessToken;

// Request interceptor - add access token to headers
api.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor - handle token refresh
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // If 401 and not already retrying
    if (error.response?.status === 401 && !originalRequest._retry) {
      
      // If already refreshing, queue this request
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return api(originalRequest);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // Call refresh endpoint - cookie sent automatically
        const response = await api.post('/auth/refresh');
        const { accessToken: newToken } = response.data.data;
        
        setAccessToken(newToken);
        processQueue(null, newToken);
        
        // Retry original request with new token
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return api(originalRequest);
        
      } catch (refreshError) {
        processQueue(refreshError, null);
        
        // Refresh failed - clear auth state and redirect to login
        setAccessToken(null);
        window.location.href = '/login';
        
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default api;
```

---

## Making Authenticated API Requests

```javascript
// The interceptor automatically adds the Authorization header
const emails = await api.get('/emails');
const mailboxes = await api.get('/mailboxes');

// Sending email
await api.post('/emails/send', {
  to: ['recipient@example.com'],
  subject: 'Hello',
  body: '<p>Hello World</p>'
});
```

---

## Cookie Details (For Reference)

The refresh token cookie has these attributes:

| Attribute | Value | Purpose |
|-----------|-------|---------|
| `Name` | `refreshToken` | Cookie identifier |
| `HttpOnly` | `true` | Prevents JavaScript access (XSS protection) |
| `Secure` | `true` (production) | Only sent over HTTPS |
| `SameSite` | `None` | Allows cross-origin requests |
| `Path` | `/api/auth` | Cookie only sent to auth endpoints |
| `Max-Age` | `86400` (24 hours) | Cookie expiration |

---

## Error Handling

### Common Error Responses

**401 Unauthorized** - Token expired or invalid
```json
{
  "success": false,
  "message": "Invalid or expired token",
  "data": null
}
```

**400 Bad Request** - Validation error
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "email": "Email is required",
    "password": "Password must be at least 8 characters"
  }
}
```

---

## Security Best Practices for Frontend

1. **Never store access token in localStorage** - Use in-memory storage (React state, Zustand, etc.)
2. **Always use `withCredentials: true`** - Required for cookies to work
3. **Implement token refresh interceptor** - Automatically refresh expired tokens
4. **Clear auth state on logout** - Remove access token from memory
5. **Handle refresh failures** - Redirect to login when refresh token is invalid/expired

---

## Quick Reference: Endpoints

| Endpoint | Method | Auth Required | Body | Cookie Sent |
|----------|--------|---------------|------|-------------|
| `/api/auth/signup` | POST | No | `{name, email, password}` | No |
| `/api/auth/verify-email` | POST | No | `{email, code}` | No |
| `/api/auth/login` | POST | No | `{email, password}` | No |
| `/api/auth/google` | POST | No | `{code}` | No |
| `/api/auth/refresh` | POST | No | None | Yes (auto) |
| `/api/auth/logout` | POST | Yes | None | Yes (auto) |
| `/api/auth/me` | GET | Yes | None | No |
| `/api/auth/forgot-password` | POST | No | `{email}` | No |
| `/api/auth/reset-password` | POST | No | `{email, code, newPassword}` | No |

---

## Testing Checklist

- [ ] Login returns access token and sets cookie (check browser DevTools > Application > Cookies)
- [ ] Authenticated requests work with `Authorization: Bearer <token>` header
- [ ] Token refresh works when access token expires (no body needed)
- [ ] Logout clears the cookie
- [ ] Cross-origin requests work (if frontend/backend on different domains)
- [ ] Token refresh queue works (multiple simultaneous 401s don't cause multiple refresh calls)

