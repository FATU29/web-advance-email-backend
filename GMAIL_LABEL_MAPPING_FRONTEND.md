# Gmail Label Mapping - Frontend Integration Guide

## Overview

This feature allows users to map Kanban columns to Gmail labels. When an email is moved to a column, the configured Gmail labels are automatically added/removed from the email in Gmail.

## API Endpoints

### 1. Get Available Gmail Labels

Fetch all Gmail labels that can be mapped to columns.

```
GET /api/kanban/gmail-labels
Authorization: Bearer {accessToken}
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "INBOX",
      "name": "INBOX",
      "type": "system",
      "messageListVisibility": "show",
      "labelListVisibility": "labelShow"
    },
    {
      "id": "Label_123456789",
      "name": "Important Projects",
      "type": "user",
      "messageListVisibility": "show",
      "labelListVisibility": "labelShow"
    }
  ]
}
```

**Label Types:**
- `system` - Built-in Gmail labels (INBOX, SENT, TRASH, SPAM, STARRED, UNREAD, etc.)
- `user` - Custom labels created by the user

### 2. Update Column with Label Mapping

Configure Gmail label sync for a column.

```
PUT /api/kanban/columns/{columnId}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request Body:**
```json
{
  "gmailLabelId": "Label_123456789",
  "gmailLabelName": "Important Projects",
  "addLabelsOnMove": ["STARRED"],
  "removeLabelsOnMove": ["INBOX", "UNREAD"]
}
```

**Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `gmailLabelId` | string | Primary Gmail label ID to add when email moves to this column |
| `gmailLabelName` | string | Display name for the label (for UI display) |
| `addLabelsOnMove` | string[] | Additional label IDs to add when email moves to this column |
| `removeLabelsOnMove` | string[] | Label IDs to remove when email moves to this column |
| `clearLabelMapping` | boolean | Set to `true` to clear all label mapping |

### 3. Clear Label Mapping

```
PUT /api/kanban/columns/{columnId}
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "clearLabelMapping": true
}
```

### 4. Column Response (includes label mapping)

When fetching columns or the board, label mapping is included in the response:

```json
{
  "id": "column_123",
  "name": "Done",
  "type": "DONE",
  "order": 4,
  "color": "#4CAF50",
  "isDefault": true,
  "gmailLabelId": "Label_123456789",
  "gmailLabelName": "Completed",
  "removeLabelsOnMove": ["INBOX", "UNREAD"],
  "addLabelsOnMove": ["STARRED"],
  "emailCount": 15,
  "createdAt": "2024-01-01T00:00:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

## Frontend Implementation

### 1. Column Settings Modal

Add a "Gmail Label Sync" section in the column settings modal:

```tsx
interface LabelMapping {
  gmailLabelId: string | null;
  gmailLabelName: string | null;
  addLabelsOnMove: string[];
  removeLabelsOnMove: string[];
}

interface GmailLabel {
  id: string;
  name: string;
  type: 'system' | 'user';
  messageListVisibility: string;
  labelListVisibility: string;
}
```

### 2. Fetch Gmail Labels

```tsx
const fetchGmailLabels = async (): Promise<GmailLabel[]> => {
  const response = await fetch('/api/kanban/gmail-labels', {
    headers: { 'Authorization': `Bearer ${accessToken}` }
  });
  const data = await response.json();
  return data.data || [];
};
```

### 3. Update Column Label Mapping

```tsx
const updateColumnLabelMapping = async (
  columnId: string, 
  mapping: LabelMapping
) => {
  const response = await fetch(`/api/kanban/columns/${columnId}`, {
    method: 'PUT',
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(mapping)
  });
  return response.json();
};
```

### 4. Clear Label Mapping

```tsx
const clearLabelMapping = async (columnId: string) => {
  const response = await fetch(`/api/kanban/columns/${columnId}`, {
    method: 'PUT',
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ clearLabelMapping: true })
  });
  return response.json();
};
```

## UI Component Example

### Column Settings with Label Mapping

```tsx
import React, { useState, useEffect } from 'react';

interface ColumnLabelSettingsProps {
  column: KanbanColumn;
  onSave: (mapping: LabelMapping) => void;
  onClear: () => void;
}

const ColumnLabelSettings: React.FC<ColumnLabelSettingsProps> = ({
  column,
  onSave,
  onClear
}) => {
  const [labels, setLabels] = useState<GmailLabel[]>([]);
  const [selectedLabel, setSelectedLabel] = useState(column.gmailLabelId || '');
  const [addLabels, setAddLabels] = useState<string[]>(column.addLabelsOnMove || []);
  const [removeLabels, setRemoveLabels] = useState<string[]>(column.removeLabelsOnMove || []);

  useEffect(() => {
    fetchGmailLabels().then(setLabels);
  }, []);

  const handleSave = () => {
    const label = labels.find(l => l.id === selectedLabel);
    onSave({
      gmailLabelId: selectedLabel || null,
      gmailLabelName: label?.name || null,
      addLabelsOnMove: addLabels,
      removeLabelsOnMove: removeLabels
    });
  };

  // Filter labels by type for better UX
  const systemLabels = labels.filter(l => l.type === 'system');
  const userLabels = labels.filter(l => l.type === 'user');

  return (
    <div className="label-mapping-settings">
      <h4>Gmail Label Sync</h4>
      <p className="description">
        Configure automatic Gmail label changes when emails are moved to this column.
      </p>

      {/* Primary Label Selection */}
      <div className="form-group">
        <label>Primary Label (added when email moves here)</label>
        <select
          value={selectedLabel}
          onChange={(e) => setSelectedLabel(e.target.value)}
        >
          <option value="">-- No label --</option>
          <optgroup label="Your Labels">
            {userLabels.map(label => (
              <option key={label.id} value={label.id}>{label.name}</option>
            ))}
          </optgroup>
          <optgroup label="System Labels">
            {systemLabels.map(label => (
              <option key={label.id} value={label.id}>{label.name}</option>
            ))}
          </optgroup>
        </select>
      </div>

      {/* Additional Labels to Add */}
      <div className="form-group">
        <label>Additional Labels to Add</label>
        <MultiSelect
          options={labels}
          selected={addLabels}
          onChange={setAddLabels}
          placeholder="Select labels to add..."
        />
      </div>

      {/* Labels to Remove */}
      <div className="form-group">
        <label>Labels to Remove</label>
        <MultiSelect
          options={labels}
          selected={removeLabels}
          onChange={setRemoveLabels}
          placeholder="Select labels to remove..."
        />
        <small>Common: INBOX, UNREAD to archive and mark as read</small>
      </div>

      <div className="button-group">
        <button onClick={handleSave} className="btn-primary">
          Save Label Mapping
        </button>
        <button onClick={onClear} className="btn-secondary">
          Clear Mapping
        </button>
      </div>
    </div>
  );
};
```

## Common Use Cases

### 1. "Done" Column - Archive emails
```json
{
  "gmailLabelId": "Label_done",
  "gmailLabelName": "Done",
  "removeLabelsOnMove": ["INBOX", "UNREAD"],
  "addLabelsOnMove": []
}
```
**Effect:** When email moves to Done, it's removed from Inbox and marked as read.

### 2. "Important" Column - Star and label
```json
{
  "gmailLabelId": "Label_important",
  "gmailLabelName": "Important",
  "addLabelsOnMove": ["STARRED"],
  "removeLabelsOnMove": []
}
```
**Effect:** When email moves to Important, it gets starred and labeled.

### 3. "To Do" Column - Keep in inbox but label
```json
{
  "gmailLabelId": "Label_todo",
  "gmailLabelName": "To Do",
  "addLabelsOnMove": [],
  "removeLabelsOnMove": []
}
```
**Effect:** Email gets the "To Do" label but stays in inbox.

### 4. "Trash" Column - Move to trash
```json
{
  "gmailLabelId": null,
  "gmailLabelName": null,
  "addLabelsOnMove": ["TRASH"],
  "removeLabelsOnMove": ["INBOX"]
}
```
**Effect:** Email is moved to Gmail trash.

## Important Notes

1. **Gmail Connection Required**: Label sync only works when Gmail is connected. Check with `GET /api/kanban/gmail-status`.

2. **Sync is Automatic**: When `POST /api/kanban/emails/move` is called, labels are automatically synced in the background.

3. **Sync Failures Don't Block**: If Gmail label sync fails (e.g., network issue), the email move still succeeds. Check server logs for sync errors.

4. **System Labels**: Some system labels have special behavior:
   - `INBOX` - Removing archives the email
   - `UNREAD` - Removing marks as read
   - `STARRED` - Adding/removing stars the email
   - `TRASH` - Adding moves to trash
   - `SPAM` - Adding marks as spam

5. **Label IDs**: Always use label IDs (not names) for API calls. Names are for display only.
```

