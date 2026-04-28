# API Reference

Base path is configurable via `workflow.engine.api.base-path` (default: `/workflows`).
All endpoints require host-controlled security (HTTP Basic in sample host app).

## Workflow
### Start
`POST /workflows/start?definitionId={definitionId}`

Body (optional):
```json
{
  "initiator": "user123",
  "docId": "DOC-42"
}
```

### Resume
`POST /workflows/{id}/resume`

Body (optional context merge):
```json
{
  "reviewApproved": true
}
```

### Rollback
`POST /workflows/{id}/rollback`

### Get by id
`GET /workflows/{id}`

### List
`GET /workflows`

## Tasks
### List visible tasks
`GET /workflows/tasks`

### Claim
`POST /workflows/tasks/{taskId}/claim`

### Approve
`POST /workflows/tasks/{taskId}/approve`

Body:
```json
{
  "approvalType": "ANY",
  "minApprovals": 1,
  "input": {
    "approved": true
  }
}
```

### Reject
`POST /workflows/tasks/{taskId}/reject`

Body:
```json
{
  "approvalType": "ANY",
  "minApprovals": 1,
  "input": {
    "approved": false,
    "reason": "validation failed"
  }
}
```

### Approval history
`GET /workflows/tasks/{taskId}/approvals`

## Runtime Events
### Publish external event
`POST /workflows/{workflowId}/events`

Body:
```json
{
  "eventName": "document-reviewed",
  "correlationId": "DOC-42",
  "payload": {
    "reviewApproved": true
  }
}
```

### Get workflow event timeline
`GET /workflows/{workflowId}/events`

## Demo Host Endpoints
### Start sample definition
`POST /demo/workflows/start-sample`

### Rollback sample workflow
`POST /demo/workflows/rollback/{workflowId}`
