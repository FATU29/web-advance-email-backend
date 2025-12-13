# 🎯 Frontend Integration Guide - Kanban Board APIs

## 📋 Overview

The Kanban Board APIs allow you to organize emails into a visual workflow with columns like **Inbox**, **Backlog**, **To Do**, **In Progress**, **Done**, and **Snoozed**.

### Key Features:
- **Gmail Sync** - Sync emails from Gmail to Kanban board (cached in database for performance)
- **Drag-and-drop** email movement between columns
- **Snooze/Deferral** mechanism to temporarily hide emails
- **AI Summarization** using Google Gemini API
- **Custom columns** creation and management
- **Fast loading** - Emails are cached in database, Gmail API only called when syncing

---

## 📧 Gmail Integration & Default Column Behavior

### How Gmail Sync Works:
1. When a user logs in with Google OAuth, their Gmail account is connected
2. Call `POST /api/kanban/sync` or `GET /api/kanban/board?sync=true` to sync emails from Gmail
3. **New emails from Gmail INBOX are automatically placed in the Kanban BACKLOG column**
4. Emails already in the Kanban board are skipped during sync
5. **Emails are cached in database** - subsequent board loads are fast (no Gmail API calls)

### Default Column for New Emails:
- **BACKLOG** column is the default destination for all synced Gmail emails
- Users can then drag-and-drop emails to other columns (TODO, IN_PROGRESS, DONE)
- The BACKLOG column acts as the entry point for email workflow management

### Performance Optimization:
- **First load**: If no cached emails exist, automatically syncs from Gmail
- **Normal load** (`sync=false`): Fast load from database cache
- **Refresh** (`sync=true`): Fetches new emails from Gmail, stores in cache, then returns

### Recommended Frontend Flow:
```javascript
// 1. Check if Gmail is connected
const statusRes = await fetch('/api/kanban/gmail-status', { headers });
const { data: { connected } } = await statusRes.json();

if (connected) {
  // 2. Get board (first time will auto-sync, subsequent calls use cache)
  // Use sync=true to explicitly fetch new emails from Gmail
  const boardRes = await fetch('/api/kanban/board?sync=false&maxEmails=50', { headers });
  const { data: board } = await boardRes.json();
  // Board contains cached emails, new emails in BACKLOG column
} else {
  // Prompt user to connect Gmail
  window.location.href = '/api/auth/google/authorize';
}
```

---

## 🔐 Authentication

All Kanban APIs require JWT authentication. Include the access token in the `Authorization` header:

```javascript
const headers = {
  'Authorization': `Bearer ${accessToken}`,
  'Content-Type': 'application/json'
};
```

---

## 📌 API Endpoints

### Board Operations

#### 1. Get Full Kanban Board
Returns all columns and their emails in a single request. Uses cached emails from database for fast loading.

```
GET /api/kanban/board?sync=false&maxEmails=50
```

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `sync` | boolean | `false` | If `true`, syncs new Gmail emails to cache before returning the board |
| `maxEmails` | integer | `50` | Maximum emails to display/sync (max: 100) |

**Behavior:**
- **First call (no cached emails)**: Automatically syncs from Gmail
- **`sync=false`**: Fast load from database cache (recommended for normal use)
- **`sync=true`**: Fetches new emails from Gmail, stores in cache, then returns

**Response:**
```json
{
  "success": true,
  "data": {
    "columns": [
      {
        "id": "column_id",
        "name": "Backlog",
        "type": "BACKLOG",
        "order": 1,
        "color": "#9E9E9E",
        "isDefault": true,
        "emailCount": 5
      }
    ],
    "emailsByColumn": {
      "column_id": [
        {
          "id": "status_id",
          "emailId": "gmail_message_id",
          "subject": "Email Subject",
          "fromName": "John Doe",
          "fromEmail": "john@example.com",
          "preview": "Email preview text...",
          "summary": "AI-generated summary",
          "isRead": false,
          "isStarred": true,
          "snoozed": false
        }
      ]
    }
  }
}
```

---

### Gmail Sync Operations

#### 1.1 Sync Gmail Emails to Kanban
Manually sync Gmail INBOX emails to the Kanban board. New emails are placed in the **BACKLOG** column by default and cached in database.

```
POST /api/kanban/sync?maxEmails=50
```

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `maxEmails` | integer | `50` | Maximum emails to sync (max: 100) |

**Response:**
```json
{
  "success": true,
  "message": "Successfully synced 10 emails to Kanban board.",
  "data": {
    "synced": 10,
    "skipped": 5,
    "total": 15,
    "message": "Successfully synced 10 emails to Kanban board."
  }
}
```

#### 1.2 Check Gmail Connection Status
Check if the user has connected their Gmail account.

```
GET /api/kanban/gmail-status
```

**Response:**
```json
{
  "success": true,
  "data": {
    "connected": true
  }
}
```

---

### Column Operations

#### 2. Get All Columns
```
GET /api/kanban/columns
```

#### 3. Create Custom Column
```
POST /api/kanban/columns
```
**Body:**
```json
{
  "name": "Review",
  "color": "#9C27B0",
  "order": 5
}
```

#### 4. Update Column
```
PUT /api/kanban/columns/{columnId}
```
**Body:**
```json
{
  "name": "Updated Name",
  "color": "#FF5722"
}
```

#### 5. Delete Column
Emails in deleted column are automatically moved to Backlog.
```
DELETE /api/kanban/columns/{columnId}
```

#### 6. Get Emails in Column
```
GET /api/kanban/columns/{columnId}/emails
```

---

### Email Operations

#### 7. Add Email to Kanban Board
```
POST /api/kanban/emails
```
**Body:**
```json
{
  "emailId": "gmail_message_id",
  "columnId": null,
  "generateSummary": true
}
```
- `columnId`: Target column ID (null = Backlog)
- `generateSummary`: Generate AI summary on add

#### 8. Get Email Kanban Status
```
GET /api/kanban/emails/{emailId}
```

#### 9. Move Email (Drag & Drop)
```
POST /api/kanban/emails/move
```
**Body:**
```json
{
  "emailId": "gmail_message_id",
  "targetColumnId": "target_column_id",
  "newOrder": 0
}
```

#### 10. Remove Email from Kanban
```
DELETE /api/kanban/emails/{emailId}
```

---

### Snooze Operations

#### 11. Snooze Email
Moves email to Snoozed column until specified time.
```
POST /api/kanban/emails/snooze
```
**Body:**
```json
{
  "emailId": "gmail_message_id",
  "snoozeUntil": "2024-12-10T09:00:00"
}
```

#### 12. Unsnooze Email
Manually restore email to previous column.
```
POST /api/kanban/emails/{emailId}/unsnooze
```

> **Note:** Snoozed emails are automatically restored when the snooze time expires (checked every minute).

---

### AI Summarization

#### 13. Generate AI Summary
```
POST /api/kanban/emails/{emailId}/summarize
```

> **Requires:** `GEMINI_API_KEY` environment variable configured on backend.

---

## 🎨 React Integration Example

### Using react-beautiful-dnd

```jsx
import { useState, useEffect } from 'react';
import { DragDropContext, Droppable, Draggable } from 'react-beautiful-dnd';

const API_URL = 'http://localhost:8080';

function KanbanBoard({ accessToken }) {
  const [board, setBoard] = useState({ columns: [], emailsByColumn: {} });
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [gmailConnected, setGmailConnected] = useState(false);

  const headers = {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/json'
  };

  useEffect(() => {
    checkGmailAndFetchBoard();
  }, []);

  const checkGmailAndFetchBoard = async () => {
    try {
      // Check Gmail connection status
      const statusRes = await fetch(`${API_URL}/api/kanban/gmail-status`, { headers });
      const { data: { connected } } = await statusRes.json();
      setGmailConnected(connected);

      // Fetch board from cache (first time will auto-sync from Gmail)
      // Use sync=false for fast loading, sync=true only when user clicks refresh
      const res = await fetch(`${API_URL}/api/kanban/board?sync=false&maxEmails=50`, { headers });
      const { data } = await res.json();
      setBoard(data);
    } finally {
      setLoading(false);
    }
  };

  const handleManualSync = async () => {
    setSyncing(true);
    try {
      const res = await fetch(`${API_URL}/api/kanban/sync?maxEmails=50`, {
        method: 'POST',
        headers
      });
      const { data } = await res.json();
      alert(`Synced ${data.synced} emails (${data.skipped} skipped)`);
      // Refresh board after sync
      const boardRes = await fetch(`${API_URL}/api/kanban/board`, { headers });
      const { data: boardData } = await boardRes.json();
      setBoard(boardData);
    } finally {
      setSyncing(false);
    }
  };

  const fetchBoard = async () => {
    try {
      const res = await fetch(`${API_URL}/api/kanban/board`, { headers });
      const { data } = await res.json();
      setBoard(data);
    } finally {
      setLoading(false);
    }
  };

  const handleDragEnd = async (result) => {
    if (!result.destination) return;

    const { draggableId, source, destination } = result;

    // Don't allow dropping into Snoozed column via drag
    const targetColumn = board.columns.find(c => c.id === destination.droppableId);
    if (targetColumn?.type === 'SNOOZED') {
      alert('Use the snooze button to snooze emails');
      return;
    }

    // Optimistic update
    const newBoard = { ...board };
    const sourceEmails = [...newBoard.emailsByColumn[source.droppableId]];
    const [movedEmail] = sourceEmails.splice(source.index, 1);
    newBoard.emailsByColumn[source.droppableId] = sourceEmails;

    const destEmails = [...(newBoard.emailsByColumn[destination.droppableId] || [])];
    destEmails.splice(destination.index, 0, { ...movedEmail, columnId: destination.droppableId });
    newBoard.emailsByColumn[destination.droppableId] = destEmails;

    setBoard(newBoard);

    // API call
    await fetch(`${API_URL}/api/kanban/emails/move`, {
      method: 'POST',
      headers,
      body: JSON.stringify({
        emailId: draggableId,
        targetColumnId: destination.droppableId,
        newOrder: destination.index
      })
    });
  };

  const handleSnooze = async (emailId, snoozeUntil) => {
    await fetch(`${API_URL}/api/kanban/emails/snooze`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ emailId, snoozeUntil })
    });
    fetchBoard();
  };

  const handleGenerateSummary = async (emailId) => {
    await fetch(`${API_URL}/api/kanban/emails/${emailId}/summarize`, {
      method: 'POST',
      headers
    });
    fetchBoard();
  };

  if (loading) return <div>Loading...</div>;

  return (
    <div>
      {/* Sync Button */}
      <div style={{ padding: '16px', display: 'flex', gap: '8px', alignItems: 'center' }}>
        {gmailConnected ? (
          <button onClick={handleManualSync} disabled={syncing}>
            {syncing ? '🔄 Syncing...' : '🔄 Sync Gmail'}
          </button>
        ) : (
          <span style={{ color: '#f44336' }}>
            ⚠️ Gmail not connected. <a href="/api/auth/google/authorize">Connect Gmail</a>
          </span>
        )}
      </div>

      <DragDropContext onDragEnd={handleDragEnd}>
        <div className="kanban-board" style={{ display: 'flex', gap: '16px', padding: '16px' }}>
          {board.columns.map(column => (
            <div key={column.id} className="kanban-column" style={{
            minWidth: '300px',
            backgroundColor: '#f4f5f7',
            borderRadius: '8px',
            padding: '8px'
          }}>
            <h3 style={{ color: column.color, margin: '8px' }}>
              {column.name} ({column.emailCount})
            </h3>

            <Droppable droppableId={column.id}>
              {(provided, snapshot) => (
                <div
                  ref={provided.innerRef}
                  {...provided.droppableProps}
                  style={{
                    minHeight: '200px',
                    backgroundColor: snapshot.isDraggingOver ? '#e3e3e3' : 'transparent',
                    borderRadius: '4px',
                    padding: '4px'
                  }}
                >
                  {board.emailsByColumn[column.id]?.map((email, index) => (
                    <Draggable key={email.emailId} draggableId={email.emailId} index={index}>
                      {(provided, snapshot) => (
                        <div
                          ref={provided.innerRef}
                          {...provided.draggableProps}
                          {...provided.dragHandleProps}
                          style={{
                            ...provided.draggableProps.style,
                            backgroundColor: snapshot.isDragging ? '#fff' : '#fff',
                            borderRadius: '4px',
                            padding: '12px',
                            marginBottom: '8px',
                            boxShadow: '0 1px 3px rgba(0,0,0,0.12)'
                          }}
                        >
                          <div style={{ fontWeight: 'bold', marginBottom: '4px' }}>
                            {email.subject}
                          </div>
                          <div style={{ fontSize: '12px', color: '#666' }}>
                            {email.fromName || email.fromEmail}
                          </div>
                          <div style={{ fontSize: '12px', color: '#888', marginTop: '4px' }}>
                            {email.preview?.substring(0, 100)}...
                          </div>

                          {email.summary && (
                            <div style={{
                              fontSize: '11px',
                              backgroundColor: '#e8f5e9',
                              padding: '8px',
                              borderRadius: '4px',
                              marginTop: '8px'
                            }}>
                              <strong>AI Summary:</strong> {email.summary}
                            </div>
                          )}

                          <div style={{ marginTop: '8px', display: 'flex', gap: '4px' }}>
                            {!email.summary && (
                              <button onClick={() => handleGenerateSummary(email.emailId)}>
                                📝 Summarize
                              </button>
                            )}
                            {!email.snoozed && (
                              <button onClick={() => {
                                const date = prompt('Snooze until (YYYY-MM-DDTHH:mm:ss):');
                                if (date) handleSnooze(email.emailId, date);
                              }}>
                                ⏰ Snooze
                              </button>
                            )}
                          </div>
                        </div>
                      )}
                    </Draggable>
                  ))}
                  {provided.placeholder}
                </div>
              )}
            </Droppable>
          </div>
        ))}
        </div>
      </DragDropContext>
    </div>
  );
}

export default KanbanBoard;
```

---

## 📊 Column Types Reference

| Type | Description | Default Color | Deletable |
|------|-------------|---------------|-----------|
| `INBOX` | User's inbox | `#2196F3` (Blue) | No |
| `BACKLOG` | **Default for new synced emails** | `#9E9E9E` (Gray) | No |
| `TODO` | Emails to process | `#FF9800` (Orange) | No |
| `IN_PROGRESS` | Currently working on | `#9C27B0` (Purple) | No |
| `DONE` | Completed | `#4CAF50` (Green) | No |
| `SNOOZED` | Temporarily hidden | `#607D8B` (Blue Gray) | No |
| `CUSTOM` | User-created columns | User-defined | Yes |

---

## 📝 TypeScript Interfaces

```typescript
interface KanbanColumnResponse {
  id: string;
  name: string;
  type: 'INBOX' | 'BACKLOG' | 'TODO' | 'IN_PROGRESS' | 'DONE' | 'SNOOZED' | 'CUSTOM';
  order: number;
  color: string;
  isDefault: boolean;
  emailCount: number;
  createdAt: string;
  updatedAt: string;
}

interface KanbanEmailResponse {
  id: string;
  emailId: string;
  columnId: string;
  orderInColumn: number;
  subject: string;
  fromEmail: string;
  fromName: string;
  preview: string;
  receivedAt: string;
  isRead: boolean;
  isStarred: boolean;
  summary?: string;
  summaryGeneratedAt?: string;
  snoozed: boolean;
  snoozeUntil?: string;
  createdAt: string;
  updatedAt: string;
}

interface KanbanBoardResponse {
  columns: KanbanColumnResponse[];
  emailsByColumn: Record<string, KanbanEmailResponse[]>;
}

interface MoveEmailRequest {
  emailId: string;
  targetColumnId: string;
  newOrder?: number;
}

interface SnoozeEmailRequest {
  emailId: string;
  snoozeUntil: string; // ISO 8601 datetime
}

interface AddEmailToKanbanRequest {
  emailId: string;
  columnId?: string;
  generateSummary?: boolean;
}

interface CreateColumnRequest {
  name: string;
  color?: string;
  order?: number;
}

interface UpdateColumnRequest {
  name?: string;
  color?: string;
  order?: number;
}

interface KanbanSyncResult {
  synced: number;      // Number of emails successfully synced
  skipped: number;     // Number of emails skipped (already in Kanban)
  total: number;       // Total emails processed from Gmail
  message: string;     // Human-readable result message
}

interface GmailStatusResponse {
  connected: boolean;  // Whether Gmail is connected for the user
}
```

---

## ⚙️ Backend Configuration

Add these environment variables to enable AI summarization:

```bash
# Required for AI Summarization
GEMINI_API_KEY=your_gemini_api_key

# Optional (defaults to gemini-1.5-flash)
GEMINI_MODEL=gemini-1.5-flash
```

---

## 🔄 Snooze Behavior

1. When an email is snoozed, it moves to the **Snoozed** column
2. The backend scheduler checks every **60 seconds** for expired snoozes
3. When snooze expires, email is automatically restored to its **previous column**
4. Users can manually unsnooze emails at any time

---

## 📦 Recommended Libraries

- **react-beautiful-dnd** - Drag and drop for React
- **@dnd-kit/core** - Modern alternative to react-beautiful-dnd
- **date-fns** or **dayjs** - Date manipulation for snooze times
- **axios** or **fetch** - HTTP client

---

## 🧪 Testing with Postman

Import `postman_collection.json` to test all Kanban APIs. The collection includes:

- Get Full Board
- Get/Create/Update/Delete Columns
- Add/Move/Remove Emails
- Snooze/Unsnooze Emails
- Generate AI Summary

Set the `{{accessToken}}` and `{{emailId}}` variables in Postman to test the endpoints.

