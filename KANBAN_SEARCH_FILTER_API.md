# Kanban Board - Search, Filter & Sort API Documentation

This document describes the new API endpoints for fuzzy search, filtering, and sorting functionality on the Kanban board.

## Table of Contents
- [Fuzzy Search](#fuzzy-search)
- [Filtered Board](#filtered-board)
- [Data Models](#data-models)

---

## Fuzzy Search

Search for emails on the Kanban board with typo tolerance and partial matching.

### GET `/api/kanban/search`

Search using query parameters.

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `query` | string | Yes | - | Search query (supports typos and partial matches) |
| `limit` | integer | No | 20 | Max results (1-100) |
| `includeBody` | boolean | No | false | Also search in preview/summary |

**Example Request:**
```
GET /api/kanban/search?query=marketing&limit=10&includeBody=false
```

**Example Response:**
```json
{
  "success": true,
  "message": null,
  "data": {
    "query": "marketing",
    "totalResults": 3,
    "results": [
      {
        "id": "status123",
        "emailId": "gmail_msg_id",
        "columnId": "col_inbox",
        "columnName": "Inbox",
        "subject": "Q4 Marketing Campaign Update",
        "fromEmail": "john@company.com",
        "fromName": "John Smith",
        "preview": "Here's the latest update on our marketing...",
        "summary": "AI-generated summary...",
        "receivedAt": "2024-12-15T10:30:00",
        "isRead": false,
        "isStarred": true,
        "hasAttachments": true,
        "score": 0.95,
        "matchedFields": ["subject", "preview"]
      }
    ]
  }
}
```

### POST `/api/kanban/search`

Search using request body (for complex queries).

**Request Body:**
```json
{
  "query": "markting",
  "limit": 20,
  "includeBody": true
}
```

**Response:** Same as GET endpoint.

---

## Filtered Board

Get the Kanban board with filtering and sorting options.

### GET `/api/kanban/board/filter`

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `sortBy` | string | No | `date_newest` | Sort order: `date_newest`, `date_oldest`, `sender_name` |
| `unreadOnly` | boolean | No | false | Show only unread emails |
| `hasAttachmentsOnly` | boolean | No | false | Show only emails with attachments |
| `fromSender` | string | No | - | Filter by sender (partial match on email or name) |
| `columnId` | string | No | - | Filter by specific column |
| `maxEmailsPerColumn` | integer | No | 50 | Max emails per column (1-100) |

**Example Requests:**

1. **Sort by oldest first:**
```
GET /api/kanban/board/filter?sortBy=date_oldest
```

2. **Show only unread emails:**
```
GET /api/kanban/board/filter?unreadOnly=true
```

3. **Filter by sender:**
```
GET /api/kanban/board/filter?fromSender=john@company.com
```

4. **Combined filters:**
```
GET /api/kanban/board/filter?sortBy=date_newest&unreadOnly=true&hasAttachmentsOnly=true
```

**Example Response:**
```json
{
  "success": true,
  "message": null,
  "data": {
    "columns": [
      {
        "id": "col_inbox",
        "name": "Inbox",
        "type": "INBOX",
        "order": 0,
        "color": "#3B82F6",
        "isDefault": true
      }
    ],
    "emailsByColumn": {
      "col_inbox": [
        {
          "id": "status123",
          "emailId": "gmail_msg_id",
          "columnId": "col_inbox",
          "orderInColumn": 0,
          "subject": "Important Meeting",
          "fromEmail": "john@company.com",
          "fromName": "John Smith",
          "preview": "Please join us for...",
          "receivedAt": "2024-12-15T10:30:00",
          "isRead": false,
          "isStarred": false,
          "hasAttachments": true,
          "summary": null,
          "snoozed": false
        }
      ]
    }
  }
}
```

---

## Data Models

### SearchResultItem
| Field | Type | Description |
|-------|------|-------------|
| `id` | string | EmailKanbanStatus ID |
| `emailId` | string | Gmail message ID |
| `columnId` | string | Current column ID |
| `columnName` | string | Current column name |
| `subject` | string | Email subject |
| `fromEmail` | string | Sender email address |
| `fromName` | string | Sender display name |
| `preview` | string | Email preview text |
| `summary` | string | AI-generated summary (if available) |
| `receivedAt` | datetime | When email was received |
| `isRead` | boolean | Read status |
| `isStarred` | boolean | Starred status |
| `hasAttachments` | boolean | Has attachments |
| `score` | number | Relevance score (0-1.5, higher = better match) |
| `matchedFields` | string[] | Fields that matched: `subject`, `fromName`, `fromEmail`, `preview`, `summary` |

### KanbanEmailResponse (Updated)
| Field | Type | Description |
|-------|------|-------------|
| `id` | string | EmailKanbanStatus ID |
| `emailId` | string | Gmail message ID |
| `columnId` | string | Current column ID |
| `orderInColumn` | integer | Position in column |
| `subject` | string | Email subject |
| `fromEmail` | string | Sender email address |
| `fromName` | string | Sender display name |
| `preview` | string | Email preview text |
| `receivedAt` | datetime | When email was received |
| `isRead` | boolean | Read status |
| `isStarred` | boolean | Starred status |
| `hasAttachments` | boolean | **NEW** - Has attachments |
| `summary` | string | AI-generated summary |
| `summaryGeneratedAt` | datetime | When summary was generated |
| `snoozed` | boolean | Is email snoozed |
| `snoozeUntil` | datetime | Snooze end time |
| `createdAt` | datetime | When added to Kanban |
| `updatedAt` | datetime | Last update time |

---

## Search Algorithm Details

The fuzzy search uses multiple techniques for best results:

### 1. Exact Match (Score: 1.0)
Query exactly matches the text.

### 2. Contains Match (Score: 0.9)
Text contains the exact query string.

### 3. Prefix Match (Score: 0.85)
Query is a prefix of any word in the text.
- Example: "mark" matches "marketing"

### 4. N-gram Similarity
Compares character sequences for partial matches.
- Example: "markting" matches "marketing"

### 5. Levenshtein Distance (Typo Tolerance)
Allows up to 2 character differences for words ≥3 characters.
- Example: "marketng" matches "marketing"

### Field Weights
| Field | Weight | Description |
|-------|--------|-------------|
| Subject | 1.5x | Highest priority |
| Sender Name | 1.3x | High priority |
| Sender Email | 1.2x | Medium-high priority |
| Summary | 0.9x | Medium priority (if includeBody=true) |
| Preview | 0.8x | Lower priority (if includeBody=true) |

---

## Frontend Integration Examples

### React/TypeScript Example

```typescript
// Types
interface SearchResult {
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

interface SearchResponse {
  query: string;
  totalResults: number;
  results: SearchResult[];
}

// Fuzzy Search Hook
const useKanbanSearch = () => {
  const [results, setResults] = useState<SearchResult[]>([]);
  const [loading, setLoading] = useState(false);

  const search = async (query: string, includeBody = false) => {
    if (!query.trim()) {
      setResults([]);
      return;
    }

    setLoading(true);
    try {
      const params = new URLSearchParams({
        query,
        limit: '20',
        includeBody: String(includeBody)
      });

      const response = await fetch(`/api/kanban/search?${params}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });

      const data = await response.json();
      setResults(data.data.results);
    } finally {
      setLoading(false);
    }
  };

  return { results, loading, search };
};

// Filter Hook
const useKanbanFilter = () => {
  const [board, setBoard] = useState(null);

  const fetchFiltered = async (filters: {
    sortBy?: 'date_newest' | 'date_oldest' | 'sender_name';
    unreadOnly?: boolean;
    hasAttachmentsOnly?: boolean;
    fromSender?: string;
  }) => {
    const params = new URLSearchParams();
    if (filters.sortBy) params.set('sortBy', filters.sortBy);
    if (filters.unreadOnly) params.set('unreadOnly', 'true');
    if (filters.hasAttachmentsOnly) params.set('hasAttachmentsOnly', 'true');
    if (filters.fromSender) params.set('fromSender', filters.fromSender);

    const response = await fetch(`/api/kanban/board/filter?${params}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    const data = await response.json();
    setBoard(data.data);
  };

  return { board, fetchFiltered };
};
```

### Debounced Search Input

```typescript
// Debounce search for better UX
const SearchInput = () => {
  const [query, setQuery] = useState('');
  const { results, loading, search } = useKanbanSearch();

  useEffect(() => {
    const timer = setTimeout(() => {
      search(query);
    }, 300); // 300ms debounce

    return () => clearTimeout(timer);
  }, [query]);

  return (
    <div>
      <input
        type="text"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder="Search emails..."
      />
      {loading && <Spinner />}
      <SearchResults results={results} />
    </div>
  );
};
```

---

## Error Handling

All endpoints return standard API response format:

**Success:**
```json
{
  "success": true,
  "message": null,
  "data": { ... }
}
```

**Error:**
```json
{
  "success": false,
  "message": "Error description",
  "data": null
}
```

Common HTTP status codes:
- `200` - Success
- `400` - Bad request (invalid parameters)
- `401` - Unauthorized (missing/invalid token)
- `500` - Server error

