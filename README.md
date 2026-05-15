# Configurable Workflow Engine

Reusable, embeddable JSON-driven workflow engine for Spring Boot, delivered as a multi-module project.

## Modules
- `workflow-engine`: starter/library module with auto-configuration, DSL, orchestrator, step handlers, persistence adapters, and APIs.
- `host-app-example`: runnable host app showing real integration and security ownership.
- `ui`: separate optional React dashboard.

## Key Principles
- Engine does **not** own authentication/authorization.
- Engine does **not** hardcode business logic.
- Workflow definitions are loaded from configurable sources (`file`, `classpath`; `db` deferred hook).
- Host app controls behavior via `workflow.engine.*` properties and bean overrides.

## Supported Step Types
- `SYSTEM`
- `USER`
- `DECISION`
- `API`
- `EVENT`
- `DELAY`
- `SCRIPT`
- `SUB_WORKFLOW` (runs another catalog definition; optionally nests output via `subWorkflowOutputKey`)
- `END`

## Quick Start
### Prerequisites
- Java 21+
- Maven 3.9+

### Build
```bash
mvn clean install
```

### Run host app
```bash
mvn -pl host-app-example spring-boot:run
```

Host app URL: `http://localhost:8081`

### Run UI dashboard
In a separate terminal:
```bash
cd ui
npm install
npm run dev
```

UI URL (default): `http://localhost:5173`

Optional explicit backend binding:
1. Create `ui/.env`
2. Add:
```bash
VITE_API_BASE_URL=http://localhost:8081
```
3. Restart `npm run dev`

### Demo credentials (HTTP Basic)
- `manager1 / password`
- `user123 / password`
- `reviewer1 / password`
- `legal1 / password`
- `admin1 / password`

### Recommended credentials for UI testing
- **Start workflow / author user task**: `user123 / password`
- **Manager approval task**: `manager1 / password`
- **Admin actions (if workflow reaches admin step)**: `admin1 / password`
- **Legal actions (if workflow reaches legal step)**: `legal1 / password`
- **Reviewer actions (if workflow reaches reviewer step)**: `reviewer1 / password`

Note: UI calls backend APIs, so your browser session should use the same credentials you want to test (or use Postman for role switching between requests).

### Start sample workflow
```bash
curl -u user123:password -X POST "http://localhost:8081/demo/workflows/start-sample"
```

### Sub-workflow showcase (definitions on disk)

| Definition id | Role |
|---|---|
| `onboarding-with-subworkflows` | Parent: runs embedded KYC; writes merged child output under `kycResult` in parent context when the child completes. |
| `kyc-verification-subflow` | Child: one `USER` approval step (`${initiator}`); started only from a `SUB_WORKFLOW` step. |

Example start:

```bash
curl -u user123:password -X POST "http://localhost:8081/workflows/start?definitionId=onboarding-with-subworkflows" \
  -H "Content-Type: application/json" \
  -d "{\"initiator\":\"user123\"}"
```

While the parent waits on the child, `GET /workflows/{parentId}` exposes `context.__swActiveChildWorkflowId` (inspect that id for tasks on the nested instance).

## Configuration Reference (`workflow.engine.*`)
```yaml
workflow:
  engine:
    enabled: true
    api:
      enabled: true
      base-path: /workflows
    ui:
      enabled: true
      path: /workflow-ui
    definition:
      source: file # file | classpath | db (db loader deferred)
      path: host-app-example/configs/workflows
      cache-enabled: true
    execution:
      max-retries: 3
      retry-backoff-ms: 2000
    persistence:
      type: jpa
    security:
      enabled: false
```

## API Summary (default base path: `/workflows`)
### Workflow APIs
- `POST /workflows/start?definitionId={id}`
- `POST /workflows/{id}/resume`
- `POST /workflows/{id}/rollback`
- `GET /workflows/{id}`
- `GET /workflows`

### Task APIs
- `GET /workflows/tasks`
- `POST /workflows/tasks/{taskId}/claim`
- `POST /workflows/tasks/{taskId}/approve`
- `POST /workflows/tasks/{taskId}/reject`
- `GET /workflows/tasks/{taskId}/approvals`

### Runtime Event APIs
- `POST /workflows/{workflowId}/events`
- `GET /workflows/{workflowId}/events`

### Demo APIs (host sample)
- `POST /demo/workflows/start-sample`
- `POST /demo/workflows/rollback/{workflowId}`

## Postman Collection
Import:

- `docs/postman/host-app-workflow.postman_collection.json`

Variables cover `baseUrl`, auth, `definitionId` (defaults to `ratings-review-workflow`), `parentDefinitionId` / `childDefinitionId`, workflow and task ids, and `childWorkflowId` (filled when a parent is waiting on an embedded child). Folders include ratings sequences, utilities, and **“03 — Sub-workflow onboarding demo”** (`onboarding-with-subworkflows` + `kyc-verification-subflow`).

## Additional Docs
- `docs/architecture.md`
- `docs/integration.md`
- `docs/api-reference.md`
- `docs/configuration.md`
