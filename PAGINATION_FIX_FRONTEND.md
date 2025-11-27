# Frontend Pagination Fix Guide

## Problem

The email pagination API was returning the same emails for all pages because the frontend was not sending the `pageToken` parameter to the backend. The backend has been updated to support Gmail's token-based pagination, but the frontend needs to be updated to use it correctly.

## Root Cause

The frontend was only incrementing the `page` parameter but not including the `pageToken` from the previous API response:

```javascript
// ❌ WRONG - Backend receives pageToken: null for all requests
fetch(`/api/mailboxes/INBOX/emails?page=${pageNumber}&size=5`)
```

This caused the backend to always fetch the first page from Gmail API.

## Solution Overview

The frontend must:
1. **Capture** the `nextPageToken` from each API response
2. **Store** it for the next request
3. **Include** it as a query parameter when fetching the next page

---

## API Response Structure

The backend now returns pagination data with a `nextPageToken`:

```json
{
  "success": true,
  "data": {
    "content": [...],
    "page": 0,
    "size": 5,
    "totalElements": 201,
    "totalPages": 41,
    "last": false,
    "nextPageToken": "01072320750847702280"
  }
}
```

### Key Fields

- **`nextPageToken`**: Token to fetch the next page (null if no more pages)
- **`last`**: Boolean indicating if this is the last page
- **`totalElements`**: Estimated total number of emails
- **`totalPages`**: Estimated total number of pages

---

## Implementation Options

### Option 1: Simple Forward-Only Pagination

Best for infinite scroll or "Load More" buttons where users only go forward.

```javascript
class EmailPagination {
  constructor() {
    this.currentPage = 0;
    this.pageSize = 20;
    this.nextPageToken = null;
    this.hasMore = true;
  }

  async fetchEmails(mailboxId) {
    // Build URL
    let url = `/api/mailboxes/${mailboxId}/emails?page=${this.currentPage}&size=${this.pageSize}`;
    
    // Add pageToken if available (not for first page)
    if (this.nextPageToken) {
      url += `&pageToken=${encodeURIComponent(this.nextPageToken)}`;
    }

    try {
      const response = await fetch(url, {
        headers: {
          'Authorization': `Bearer ${getAuthToken()}`
        }
      });

      const result = await response.json();
      
      if (result.success) {
        const data = result.data;
        
        // Update pagination state
        this.nextPageToken = data.nextPageToken;
        this.hasMore = !data.last && data.nextPageToken !== null;
        
        return data.content;
      } else {
        throw new Error(result.message || 'Failed to fetch emails');
      }
    } catch (error) {
      console.error('Error fetching emails:', error);
      throw error;
    }
  }

  async nextPage(mailboxId) {
    if (!this.hasMore) {
      console.warn('No more pages available');
      return [];
    }
    
    this.currentPage++;
    return await this.fetchEmails(mailboxId);
  }

  reset() {
    this.currentPage = 0;
    this.nextPageToken = null;
    this.hasMore = true;
  }
}

// Usage
const pagination = new EmailPagination();

// Load first page
const emails = await pagination.fetchEmails('INBOX');
displayEmails(emails);

// Load next page
document.getElementById('loadMoreBtn').addEventListener('click', async () => {
  const moreEmails = await pagination.nextPage('INBOX');
  appendEmails(moreEmails);
  
  // Disable button if no more pages
  if (!pagination.hasMore) {
    document.getElementById('loadMoreBtn').disabled = true;
  }
});
```

---

### Option 2: Bidirectional Pagination with Token History

Best for traditional pagination with Previous/Next buttons.

```javascript
class EmailPaginationWithHistory {
  constructor() {
    this.currentPage = 0;
    this.pageSize = 20;
    this.pageTokens = [null]; // Index 0 = first page (no token)
    this.totalPages = 1;
    this.isLastPage = false;
  }

  async fetchEmails(mailboxId) {
    // Get the token for the current page
    const pageToken = this.pageTokens[this.currentPage];
    
    // Build URL
    let url = `/api/mailboxes/${mailboxId}/emails?page=${this.currentPage}&size=${this.pageSize}`;
    
    if (pageToken) {
      url += `&pageToken=${encodeURIComponent(pageToken)}`;
    }

    try {
      const response = await fetch(url, {
        headers: {
          'Authorization': `Bearer ${getAuthToken()}`
        }
      });

      const result = await response.json();
      
      if (result.success) {
        const data = result.data;
        
        // Store the next page token if we haven't seen it before
        if (data.nextPageToken && !this.pageTokens[this.currentPage + 1]) {
          this.pageTokens[this.currentPage + 1] = data.nextPageToken;
        }
        
        // Update metadata
        this.totalPages = data.totalPages;
        this.isLastPage = data.last;
        
        return {
          emails: data.content,
          currentPage: this.currentPage,
          totalPages: this.totalPages,
          hasNext: !this.isLastPage && data.nextPageToken !== null,
          hasPrevious: this.currentPage > 0
        };
      } else {
        throw new Error(result.message || 'Failed to fetch emails');
      }
    } catch (error) {
      console.error('Error fetching emails:', error);
      throw error;
    }
  }

  async nextPage(mailboxId) {
    if (this.isLastPage || !this.pageTokens[this.currentPage + 1]) {
      console.warn('No next page available');
      return null;
    }
    
    this.currentPage++;
    return await this.fetchEmails(mailboxId);
  }

  async previousPage(mailboxId) {
    if (this.currentPage === 0) {
      console.warn('Already on first page');
      return null;
    }
    
    this.currentPage--;
    return await this.fetchEmails(mailboxId);
  }

  async goToPage(mailboxId, pageNumber) {
    if (pageNumber < 0 || pageNumber >= this.totalPages) {
      console.warn('Invalid page number');
      return null;
    }
    
    // Check if we have the token for this page
    if (pageNumber > 0 && !this.pageTokens[pageNumber]) {
      console.warn('Cannot jump to page - must navigate sequentially');
      return null;
    }
    
    this.currentPage = pageNumber;
    return await this.fetchEmails(mailboxId);
  }

  reset() {
    this.currentPage = 0;
    this.pageTokens = [null];
    this.totalPages = 1;
    this.isLastPage = false;
  }
}

// Usage
const pagination = new EmailPaginationWithHistory();

async function loadEmails() {
  const result = await pagination.fetchEmails('INBOX');
  displayEmails(result.emails);
  updatePaginationUI(result);
}

function updatePaginationUI(result) {
  document.getElementById('currentPage').textContent = result.currentPage + 1;
  document.getElementById('totalPages').textContent = result.totalPages;
  document.getElementById('prevBtn').disabled = !result.hasPrevious;
  document.getElementById('nextBtn').disabled = !result.hasNext;
}

// Event listeners
document.getElementById('prevBtn').addEventListener('click', async () => {
  const result = await pagination.previousPage('INBOX');
  if (result) {
    displayEmails(result.emails);
    updatePaginationUI(result);
  }
});

document.getElementById('nextBtn').addEventListener('click', async () => {
  const result = await pagination.nextPage('INBOX');
  if (result) {
    displayEmails(result.emails);
    updatePaginationUI(result);
  }
});

// Initial load
loadEmails();
```

---

### Option 3: React/Vue Component Example

For modern frameworks:

```javascript
// React Example
import { useState, useEffect } from 'react';

function EmailList({ mailboxId }) {
  const [emails, setEmails] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [nextPageToken, setNextPageToken] = useState(null);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);

  const fetchEmails = async (page, token) => {
    setLoading(true);
    try {
      let url = `/api/mailboxes/${mailboxId}/emails?page=${page}&size=20`;
      if (token) {
        url += `&pageToken=${encodeURIComponent(token)}`;
      }

      const response = await fetch(url, {
        headers: {
          'Authorization': `Bearer ${getAuthToken()}`
        }
      });

      const result = await response.json();
      
      if (result.success) {
        setEmails(result.data.content);
        setNextPageToken(result.data.nextPageToken);
        setHasMore(!result.data.last && result.data.nextPageToken !== null);
      }
    } catch (error) {
      console.error('Error fetching emails:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEmails(0, null);
  }, [mailboxId]);

  const handleNextPage = () => {
    const newPage = currentPage + 1;
    setCurrentPage(newPage);
    fetchEmails(newPage, nextPageToken);
  };

  return (
    <div>
      {loading ? (
        <div>Loading...</div>
      ) : (
        <>
          <EmailListView emails={emails} />
          <button 
            onClick={handleNextPage} 
            disabled={!hasMore || loading}
          >
            Load More
          </button>
        </>
      )}
    </div>
  );
}
```

---

## Important Notes

### 1. Token-Based Pagination Limitations

- **Cannot jump to arbitrary pages**: You must navigate sequentially (page 1 → 2 → 3)
- **Tokens expire**: Gmail tokens may expire after some time
- **Going backwards**: Requires storing token history (Option 2)

### 2. When to Reset Pagination

Reset pagination state when:
- User switches mailboxes
- User applies filters
- User refreshes the email list

```javascript
// When switching mailboxes
function switchMailbox(newMailboxId) {
  pagination.reset();
  loadEmails(newMailboxId);
}
```

### 3. Error Handling

Always handle errors gracefully:

```javascript
try {
  const emails = await pagination.fetchEmails('INBOX');
  displayEmails(emails);
} catch (error) {
  if (error.message.includes('token')) {
    // Token expired - reset and retry
    pagination.reset();
    const emails = await pagination.fetchEmails('INBOX');
    displayEmails(emails);
  } else {
    showErrorMessage('Failed to load emails');
  }
}
```

---

## Testing Checklist

- [ ] First page loads without pageToken
- [ ] Second page loads with pageToken from first response
- [ ] Different emails appear on each page
- [ ] "Load More" button disables when `last: true`
- [ ] Previous button works (if implemented)
- [ ] Pagination resets when switching mailboxes
- [ ] Error handling works for expired tokens
- [ ] Loading states display correctly

---

## API Endpoint Reference

```
GET /api/mailboxes/{mailboxId}/emails
```

### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| page | integer | No | 0 | Current page number (for display) |
| size | integer | No | 20 | Number of emails per page |
| pageToken | string | No | null | Token from previous response for next page |

### Response

```json
{
  "success": true,
  "data": {
    "content": [...],
    "page": 0,
    "size": 20,
    "totalElements": 201,
    "totalPages": 11,
    "last": false,
    "nextPageToken": "token_string_here"
  }
}
```

---

## Summary

1. **Always include `pageToken`** from the previous response when fetching the next page
2. **Store token history** if you need bidirectional navigation
3. **Reset pagination** when changing mailboxes or filters
4. **Check `last` flag** to determine if there are more pages
5. **Handle errors** gracefully, especially token expiration

The backend is now working correctly - you just need to update your frontend to pass the `pageToken` parameter!

