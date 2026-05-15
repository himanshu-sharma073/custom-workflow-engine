# Integration Guide

## 1) Add dependency
In host app module:

```xml
<dependency>
  <groupId>com.example</groupId>
  <artifactId>workflow-engine</artifactId>
</dependency>
```

## 2) Configure `workflow.engine.*`
Example:

```properties
workflow.engine.enabled=true
workflow.engine.api.enabled=true
workflow.engine.api.base-path=/workflows
workflow.engine.definition.source=file
workflow.engine.definition.path=host-app-example/configs/workflows
workflow.engine.definition.cache-enabled=true
workflow.engine.execution.max-retries=3
workflow.engine.execution.retry-backoff-ms=2000
workflow.engine.persistence.type=jpa
```

## 3) Bridge host auth
Provide `UserContextProvider` bean from host security context.

## 4) Supply workflow definitions
- `file`: `<path>/<workflowId>.json`
- `classpath`: `<path>/<workflowId>.json`
- Each JSON file corresponds to `{definition.path}/{definitionId}.json`, e.g. `onboarding-with-subworkflows.json`, `kyc-verification-subflow.json`.

## 5) Start/resume/rollback through API
Default path examples:
- `POST /workflows/start?definitionId=document-update-approval`
- `POST /workflows/start?definitionId=onboarding-with-subworkflows` (parent; embeds `kyc-verification-subflow` via `SUB_WORKFLOW`)
- `POST /workflows/{id}/resume`
- `POST /workflows/{id}/rollback`

## 6) Task operations
- `GET /workflows/tasks`
- `POST /workflows/tasks/{taskId}/claim`
- `POST /workflows/tasks/{taskId}/approve`
- `POST /workflows/tasks/{taskId}/reject`

## 7) Runtime events
- `POST /workflows/{workflowId}/events`
- `GET /workflows/{workflowId}/events`

## 8) Run the separate UI
Run UI module and point API calls to host backend (`http://localhost:8081`).

## 9) Host sample shortcuts
- Start sample workflow: `POST /demo/workflows/start-sample`
- Rollback by workflow id: `POST /demo/workflows/rollback/{workflowId}`
