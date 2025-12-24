# Search Features - Frontend Integration Guide

This document covers all search functionality: Fuzzy Search, Semantic Search (AI-powered), and Auto-Suggestions.

## Table of Contents
- [1. Fuzzy Search](#1-fuzzy-search)
- [2. Semantic Search (AI-Powered)](#2-semantic-search-ai-powered)
- [3. Auto-Suggestions](#3-auto-suggestions)
- [4. Combined Search UI](#4-combined-search-ui)
- [5. When to Use Each Search Type](#5-when-to-use-each-search-type)
- [6. Error Handling](#6-error-handling)
- [7. CSS Styles](#7-css-styles-example)

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

## 2. Semantic Search (AI-Powered)

AI-powered conceptual search using OpenAI vector embeddings. Finds **conceptually related** emails even without exact keyword matches.

### How It Works

1. **Embeddings**: Each email is converted to a 1536-dimensional vector using OpenAI's `text-embedding-3-small` model
2. **Query Embedding**: Your search query is also converted to a vector
3. **Cosine Similarity**: The system calculates similarity between query and email vectors
4. **Ranking**: Results are ranked by similarity score (0.0 to 1.0)

### Example Use Cases

| Query | Finds emails about... |
|-------|----------------------|
| "money" | invoices, payments, salary, pricing, budget |
| "meeting" | appointments, schedules, calendar, calls |
| "urgent" | deadlines, ASAP, important, priority |

### Check Availability

Before using semantic search, check if the AI service is available:

```
GET /api/search/semantic/status
```

**Response:**
```json
{
  "success": true,
  "data": {
    "available": true,
    "message": "Semantic search is available"
  }
}
```

### Generate Embeddings (Required First Time)

Before semantic search works, you need to generate embeddings for existing emails:

```
POST /api/search/semantic/generate-embeddings
Authorization: Bearer {accessToken}
```

**Response:**
```json
{
  "success": true,
  "message": "Generated embeddings for 50 emails",
  "data": {
    "generated": 50,
    "message": "Generated embeddings for 50 emails"
  }
}
```

### Perform Semantic Search

```
POST /api/search/semantic
Content-Type: application/json
Authorization: Bearer {accessToken}
```

### Request Body

```json
{
  "query": "marketing",
  "limit": 20,
  "minScore": 0.2,
  "generateMissingEmbeddings": true
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `query` | string | required | Natural language query |
| `limit` | number | 20 | Max results (1-100) |
| `minScore` | number | **0.2** | Minimum similarity score (0.0-1.0) |
| `generateMissingEmbeddings` | boolean | false | Auto-generate embeddings for emails without them |

### ⚠️ Important: Understanding `minScore`

OpenAI embeddings typically produce similarity scores in the **0.15 - 0.40 range** for related content:

| minScore | Use Case | Description |
|----------|----------|-------------|
| **0.15** | Broad | Include loosely related results |
| **0.2** | Recommended | Good balance of relevance |
| **0.25** | Strict | Only highly relevant results |
| **0.3+** | Very Strict | May return few/no results |

**Example**: Searching "marketing" against an email with subject "Marketing Intern" typically scores ~0.28

### Response

```json
{
  "success": true,
  "data": {
    "query": "marketing",
    "totalResults": 13,
    "results": [
      {
        "emailId": "19b15a1a7649d40f",
        "subject": "[QC] E-Commerce Marketing Intern - Thực Tập Sinh Marketing",
        "fromEmail": "jobs@company.com",
        "fromName": "HR Team",
        "preview": "We are looking for a marketing intern...",
        "columnId": "col_inbox",
        "columnName": "Inbox",
        "receivedAt": "2024-12-15T10:30:00",
        "read": false,
        "starred": false,
        "hasAttachments": false,
        "similarityScore": 0.2837,
        "summary": null
      }
    ],
    "emailsWithEmbeddings": 50,
    "emailsWithoutEmbeddings": 0,
    "processingTimeMs": 2500
  }
}
```

### Response Fields Explained

| Field | Description |
|-------|-------------|
| `similarityScore` | How similar the email is to your query (0.0-1.0) |
| `emailsWithEmbeddings` | Number of emails that have embeddings |
| `emailsWithoutEmbeddings` | Number of emails missing embeddings |
| `processingTimeMs` | Time taken to process the search |

### Frontend Implementation

```tsx
interface SemanticSearchResult {
  emailId: string;
  subject: string;
  fromEmail: string;
  fromName: string;
  preview: string;
  columnId: string;
  columnName: string;
  receivedAt: string;
  read: boolean;
  starred: boolean;
  hasAttachments: boolean;
  similarityScore: number;
  summary: string | null;
}

interface SemanticSearchResponse {
  query: string;
  totalResults: number;
  results: SemanticSearchResult[];
  emailsWithEmbeddings: number;
  emailsWithoutEmbeddings: number;
  processingTimeMs: number;
}

// Check if semantic search is available
const checkSemanticSearchStatus = async (): Promise<boolean> => {
  try {
    const response = await fetch('/api/search/semantic/status', {
      headers: { 'Authorization': `Bearer ${accessToken}` }
    });
    const data = await response.json();
    return data.data?.available ?? false;
  } catch {
    return false;
  }
};

// Generate embeddings for all emails (call once or when new emails arrive)
const generateEmbeddings = async (): Promise<number> => {
  const response = await fetch('/api/search/semantic/generate-embeddings', {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${accessToken}` }
  });
  const data = await response.json();
  return data.data?.generated ?? 0;
};

// Perform semantic search
const semanticSearch = async (
  query: string,
  minScore = 0.2,
  generateMissing = false
): Promise<SemanticSearchResponse> => {
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
      generateMissingEmbeddings: generateMissing
    })
  });

  const data = await response.json();
  return data.data;
};
```

### Initialization Flow

```tsx
// On app startup or user login
const initializeSemanticSearch = async () => {
  // 1. Check if AI service is available
  const isAvailable = await checkSemanticSearchStatus();
  if (!isAvailable) {
    console.warn('Semantic search not available - AI service not running');
    return false;
  }

  // 2. Generate embeddings for emails that don't have them
  const generated = await generateEmbeddings();
  console.log(`Generated embeddings for ${generated} emails`);

  return true;
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

### Get All Contacts

Get all unique contacts (senders) for address book or autocomplete:

```
GET /api/search/contacts
Authorization: Bearer {accessToken}
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "email": "john@company.com",
      "name": "John Smith",
      "emailCount": 25
    },
    {
      "email": "jane@example.com",
      "name": "Jane Doe",
      "emailCount": 12
    }
  ]
}
```

---

## 5. When to Use Each Search Type

| Search Type | Best For | Example Queries | Speed |
|-------------|----------|-----------------|-------|
| **Fuzzy Search** | Exact/partial text matches, typo tolerance | "markting" (typo), "John", "invoice 1234" | ⚡ Fast |
| **Semantic Search** | Conceptual queries, finding related content | "money problems", "project deadlines", "team updates" | 🐢 Slower (AI processing) |

### Key Differences

| Feature | Fuzzy Search | Semantic Search |
|---------|--------------|-----------------|
| **Matching** | Text-based (keywords) | Concept-based (meaning) |
| **Typo tolerance** | ✅ Yes | ❌ No |
| **Finds related concepts** | ❌ No | ✅ Yes |
| **Speed** | ~50-200ms | ~1-5 seconds |
| **Requires AI service** | ❌ No | ✅ Yes |
| **Score range** | 0.0 - 1.0 | 0.15 - 0.40 (typical) |

### Recommended UX Flow

1. **Default to Fuzzy Search** - Faster, works for most queries
2. **Show AI toggle** - Let users switch to semantic when needed
3. **Auto-suggest while typing** - Help users find what they want faster
4. **Show similarity scores** - Help users understand relevance
5. **Initialize embeddings on login** - Call `generate-embeddings` once

### Recommended minScore Values

```tsx
// For semantic search
const MIN_SCORE_STRICT = 0.25;    // Only highly relevant
const MIN_SCORE_BALANCED = 0.2;   // Recommended default
const MIN_SCORE_BROAD = 0.15;     // Include loosely related
```

---

## 6. Error Handling

```tsx
const handleSearchError = (error: any) => {
  if (error.message?.includes('Semantic search is not available')) {
    // AI service not running or not configured
    toast.error('AI search is not available. Please use text search.');
    setMode('fuzzy');
  } else if (error.message?.includes('AI service not configured')) {
    toast.error('AI service is not configured. Contact administrator.');
    setMode('fuzzy');
  } else if (error.message?.includes('Gmail not connected')) {
    toast.error('Please connect your Gmail account first.');
  } else {
    toast.error('Search failed. Please try again.');
  }
};

// Check for missing embeddings and prompt user
const checkEmbeddingsStatus = (response: SemanticSearchResponse) => {
  if (response.emailsWithoutEmbeddings > 0) {
    toast.info(
      `${response.emailsWithoutEmbeddings} emails don't have embeddings. ` +
      `Click "Generate Embeddings" to enable full semantic search.`
    );
  }
};
```

---

## 7. API Endpoints Summary

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/kanban/search` | GET | Fuzzy text search |
| `/api/search/semantic/status` | GET | Check if AI search is available |
| `/api/search/semantic` | POST | AI-powered semantic search |
| `/api/search/semantic/generate-embeddings` | POST | Generate embeddings for all emails |
| `/api/search/semantic/generate-embedding/{emailId}` | POST | Generate embedding for single email |
| `/api/search/suggestions` | GET | Get search suggestions (type-ahead) |
| `/api/search/contacts` | GET | Get all contacts |

---

## 8. CSS Styles (Example)

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

