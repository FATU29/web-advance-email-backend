# Search Features - Frontend Integration Guide

This document covers all search functionality: Fuzzy Search, Semantic Search, and Auto-Suggestions.

## Table of Contents
- [1. Fuzzy Search](#1-fuzzy-search)
- [2. Semantic Search](#2-semantic-search)
- [3. Auto-Suggestions](#3-auto-suggestions)
- [4. Combined Search UI](#4-combined-search-ui)

---

## 1. Fuzzy Search

Fast text-based search with typo tolerance and partial matching.

### Endpoint

```
GET /api/kanban/search?query={query}&limit={limit}&includeBody={boolean}
```

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `query` | string | required | Search query (supports typos) |
| `limit` | number | 20 | Max results (1-100) |
| `includeBody` | boolean | false | Also search in preview/summary |

### Response

```json
{
  "success": true,
  "data": {
    "query": "marketing",
    "totalResults": 5,
    "results": [
      {
        "id": "status_123",
        "emailId": "gmail_msg_id",
        "columnId": "col_inbox",
        "columnName": "Inbox",
        "subject": "Q4 Marketing Campaign",
        "fromEmail": "john@company.com",
        "fromName": "John Smith",
        "preview": "Here's the latest update...",
        "summary": "AI summary...",
        "receivedAt": "2024-12-15T10:30:00",
        "isRead": false,
        "isStarred": true,
        "hasAttachments": true,
        "score": 0.95,
        "matchedFields": ["subject", "fromName"]
      }
    ]
  }
}
```

### Frontend Implementation

```tsx
interface FuzzySearchResult {
  id: string;
  emailId: string;
  columnId: string;
  columnName: string;
  subject: string;
  fromEmail: string;
  fromName: string;
  preview: string;
  summary: string | null;
  receivedAt: string;
  isRead: boolean;
  isStarred: boolean;
  hasAttachments: boolean;
  score: number;
  matchedFields: string[];
}

const fuzzySearch = async (query: string, includeBody = false): Promise<FuzzySearchResult[]> => {
  const params = new URLSearchParams({
    query,
    limit: '20',
    includeBody: String(includeBody)
  });
  
  const response = await fetch(`/api/kanban/search?${params}`, {
    headers: { 'Authorization': `Bearer ${accessToken}` }
  });
  
  const data = await response.json();
  return data.data.results;
};
```

---

## 2. Semantic Search

AI-powered conceptual search using vector embeddings. Finds related emails even without exact keyword matches.

### Endpoint

```
POST /api/search/semantic
Content-Type: application/json
```

### Request Body

```json
{
  "query": "money issues",
  "limit": 20,
  "minScore": 0.7,
  "generateMissingEmbeddings": false
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `query` | string | required | Natural language query |
| `limit` | number | 20 | Max results (1-50) |
| `minScore` | number | 0.7 | Minimum similarity score (0-1) |
| `generateMissingEmbeddings` | boolean | false | Generate embeddings for emails without them |

### Response

```json
{
  "success": true,
  "data": {
    "query": "money issues",
    "totalResults": 3,
    "results": [
      {
        "id": "status_456",
        "emailId": "gmail_msg_id",
        "columnId": "col_todo",
        "columnName": "To Do",
        "subject": "Invoice #1234 - Payment Due",
        "fromEmail": "billing@vendor.com",
        "fromName": "Vendor Billing",
        "preview": "Your invoice is ready...",
        "summary": "Invoice payment reminder",
        "receivedAt": "2024-12-10T09:00:00",
        "isRead": true,
        "isStarred": false,
        "hasAttachments": true,
        "similarityScore": 0.89
      }
    ],
    "emailsWithEmbeddings": 150,
    "emailsWithoutEmbeddings": 10,
    "processingTimeMs": 245
  }
}
```

### Frontend Implementation

```tsx
interface SemanticSearchResult {
  id: string;
  emailId: string;
  columnId: string;
  columnName: string;
  subject: string;
  fromEmail: string;
  fromName: string;
  preview: string;
  summary: string | null;
  receivedAt: string;
  isRead: boolean;
  isStarred: boolean;
  hasAttachments: boolean;
  similarityScore: number;
}

interface SemanticSearchResponse {
  query: string;
  totalResults: number;
  results: SemanticSearchResult[];
  emailsWithEmbeddings: number;
  emailsWithoutEmbeddings: number;
  processingTimeMs: number;
}

const semanticSearch = async (query: string, minScore = 0.7): Promise<SemanticSearchResponse> => {
  const response = await fetch('/api/search/semantic', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      query,
      limit: 20,
      minScore,
      generateMissingEmbeddings: false
    })
  });

  const data = await response.json();
  return data.data;
};
```

---

## 3. Auto-Suggestions

Get search suggestions as the user types.

### Endpoint

```
GET /api/search/suggestions?query={query}&limit={limit}
```

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `query` | string | required | Partial search query |
| `limit` | number | 5 | Max suggestions per category |

### Response

```json
{
  "success": true,
  "data": {
    "query": "john",
    "contacts": [
      {
        "email": "john@company.com",
        "name": "John Smith",
        "emailCount": 25
      },
      {
        "email": "johnny@example.com",
        "name": "Johnny Doe",
        "emailCount": 10
      }
    ],
    "keywords": [
      {
        "keyword": "Johnson Report",
        "frequency": 5
      }
    ],
    "recentSearches": ["john meeting", "john project"]
  }
}
```

### Frontend Implementation

```tsx
interface ContactSuggestion {
  email: string;
  name: string;
  emailCount: number;
}

interface KeywordSuggestion {
  keyword: string;
  frequency: number;
}

interface SuggestionResponse {
  query: string;
  contacts: ContactSuggestion[];
  keywords: KeywordSuggestion[];
  recentSearches: string[];
}

const getSuggestions = async (query: string): Promise<SuggestionResponse> => {
  const params = new URLSearchParams({ query, limit: '5' });

  const response = await fetch(`/api/search/suggestions?${params}`, {
    headers: { 'Authorization': `Bearer ${accessToken}` }
  });

  const data = await response.json();
  return data.data;
};
```

---

## 4. Combined Search UI

### React Component Example

```tsx
import React, { useState, useEffect, useCallback } from 'react';
import debounce from 'lodash/debounce';

type SearchMode = 'fuzzy' | 'semantic';

interface SearchBarProps {
  onResultsChange: (results: any[]) => void;
}

const SearchBar: React.FC<SearchBarProps> = ({ onResultsChange }) => {
  const [query, setQuery] = useState('');
  const [mode, setMode] = useState<SearchMode>('fuzzy');
  const [suggestions, setSuggestions] = useState<SuggestionResponse | null>(null);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [loading, setLoading] = useState(false);

  // Debounced suggestion fetcher
  const fetchSuggestions = useCallback(
    debounce(async (q: string) => {
      if (q.length < 2) {
        setSuggestions(null);
        return;
      }
      const data = await getSuggestions(q);
      setSuggestions(data);
    }, 300),
    []
  );

  useEffect(() => {
    fetchSuggestions(query);
  }, [query, fetchSuggestions]);

  const handleSearch = async () => {
    if (!query.trim()) return;

    setLoading(true);
    setShowSuggestions(false);

    try {
      if (mode === 'fuzzy') {
        const results = await fuzzySearch(query, true);
        onResultsChange(results);
      } else {
        const response = await semanticSearch(query);
        onResultsChange(response.results);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleSuggestionClick = (suggestion: string) => {
    setQuery(suggestion);
    setShowSuggestions(false);
    // Trigger search
    handleSearch();
  };

  return (
    <div className="search-container">
      <div className="search-input-wrapper">
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onFocus={() => setShowSuggestions(true)}
          onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
          placeholder={mode === 'fuzzy' ? 'Search emails...' : 'Search by concept...'}
        />

        {/* Search Mode Toggle */}
        <div className="search-mode-toggle">
          <button
            className={mode === 'fuzzy' ? 'active' : ''}
            onClick={() => setMode('fuzzy')}
            title="Text search with typo tolerance"
          >
            🔤 Text
          </button>
          <button
            className={mode === 'semantic' ? 'active' : ''}
            onClick={() => setMode('semantic')}
            title="AI-powered conceptual search"
          >
            🧠 AI
          </button>
        </div>

        <button onClick={handleSearch} disabled={loading}>
          {loading ? '...' : '🔍'}
        </button>
      </div>

      {/* Suggestions Dropdown */}
      {showSuggestions && suggestions && (
        <div className="suggestions-dropdown">
          {/* Contact Suggestions */}
          {suggestions.contacts.length > 0 && (
            <div className="suggestion-group">
              <div className="group-label">Contacts</div>
              {suggestions.contacts.map((contact) => (
                <div
                  key={contact.email}
                  className="suggestion-item"
                  onClick={() => handleSuggestionClick(`from:${contact.email}`)}
                >
                  <span className="contact-name">{contact.name}</span>
                  <span className="contact-email">{contact.email}</span>
                  <span className="email-count">({contact.emailCount})</span>
                </div>
              ))}
            </div>
          )}

          {/* Keyword Suggestions */}
          {suggestions.keywords.length > 0 && (
            <div className="suggestion-group">
              <div className="group-label">Keywords</div>
              {suggestions.keywords.map((kw) => (
                <div
                  key={kw.keyword}
                  className="suggestion-item"
                  onClick={() => handleSuggestionClick(kw.keyword)}
                >
                  {kw.keyword}
                </div>
              ))}
            </div>
          )}

          {/* Recent Searches */}
          {suggestions.recentSearches.length > 0 && (
            <div className="suggestion-group">
              <div className="group-label">Recent</div>
              {suggestions.recentSearches.map((search) => (
                <div
                  key={search}
                  className="suggestion-item"
                  onClick={() => handleSuggestionClick(search)}
                >
                  🕐 {search}
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
```

---

## 5. When to Use Each Search Type

| Search Type | Best For | Example Queries |
|-------------|----------|-----------------|
| **Fuzzy Search** | Quick text lookups, known keywords, sender names | "markting" (typo), "John", "invoice 1234" |
| **Semantic Search** | Conceptual queries, finding related content | "money problems", "project deadlines", "team updates" |

### Recommended UX Flow

1. **Default to Fuzzy Search** - Faster, works for most queries
2. **Show AI toggle** - Let users switch to semantic when needed
3. **Auto-suggest while typing** - Help users find what they want faster
4. **Highlight matched fields** - Show why each result matched

---

## 6. Error Handling

```tsx
const handleSearchError = (error: any) => {
  if (error.message?.includes('Semantic search is not available')) {
    // OpenAI API key not configured
    toast.error('AI search is not available. Please use text search.');
    setMode('fuzzy');
  } else if (error.message?.includes('Gmail not connected')) {
    toast.error('Please connect your Gmail account first.');
  } else {
    toast.error('Search failed. Please try again.');
  }
};
```

---

## 7. CSS Styles (Example)

```css
.search-container {
  position: relative;
  width: 100%;
  max-width: 600px;
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background: white;
}

.search-input-wrapper input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 14px;
}

.search-mode-toggle {
  display: flex;
  gap: 4px;
}

.search-mode-toggle button {
  padding: 4px 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  font-size: 12px;
}

.search-mode-toggle button.active {
  background: #007bff;
  color: white;
  border-color: #007bff;
}

.suggestions-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  margin-top: 4px;
  max-height: 300px;
  overflow-y: auto;
  z-index: 100;
}

.suggestion-group {
  padding: 8px 0;
  border-bottom: 1px solid #eee;
}

.group-label {
  padding: 4px 12px;
  font-size: 11px;
  color: #666;
  text-transform: uppercase;
}

.suggestion-item {
  padding: 8px 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
}

.suggestion-item:hover {
  background: #f5f5f5;
}

.contact-name {
  font-weight: 500;
}

.contact-email {
  color: #666;
  font-size: 12px;
}

.email-count {
  color: #999;
  font-size: 11px;
  margin-left: auto;
}
```

