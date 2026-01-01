# Frontend Integration Guide

Complete API reference for integrating with the AWAD Email Backend.

**Base URL:** `http://localhost:8080` (development) or your deployed backend URL

---

## Table of Contents

1. [Authentication](#1-authentication)
2. [Email Operations](#2-email-operations)
3. [Kanban Board](#3-kanban-board)
4. [Search Features](#4-search-features)
5. [Refreshing Emails](#5-refreshing-emails)
6. [TypeScript Interfaces](#6-typescript-interfaces)

---

## API Response Format

All API responses follow this structure:

```typescript
interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
  error?: string;
}
```

---

## 1. Authentication

### 1.1 Google OAuth Login

**Endpoint:** `POST /api/auth/google`

Exchange Google authorization code for JWT tokens.

```typescript
// Request
interface GoogleAuthRequest {
  code: string;  // Authorization code from Google OAuth
}

// Response
interface AuthResponse {
  accessToken: string;
  tokenType: string;  // "Bearer"
  expiresIn: number;  // seconds
  user: {
    id: string;
    email: string;
    name: string;
    profilePicture?: string;
  };
}
```

**Example:**
```typescript
const response = await fetch('/api/auth/google', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include',  // Important for cookies
  body: JSON.stringify({ code: googleAuthCode })
});
```

### 1.2 Email/Password Signup

**Endpoint:** `POST /api/auth/signup`

```typescript
interface SignupRequest {
  name: string;
  email: string;
  password: string;  // min 6 characters
}
```

### 1.3 Verify Email (OTP)

**Endpoint:** `POST /api/auth/verify-email`

```typescript
interface VerifyOtpRequest {
  email: string;
  code: string;  // 6-digit OTP
}
```

### 1.4 Login

**Endpoint:** `POST /api/auth/login`

```typescript
interface LoginRequest {
  email: string;
  password: string;
}
```

### 1.5 Refresh Token

**Endpoint:** `POST /api/auth/refresh`

Refresh token is sent automatically via HttpOnly cookie.

```typescript
const response = await fetch('/api/auth/refresh', {
  method: 'POST',
  credentials: 'include'  // Cookie sent automatically
});
```

### 1.6 Logout

**Endpoint:** `POST /api/auth/logout`

```typescript
await fetch('/api/auth/logout', {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${accessToken}` },
  credentials: 'include'
});
```

### 1.7 Get Current User

**Endpoint:** `GET /api/auth/me`

```typescript
interface UserInfo {
  id: string;
  email: string;
  name: string;
  profilePicture?: string;
}
```

### 1.8 Forgot Password

**Endpoint:** `POST /api/auth/forgot-password`

```typescript
interface ForgotPasswordRequest {
  email: string;
}
```

### 1.9 Reset Password

**Endpoint:** `POST /api/auth/reset-password`

```typescript
interface ResetPasswordRequest {
  email: string;
  code: string;      // OTP code
  newPassword: string;
}
```

---

## 2. Email Operations

> **Note:** All email endpoints require `Authorization: Bearer <accessToken>` header.

### 2.1 Get Mailboxes (Gmail Labels)

**Endpoint:** `GET /api/mailboxes`

```typescript
interface MailboxResponse {
  id: string;        // Gmail label ID (e.g., "INBOX", "SENT")
  name: string;
  type: 'INBOX' | 'SENT' | 'DRAFTS' | 'TRASH' | 'STARRED' | 'CUSTOM';
  unreadCount: number;
  totalCount: number;
}
```

### 2.2 Get Emails in Mailbox

**Endpoint:** `GET /api/mailboxes/{mailboxId}/emails`

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | number | 0 | Page number (0-indexed) |
| size | number | 20 | Emails per page |
| pageToken | string | - | Gmail page token for pagination |

```typescript
interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  nextPageToken?: string;
}

interface EmailListResponse {
  id: string;
  from: string;
  fromName?: string;
  subject: string;
  preview: string;
  isRead: boolean;
  isStarred: boolean;
  hasAttachments: boolean;
  receivedAt: string;  // ISO datetime
}
```

### 2.3 Get Email Detail

**Endpoint:** `GET /api/emails/{emailId}`

```typescript
interface EmailDetailResponse {
  id: string;
  from: string;
  fromName?: string;
  to: string[];
  cc?: string[];
  bcc?: string[];
  subject: string;
  body: string;        // HTML content
  isRead: boolean;
  isStarred: boolean;
  isImportant: boolean;
  attachments: Attachment[];
  receivedAt: string;
  sentAt?: string;
}

interface Attachment {
  id: string;
  filename: string;
  mimeType: string;
  size: number;
}
```

### 2.4 Email Actions (Bulk)

**Endpoint:** `POST /api/emails/actions`

```typescript
interface EmailActionRequest {
  action: 'read' | 'unread' | 'star' | 'unstar' | 'delete' | 'archive';
  emailIds: string[];
}
```

### 2.5 Mark Email Read/Unread

**Endpoint:** `PUT /api/emails/{emailId}/read`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| read | boolean | true = mark read, false = mark unread |

### 2.6 Star/Unstar Email

**Endpoint:** `PUT /api/emails/{emailId}/star`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| starred | boolean | true = star, false = unstar |

### 2.7 Delete Email

**Endpoint:** `DELETE /api/emails/{emailId}`

Moves email to Gmail Trash.

### 2.8 Send Email

**Endpoint:** `POST /api/emails/send`

```typescript
interface SendEmailRequest {
  to: string[];
  cc?: string[];
  bcc?: string[];
  subject: string;
  body: string;  // HTML content
}
```

### 2.9 Reply to Email

**Endpoint:** `POST /api/emails/{emailId}/reply`

```typescript
interface ReplyEmailRequest {
  body: string;       // HTML content
  replyAll?: boolean; // Include all recipients
}
```

### 2.10 Forward Email

**Endpoint:** `POST /api/emails/{emailId}/forward`

```typescript
interface ForwardEmailRequest {
  to: string[];
  cc?: string[];
  bcc?: string[];
  additionalMessage?: string;  // Message to add before forwarded content
}
```

### 2.11 Download Attachment

**Endpoint:** `GET /api/attachments/{messageId}/{attachmentId}`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| filename | string | Suggested filename for download |
| mimeType | string | MIME type for content |

**Example:**
```typescript
const downloadUrl = `/api/attachments/${messageId}/${attachmentId}?filename=${encodeURIComponent(filename)}&mimeType=${encodeURIComponent(mimeType)}`;
window.open(downloadUrl, '_blank');
```

---

## 3. Kanban Board

### 3.1 Get Kanban Board

**Endpoint:** `GET /api/kanban/board`

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| maxEmails | number | 50 | Max emails to load (max: 100) |
| sync | boolean | false | Sync new emails from Gmail first |

```typescript
interface KanbanBoardResponse {
  columns: KanbanColumnResponse[];
  emailsByColumn: Record<string, KanbanEmailResponse[]>;
}

interface KanbanColumnResponse {
  id: string;
  name: string;
  type: 'INBOX' | 'BACKLOG' | 'TODO' | 'IN_PROGRESS' | 'DONE' | 'SNOOZED' | 'CUSTOM';
  order: number;
  color?: string;
  isDefault: boolean;
  emailCount: number;
  gmailLabelId?: string;
  gmailLabelName?: string;
  removeLabelsOnMove?: string[];
  addLabelsOnMove?: string[];
}

interface KanbanEmailResponse {
  id: string;           // Kanban status ID
  emailId: string;      // Gmail message ID
  columnId: string;
  orderInColumn: number;
  subject: string;
  fromEmail: string;
  fromName?: string;
  preview: string;
  receivedAt: string;
  isRead: boolean;
  isStarred: boolean;
  hasAttachments: boolean;
  summary?: string;           // AI-generated summary
  summaryGeneratedAt?: string;
  snoozed: boolean;
  snoozeUntil?: string;
}
```

### 3.2 Get Board with Filters

**Endpoint:** `GET /api/kanban/board/filter`

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| sortBy | string | `date_newest` | `date_newest`, `date_oldest`, `sender_name` |
| unreadOnly | boolean | false | Show only unread emails |
| hasAttachmentsOnly | boolean | false | Show only emails with attachments |
| fromSender | string | - | Filter by sender (partial match) |
| columnId | string | - | Filter by specific column |
| maxEmailsPerColumn | number | 50 | Max emails per column (max: 100) |

### 3.3 Sync Gmail Emails

**Endpoint:** `POST /api/kanban/sync`

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| maxEmails | number | 50 | Max emails to sync (max: 100) |

```typescript
interface KanbanSyncResult {
  synced: number;
  skipped: number;
  total: number;
  message: string;
}
```

### 3.4 Check Gmail Connection

**Endpoint:** `GET /api/kanban/gmail-status`

```typescript
interface GmailStatusResponse {
  connected: boolean;
}
```

### 3.5 Get Gmail Labels

**Endpoint:** `GET /api/kanban/gmail-labels`

```typescript
interface GmailLabelResponse {
  id: string;
  name: string;
  type: string;
  messageListVisibility?: string;
  labelListVisibility?: string;
}
```

### 3.6 Column Operations

#### Get Columns
**Endpoint:** `GET /api/kanban/columns`

#### Create Column
**Endpoint:** `POST /api/kanban/columns`

```typescript
interface CreateColumnRequest {
  name: string;
  color?: string;
  gmailLabelId?: string;
  addLabelsOnMove?: string[];
  removeLabelsOnMove?: string[];
}
```

#### Update Column
**Endpoint:** `PUT /api/kanban/columns/{columnId}`

```typescript
interface UpdateColumnRequest {
  name?: string;
  color?: string;
  order?: number;
  gmailLabelId?: string;
  addLabelsOnMove?: string[];
  removeLabelsOnMove?: string[];
}
```

#### Delete Column
**Endpoint:** `DELETE /api/kanban/columns/{columnId}`

### 3.7 Move Email (Drag & Drop)

**Endpoint:** `POST /api/kanban/emails/move`

```typescript
interface MoveEmailRequest {
  emailId: string;
  targetColumnId: string;
  newOrder?: number;
}
```

### 3.8 Snooze Email

**Endpoint:** `POST /api/kanban/emails/snooze`

```typescript
interface SnoozeEmailRequest {
  emailId: string;
  snoozeUntil: string;  // ISO datetime
}
```

**Preset Snooze Times:**
```typescript
// Tomorrow 9 AM
const tomorrow = new Date();
tomorrow.setDate(tomorrow.getDate() + 1);
tomorrow.setHours(9, 0, 0, 0);

// Next Week Monday 9 AM
const nextWeek = new Date();
nextWeek.setDate(nextWeek.getDate() + (8 - nextWeek.getDay()) % 7);
nextWeek.setHours(9, 0, 0, 0);
```

### 3.9 Unsnooze Email

**Endpoint:** `POST /api/kanban/emails/{emailId}/unsnooze`

### 3.10 Generate AI Summary

**Endpoint:** `POST /api/kanban/emails/{emailId}/summarize`

---

## 4. Search Features

### 4.1 Fuzzy Search

**Endpoint:** `GET /api/kanban/search`

Searches with typo tolerance and partial matching.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| query | string | required | Search query |
| limit | number | 20 | Max results (max: 100) |
| includeBody | boolean | false | Also search in preview/summary |

```typescript
interface FuzzySearchResponse {
  query: string;
  totalResults: number;
  results: SearchResultItem[];
}

interface SearchResultItem {
  id: string;
  emailId: string;
  columnId: string;
  columnName: string;
  subject: string;
  fromEmail: string;
  fromName?: string;
  preview: string;
  summary?: string;
  receivedAt: string;
  isRead: boolean;
  isStarred: boolean;
  hasAttachments: boolean;
  score: number;           // Relevance score (higher = better)
  matchedFields: string[]; // e.g., ["subject", "fromName"]
}
```

**Example:**
```typescript
// Typo tolerance: "marketng" finds "marketing"
// Partial match: "Nguy" finds "Nguyen Van A"
const response = await fetch(`/api/kanban/search?query=${encodeURIComponent('marketng')}&limit=20`);
```

### 4.2 Semantic Search

**Endpoint:** `POST /api/search/semantic`

AI-powered conceptual search using embeddings.

```typescript
interface SemanticSearchRequest {
  query: string;
  limit?: number;                    // default: 20, max: 100
  minScore?: number;                 // default: 0.5
  generateMissingEmbeddings?: boolean;
}

interface SemanticSearchResponse {
  query: string;
  totalResults: number;
  results: SemanticSearchResultItem[];
  emailsWithEmbeddings: number;
  emailsWithoutEmbeddings: number;
  processingTimeMs: number;
}

interface SemanticSearchResultItem {
  id: string;
  emailId: string;
  columnId: string;
  columnName: string;
  subject: string;
  fromEmail: string;
  fromName?: string;
  preview: string;
  summary?: string;
  receivedAt: string;
  isRead: boolean;
  isStarred: boolean;
  hasAttachments: boolean;
  score: number;  // Similarity score (0-1)
}
```

**Example:**
```typescript
// "money" finds emails about "invoice", "price", "salary"
const response = await fetch('/api/search/semantic', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${accessToken}`
  },
  body: JSON.stringify({ query: 'money', limit: 20 })
});
```

### 4.3 Check Semantic Search Status

**Endpoint:** `GET /api/search/semantic/status`

```typescript
interface SemanticSearchStatusResponse {
  available: boolean;
  message: string;
}
```

### 4.4 Generate Embeddings

**Endpoint:** `POST /api/search/semantic/generate-embeddings`

Generates embeddings for all emails without them.

### 4.5 Search Suggestions (Type-ahead)

**Endpoint:** `GET /api/search/suggestions`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| query | string | Partial search query |

```typescript
interface SearchSuggestionResponse {
  query: string;
  contacts: ContactSuggestion[];
  keywords: KeywordSuggestion[];
  recentSearches: string[];
}

interface ContactSuggestion {
  email: string;
  name?: string;
  emailCount: number;
}

interface KeywordSuggestion {
  keyword: string;
  count: number;
}
```

### 4.6 Get All Contacts

**Endpoint:** `GET /api/search/contacts`

Returns all unique senders for autocomplete.

---

## 5. Refreshing Emails

Use these approaches to keep emails updated:

### 5.1 Manual Sync (Recommended)

**Endpoint:** `POST /api/kanban/sync`

Fetches new emails from Gmail and adds them to the Kanban board.

```typescript
// Sync new emails (call on page load or with a refresh button)
const { data } = await api.post('/kanban/sync?maxEmails=50');
console.log(`Synced ${data.synced} new emails`);
```

### 5.2 Sync on Board Load

**Endpoint:** `GET /api/kanban/board?sync=true`

Load the board and sync new emails in one request.

```typescript
// Load board with automatic sync
const { data: board } = await api.get('/kanban/board?sync=true&maxEmails=50');
```

### 5.3 Polling (Optional)

For near real-time updates, implement client-side polling:

```typescript
// Poll for new emails every 30 seconds
const POLL_INTERVAL = 30000;

useEffect(() => {
  const interval = setInterval(async () => {
    const { data } = await api.post('/kanban/sync?maxEmails=20');
    if (data.synced > 0) {
      // Refresh the board state
      refetchBoard();
    }
  }, POLL_INTERVAL);

  return () => clearInterval(interval);
}, []);
```

---

## 6. TypeScript Interfaces

Complete TypeScript interfaces for all DTOs:

```typescript
// ============ Common ============
interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
  error?: string;
}

interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  nextPageToken?: string;
}

// ============ Auth ============
interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserInfo;
}

interface UserInfo {
  id: string;
  email: string;
  name: string;
  profilePicture?: string;
}

// ============ Email ============
interface EmailListResponse {
  id: string;
  from: string;
  fromName?: string;
  subject: string;
  preview: string;
  isRead: boolean;
  isStarred: boolean;
  hasAttachments: boolean;
  receivedAt: string;
}

interface EmailDetailResponse {
  id: string;
  from: string;
  fromName?: string;
  to: string[];
  cc?: string[];
  bcc?: string[];
  subject: string;
  body: string;
  isRead: boolean;
  isStarred: boolean;
  isImportant: boolean;
  attachments: Attachment[];
  receivedAt: string;
  sentAt?: string;
}

interface Attachment {
  id: string;
  filename: string;
  mimeType: string;
  size: number;
}

interface MailboxResponse {
  id: string;
  name: string;
  type: MailboxType;
  unreadCount: number;
  totalCount: number;
}

type MailboxType = 'INBOX' | 'SENT' | 'DRAFTS' | 'TRASH' | 'STARRED' | 'CUSTOM';

// ============ Kanban ============
interface KanbanBoardResponse {
  columns: KanbanColumnResponse[];
  emailsByColumn: Record<string, KanbanEmailResponse[]>;
}

interface KanbanColumnResponse {
  id: string;
  name: string;
  type: ColumnType;
  order: number;
  color?: string;
  isDefault: boolean;
  emailCount: number;
  gmailLabelId?: string;
  gmailLabelName?: string;
  removeLabelsOnMove?: string[];
  addLabelsOnMove?: string[];
  createdAt: string;
  updatedAt: string;
}

type ColumnType = 'INBOX' | 'BACKLOG' | 'TODO' | 'IN_PROGRESS' | 'DONE' | 'SNOOZED' | 'CUSTOM';

interface KanbanEmailResponse {
  id: string;
  emailId: string;
  columnId: string;
  orderInColumn: number;
  subject: string;
  fromEmail: string;
  fromName?: string;
  preview: string;
  receivedAt: string;
  isRead: boolean;
  isStarred: boolean;
  hasAttachments: boolean;
  summary?: string;
  summaryGeneratedAt?: string;
  snoozed: boolean;
  snoozeUntil?: string;
  createdAt: string;
  updatedAt: string;
}

interface KanbanSyncResult {
  synced: number;
  skipped: number;
  total: number;
  message: string;
}

// ============ Search ============
interface FuzzySearchResponse {
  query: string;
  totalResults: number;
  results: SearchResultItem[];
}

interface SearchResultItem {
  id: string;
  emailId: string;
  columnId: string;
  columnName: string;
  subject: string;
  fromEmail: string;
  fromName?: string;
  preview: string;
  summary?: string;
  receivedAt: string;
  isRead: boolean;
  isStarred: boolean;
  hasAttachments: boolean;
  score: number;
  matchedFields: string[];
}

interface SemanticSearchRequest {
  query: string;
  limit?: number;
  minScore?: number;
  generateMissingEmbeddings?: boolean;
}

interface SemanticSearchResponse {
  query: string;
  totalResults: number;
  results: SemanticSearchResultItem[];
  emailsWithEmbeddings: number;
  emailsWithoutEmbeddings: number;
  processingTimeMs: number;
}

interface SemanticSearchResultItem {
  id: string;
  emailId: string;
  columnId: string;
  columnName: string;
  subject: string;
  fromEmail: string;
  fromName?: string;
  preview: string;
  summary?: string;
  receivedAt: string;
  isRead: boolean;
  isStarred: boolean;
  hasAttachments: boolean;
  score: number;
}

interface SearchSuggestionResponse {
  query: string;
  contacts: ContactSuggestion[];
  keywords: KeywordSuggestion[];
  recentSearches: string[];
}

interface ContactSuggestion {
  email: string;
  name?: string;
  emailCount: number;
}

interface KeywordSuggestion {
  keyword: string;
  count: number;
}
```

---

## Quick Start Example

```typescript
// 1. Login with Google
const authResponse = await fetch('/api/auth/google', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include',
  body: JSON.stringify({ code: googleAuthCode })
});
const { data: auth } = await authResponse.json();
const accessToken = auth.accessToken;

// 2. Create axios instance with auth
const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: { 'Authorization': `Bearer ${accessToken}` },
  withCredentials: true
});

// 3. Load Kanban board with sync
const { data: board } = await api.get('/kanban/board?sync=true');

// 4. Move email to different column (drag & drop)
await api.post('/kanban/emails/move', {
  emailId: 'gmail-message-id',
  targetColumnId: 'todo-column-id',
  newOrder: 0
});

// 5. Search emails
const { data: searchResults } = await api.get('/kanban/search', {
  params: { query: 'meeting', limit: 20 }
});

// 6. Snooze email until tomorrow
const tomorrow = new Date();
tomorrow.setDate(tomorrow.getDate() + 1);
tomorrow.setHours(9, 0, 0, 0);

await api.post('/kanban/emails/snooze', {
  emailId: 'gmail-message-id',
  snoozeUntil: tomorrow.toISOString()
});
```

---

## Error Handling

```typescript
interface ErrorResponse {
  success: false;
  message: string;
  error: string;
  data: null;
}

// Common HTTP status codes:
// 400 - Bad Request (validation error)
// 401 - Unauthorized (invalid/expired token)
// 403 - Forbidden (no permission)
// 404 - Not Found
// 500 - Internal Server Error

// Handle 401 by refreshing token
api.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401) {
      const refreshResponse = await fetch('/api/auth/refresh', {
        method: 'POST',
        credentials: 'include'
      });
      if (refreshResponse.ok) {
        const { data } = await refreshResponse.json();
        // Update token and retry
        error.config.headers['Authorization'] = `Bearer ${data.accessToken}`;
        return api.request(error.config);
      }
      // Redirect to login
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

